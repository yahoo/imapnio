# AGENTS.md

Guidance for AI coding agents (and humans) working in this repository. This file holds the rules for every change. Area-specific detail
lives in `.claude/rules/`. Claude Code loads each page automatically when matching files are touched; other agents should read the
matching page before changing that area:

| Changing | Read |
|---|---|
| a command class in `async/request/` | `.claude/rules/commands.md` |
| `async/internal/`, `async/netty/`, `ImapAsyncClient` pipeline code | `.claude/rules/session-and-pipeline.md` |
| `ImapResponseMapper` or a class in `async/data/` | `.claude/rules/response-parsing.md` |
| any test | `.claude/rules/testing.md` |

## What this is

`imapnio` is an open-source Java library (Apache 2.0, published to Maven Central as `com.yahoo.imapnio:imapnio.core`) that provides a
non-blocking IMAP client built on Netty. Callers create an `ImapAsyncClient`, open an `ImapAsyncSession` per server connection, and
`execute(...)` `ImapRequest` commands that complete an `ImapFuture<ImapAsyncResponse>`. `ImapResponseMapper` turns raw `IMAPResponse`
lines into typed results.

This is a public repository. Everything committed here (code, comments, commit messages, docs, PR text) must make sense to an outside
contributor: no links to private systems, internal ticket ids, internal hostnames, or customer data.

## Build and verify

The full build needs **JDK 17** (what CI uses). On JDK 21, SpotBugs 4.7.3 crashes with `Java returned: 1`. The bytecode target is Java 8.

```bash
# Full build with every gate: checkstyle, compile, tests, JaCoCo coverage check, SpotBugs, PMD (~20s, works offline once deps are cached)
mvn clean install -Dgpg.skip=true

# Compile + checkstyle + SpotBugs + PMD without tests: proves it compiles and is clean, not that it works
mvn clean install -DskipTests -Dgpg.skip=true

# One test class / one method (checkstyle still runs in the validate phase; JaCoCo check only runs at verify)
mvn -pl core test -Dtest=RenameFolderCommandTest
mvn -pl core test -Dtest='RenameFolderCommandTest#testGetCommandLine'

# Coverage report after a build: core/target/jacoco/index.html
```

A change is done only when `mvn clean install -Dgpg.skip=true` passes. Don't guess at style or formatting: run the build and fix
what the tools report. The gates are strict:

| Gate | Where | What fails the build |
|---|---|---|
| Checkstyle (`checkstyle.xml`) | `validate`, main **and** test sources | any violation (`maxAllowedViolations=0`) |
| JaCoCo (`core/pom.xml`) | `verify` | line/branch/method/instruction < 98%, complexity < 97%, any class with no coverage |
| SpotBugs (`spotbugs-include.xml`) | `verify` | medium+ findings in main code |
| PMD (`pmd-ruleset.xml`) | `verify` | more than one priority-1 violation (`StringInstantiation`, `GuardLogStatementJavaUtil`) |

Coverage is near-total, so **every new branch needs a test**. Plan the tests together with the code.

`SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder"` during tests is expected (no logger binding on the test classpath).

## Layout

Single Maven module `core/` under the parent `pom.xml`. The parent pom says "DO NOT ADD DEPENDENCIES TO THIS FILE": declare versions in the
parent's `<dependencyManagement>` and the dependency itself in `core/pom.xml`.

```
core/src/main/java/com/yahoo/imapnio/
  async/client/     Public entry points: ImapAsyncClient (bootstrap, TLS, pipeline), ImapAsyncSession (interface),
                    ImapAsyncSessionConfig, ImapFuture, ImapAsyncCreateSessionResponse
  async/internal/   ImapAsyncSessionImpl: tags, the in-flight command queue, continuations, timeouts, COMPRESS
  async/netty/      Channel handlers: greeting detection (ImapClientConnectHandler), response dispatch (ImapClientCommandRespHandler)
  async/request/    One class per IMAP command + ImapCommandLineBuilder / ImapArgumentFormatter (wire encoding and validation)
  async/response/   ImapAsyncResponse (getResponseLines, byte counts, latency), ImapResponseMapper (lines -> typed results)
  async/data/       Typed result and parameter classes (Capability, SearchResult, MessageNumberSet, ...)
  async/exception/  ImapAsyncClientException and its FailureType enum
  client/           ImapClientRespReader: frames server bytes into lines, gluing {n} literals onto their line
  command/          ImapClientRespDecoder (String -> javax.mail IMAPResponse), Argument, ProxyProtocol
core/src/test/java/ Mirrors main, one *Test per class (TestNG + Mockito)
```

