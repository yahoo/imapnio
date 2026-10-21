---
paths:
  - "core/src/main/java/com/yahoo/imapnio/async/internal/**"
  - "core/src/main/java/com/yahoo/imapnio/async/netty/**"
  - "core/src/main/java/com/yahoo/imapnio/async/client/ImapAsyncClient.java"
---

# Session state machine and channel handlers (`async/internal/`, `async/netty/`)

`ImapAsyncSessionImpl` plus the handlers in `async/netty/` are the concurrency core. This code runs on Netty event-loop threads while callers
invoke `execute` / `close` from their own threads. Changes need care and thorough tests.

## Model

- A session wraps one `Channel`. `requestsQueue` holds at most one `ImapCommandEntry`. A second `execute` while one is pending throws
  `COMMAND_NOT_ALLOWED`. Keep that guarantee: response routing assumes the head of the queue owns every untagged line.
- Entry states: `REQUEST_IN_PREPARATION` → `REQUEST_SENT` (set in the write listener `operationComplete`) → `RESPONSES_DONE`.
  The idle timer (`IdleStateHandler` ALL_IDLE, from `readTimeoutMillis`) fails a command with `CHANNEL_TIMEOUT` only in `REQUEST_SENT`,
  and never for `IdleCommand`.
- Tags are `"a" + AtomicLong`. The command completes on the first tagged response whose tag matches.
- While a command is pending, any channel exception, write failure, timeout or disconnect fails its future **and closes the session**.
  A session is never reused after an error.
- `CompressCommand` OK installs `JdkZlibDecoder`/`JdkZlibEncoder` right after `ImapAsyncClient.SSL_HANDLER` (or first, without TLS).
  Handler names are part of that contract, so don't rename them.
- `ImapAsyncResponse` carries the request/response byte counts and the elapsed time from the injected `Clock`. Read time only from that
  clock, so tests stay deterministic.

## Handlers (`async/netty/`)

- `ImapClientConnectHandler` lives only until the greeting: it removes itself, then on `OK` builds the session (which adds
  `ImapClientCommandRespHandler`), otherwise fails with `CONNECTION_FAILED_WITHOUT_OK_RESPONSE` and closes.
  READER_IDLE before the greeting fails with `CONNECTION_FAILED_EXCEED_IDLE_MAX`.
- `ImapClientCommandRespHandler` forwards to `ImapCommandChannelEventProcessor` and drops its reference after `channelInactive`.
- Handlers in `ImapAsyncClient.ImapClientChannelInitializer` are shared bootstrap state and must stay session-less. Per-session handlers
  are added after connect.

## Logging

- Gate on `isDebugEnabled()`: trace logs every session, and debug logs only sessions whose `DebugMode` is `DEBUG_ON`.
- Format: `"[{},{}] C:{}"` / `"[{},{}] S:{}"` with `sessionId, sessionCtx`. Client lines of sensitive commands log `getDebugData()`, never the raw line.

## Testing

`ImapAsyncSessionImplTest` mocks `Channel`, `ChannelPipeline`, `ChannelPromise` and `Clock`, and captures listeners with `ArgumentCaptor`
to drive `operationComplete` and responses by hand. The `netty` handler tests mock `ChannelHandlerContext` and call `decode`,
`exceptionCaught`, `userEventTriggered` and `channelInactive` directly. Follow those patterns rather than opening sockets.
