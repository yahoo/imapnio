---
paths:
  - "core/src/main/java/com/yahoo/imapnio/async/response/**"
  - "core/src/main/java/com/yahoo/imapnio/async/data/**"
---

# Parsing server responses into typed results (`async/response/`, `async/data/`)

## Adding a new result type

1. Create the result class in `async/data/` as an immutable value: `final` fields set in the constructor, getters, Javadoc everywhere, and
   `@Nonnull` / `@Nullable` on every accessor. Reuse `javax.mail` / `com.sun.mail.imap.protocol` types (`MailboxInfo`, `Status`,
   `AppendUID`, ...) when one already fits instead of duplicating it.
2. In `ImapResponseMapper`, add a `valueType == YourType.class` branch to `readValue` and a `parseToYourType(IMAPResponse[])` method on the
   inner parser. Unknown types fall through to `UNKNOWN_PARSE_RESULT_TYPE`. Keep that as the last line.
3. Parse defensively. The last element is normally the tagged completion. Check `isOK()` and fail with an `ImapAsyncClientException`
   instead of returning partial data, and skip untagged lines that belong to other extensions.
4. Tests: `ImapResponseMapperTest` builds inputs with `new IMAPResponse("* ...")`. Cover the happy path, a NO/BAD tagged line, a missing
   or malformed line, and extra unrelated untagged lines. Add `<Type>Test` in `data/` for the value class.

## Notes

- `ImapAsyncResponse` is what callers receive: the response lines plus `getRequestTotalBytes`, `getResponseTotalBytes` and
  `getTotalTimeElapsed` (milliseconds). Changing these is a public API change.
- `MessageNumberSet` (`async/data/`) models sequence sets, and `MessageNumberSet.buildString` renders them. Use it instead of hand-formatting
  `1:5,7`.