## How a command flows

1. `ImapAsyncClient.createSession` connects. For `imaps` it adds an `SslHandler` with host-name verification (SNI name if given, else the URI host).
   The pipeline is: IdleStateHandler → ImapClientRespReader → StringDecoder/Encoder (US-ASCII) → ImapClientRespDecoder → ImapClientConnectHandler.
2. `ImapClientConnectHandler` waits for the server greeting and then removes itself. On `OK` it creates `ImapAsyncSessionImpl`, which
   adds `ImapClientCommandRespHandler`, and completes the session future; otherwise it fails the future and closes the channel.
3. `session.execute(cmd)` assigns tag `a<n>`, queues the entry, and writes `tag SP cmd.getCommandLineBytes()`.
4. Each server line goes to `handleChannelResponse`. A continuation (`+`) asks the command for `getNextCommandLineAfterContinuation`.
   The matching tagged line completes the future with an `ImapAsyncResponse`. On error, timeout or disconnect, the future fails and the session closes.
5. The caller maps lines with `new ImapResponseMapper().readValue(lines, Capability.class)`.

## Invariants: do not break these

- **One command in flight per session.** `execute` throws `COMMAND_NOT_ALLOWED` while the queue is non-empty. IDLE is ended through `terminateCommand`.
- **Caller input never reaches the wire unchecked.** Write strings through `ImapCommandLineBuilder.astring(...)` (atom, quoted, or literal)
  or validate them with `ImapArgumentFormatter`. SEARCH keys are the exception: they are a javax.mail `Argument` written through
  `ByteBufWriter`. Never concatenate caller input into the line. A raw CR/LF would inject a second IMAP command. Folder names go through
  `BASE64MailboxEncoder` (modified UTF-7).
- **The command line is US-ASCII.** `ImapCommandLineBuilder` refuses NUL and rejects non-ASCII rather than silently replacing it. Non-ASCII
  reaches the wire only after encoding: folder names as modified UTF-7, and SEARCH keys as literals in the declared `CHARSET`.
- **Secrets are never logged.** A command that carries credentials returns `true` from `isCommandLineDataSensitive()` and returns a
  redacted `getDebugData()`. `cleanup()` must drop references to secrets and to any buffered segments.
- **`cleanup()` nulls every non-primitive, non-static field.** Command tests check this by reflection.
- **TLS host-name verification stays on**, including for a caller-supplied `SSLContext`.
- **Errors are `ImapAsyncClientException` with a `FailureType`.** It is a checked exception; add a new enum constant when none fits.
- **Never throw an unchecked exception, in main or test code.** That means `RuntimeException` and every subclass
  (`IllegalArgumentException`, `IllegalStateException`, `NullPointerException`, `UnsupportedOperationException`, ...) and `Error`.
  Reject bad input with `ImapAsyncClientException(FailureType.INVALID_INPUT, ...)`, not `IllegalArgumentException`. Where a
  dependency (Netty, javax.mail) can throw one, catch it at that call and wrap it, as the session does. Test helpers return values
  or declare a specific checked exception.
- **Public API is a published contract.** Do not change or remove public signatures. Add an overload and keep the old one delegating.
  The version is set in both `pom.xml` and `core/pom.xml`. Maintainers bump it in a release PR, not in feature changes.

## Code style (enforced by checkstyle unless noted)

- Javadoc with complete `@param`/`@return`/`@throws`: checkstyle requires it on public classes, methods and fields, and by convention
  every non-public member, constants included, has one too.
- All parameters `final`. No star imports. Lines ≤ 150 chars. Braces always. A `default` in every switch.
- No `System.out/err/in/exit`. Do not catch `Exception` or `Throwable`; catch specific types (a `RuntimeException` multi-catch is used deliberately in the session).
- Guard log calls with `logger.isDebugEnabled()` etc. This is a convention: the PMD guard rule only covers `java.util.logging`, and this
  code uses SLF4J. The log prefix is `"[{},{}] ..."` with `sessionId, sessionCtx`.
- Annotate nullability with `javax.annotation.Nonnull` / `@Nullable`. Constants are `private static final`.
- Prefer instance methods over static ones, test helpers included (constants are fine as `static final`). *(maintainer convention)*
- Keep main code compatible with Java 8 APIs. The compiler uses `-source/-target 1.8` without `--release`, so a Java 9+ library call
  compiles on JDK 17 and then fails on a Java 8 runtime.
- Match the surrounding code: build strings with `StringBuilder` chains, and pre-encode command keywords as `private static final byte[]`.

## Design rules (judgment the tools cannot enforce)

- **Narrow types over wide ones.** In new public API, use an enum instead of a `boolean` flag (as `DebugMode` and `LiteralSupport` do),
  and a typed value instead of a raw `String` or `long` when one exists (`MessageNumberSet` for sequence sets, `Flags` for flags).
- **Immutable by default.** New result and parameter classes have `final` fields set in the constructor and no setters. Return unmodifiable
  views of collections. `ImapAsyncSessionConfig` is the deliberate mutable exception.
- **No lambdas, streams or method references in main code.** Use loops and anonymous classes, as the rest of the library does.
- **Wire bytes go into a Netty `ByteBuf`** (`Unpooled.buffer`, `ByteBufWriter`), never through `String` concatenation.
- **Size an `ArrayList` up front** when the count is known (`new ArrayList<SNIServerName>(sniNames.size())` in `ImapAsyncClient`).
- **No throwaway temporaries.** Chain calls instead of naming a value used once, unless the name adds real clarity.
- **One vocabulary.** Command classes and their arguments say *folder* (`CreateFolderCommand`, `folderName`). *Mailbox* stays only where
  it is the RFC or javax.mail term (`MailboxInfo`, `mailboxId` from RFC 8474, ABNF in comments). Keep *message sequence number* and *UID* distinct: each has its own command class (`CopyMessageCommand` vs
  `UidCopyMessageCommand`), and one must never be passed where the other is expected.
- **No mutable static state.** State lives in instances, so sessions stay independent.

## Comments

- Default to no inline comment. Write one only when a competent reader would otherwise make a wrong change. One line, two at most.
- Explain *why*, not *what*: cite the RFC section when the reason is protocol behavior (e.g. "RFC 3501 section 4.3"), or name the hazard.
- No narration of the next line and no history ("used to", "previously", "replaced"). Git and the PR hold that.
- Javadoc that checkstyle requires is not optional. The class Javadoc is the place for the longer protocol rationale.
- A `TODO` / `FIXME` names a GitHub issue. Otherwise fix it now or leave it out.

## Tests

See `.claude/rules/testing.md`. Short version: TestNG `Assert.assertEquals(actual, expected, message)`, Mockito 1.10 (`mockito-all`, old
API: `Matchers.eq`, `Matchers.isA`), no real network, asserts only inside `@Test` methods,
no new threads (mock instead), no static helpers.
For each change, add unit tests that cover only the new or changed code, not the entire stack.

## Commits and PRs

- Commit subject: imperative, ≤ 72 chars, describes the behavior change ("Check the server's host name on imaps connections").
  The body explains what was wrong and why the fix is right, wrapped at ~72 columns. Don't restate the diff.
- One logical change per commit and per PR. Don't fold unrelated cleanups in.
- Stage files by name. Never `git add -A` / `git add .`: it keeps scratch files out of commits.
- Never use `--no-verify`. Never push directly to `yahoo/imapnio`; open PRs from a fork. Force-push only your own PR branch, and only with
  `--force-with-lease`. Once a PR has review comments, don't amend or force-push it without the author's agreement.
- Fill in `pull_request_template.md`. Add or update tests for every change and update `README.md` when public behavior changes.
- Before opening a PR, run the `review-change` skill (`.claude/skills/review-change/`) or walk its checklist by hand.
- Never commit `target/`, IDE files, `ci/deploy/` (decrypted signing keys), or any key material. Do not edit `ci/*.enc` or `ci/deploy.sh`
  unless the task is about release automation.
- CI is Travis (`.travis.yml`, JDK 17, `mvn clean install -Dgpg.skip=true`). Pushes to `master` deploy to Maven Central via `ci/deploy.sh`.
  Treat merging to `master` as releasing.

## Known gotchas

- `spotbugs-exclude.xml` starts with a DOCTYPE, which SpotBugs refuses to read ("Unable to read filter ... DOCTYPE is disallowed"), so the
  exclude list is currently ignored. Only the include filter applies.
- Checkstyle runs in `validate`, so even `mvn test` fails on a style violation in a test file.
- Artifact signing lives only in the `ossrh` release profile, which `ci/deploy.sh` activates. `-Dgpg.skip=true` in the commands above
  mirrors CI and is harmless locally.
- `ImapAsyncSessionImpl.execute` calls `getCommandLineBytes()` twice (byte count, then send). Commands must therefore build the line
  idempotently and reset any segment cursor on every call (see `LoginCommand`, `IdCommand`).
