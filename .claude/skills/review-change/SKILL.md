---
name: review-change
description: Review a branch or working-tree change in imapnio before it becomes a PR. Checks the diff against the repo's AGENTS.md and .claude/rules (wire-encoding safety, secrets, cleanup, API compatibility, design and test conventions), runs the Maven gate, and reports BLOCKER/WARNING findings with file:line. Use when asked to review changes, check a branch before a PR, or self-review work just finished.
---

# Review an imapnio change

Read-only. Don't edit files, stage or commit while reviewing. Report the findings, and fix only when asked.

## 1. Collect the change

```bash
git fetch https://github.com/yahoo/imapnio.git master:refs/remotes/upstream-master   # or use an existing upstream remote
git diff --stat upstream-master...HEAD
git diff upstream-master          # committed + uncommitted changes as one patch
git status --short                # untracked files: ask which ones are part of the change before reading them
```

Read every changed file in full, plus `AGENTS.md` and the `.claude/rules/` page for each area the change touches.

## 2. Check, in this order

**Protocol and security: BLOCKER if violated**
- Caller-supplied text reaches the wire only via `ImapCommandLineBuilder.astring` or an `ImapArgumentFormatter.validate*` method
  (SEARCH keys: a javax.mail `Argument` through `ByteBufWriter`). Look for `+`, `append` or `writeBytes` of raw arguments.
- Folder names are `BASE64MailboxEncoder`-encoded. A literal-capable argument uses `finish()` with continuation segments, never `finishSingle()`.
- `getCommandLineBytes()` is safe to call twice: segments are rebuilt and the cursor is reset.
- Secret-bearing commands: `isCommandLineDataSensitive()` returns true, `getDebugData()` is redacted, `cleanup()` drops the secret and the segments.
  No secret in any log or exception message.
- `cleanup()` nulls every non-primitive field, including new ones.
- TLS host-name verification is still applied on both SSL engine paths.
- No `throw` of `RuntimeException`, any of its subclasses, or `Error`, in main or test code. Failures are `ImapAsyncClientException`.
- Session invariants hold: one command in flight; errors fail the future with an `ImapAsyncClientException` and close the session.
- No public signature removed or changed incompatibly; additions come as overloads.
- No Java 9+ API in main code. No new dependency without a version in the parent `<dependencyManagement>`.

**Design and style: WARNING unless the build fails**
- `AGENTS.md` "Design rules" and "Comments": enum over boolean in new API, immutable values, no lambdas/streams, `ByteBuf` for
  wire bytes, folder/mailbox and UID/sequence-number vocabulary, no mutable static state, no narration or history comments.
- Logging is guarded and uses the `"[{},{}] ..."` prefix.
- Nothing internal or private in code, comments or the commit message (public repository).

**Tests: BLOCKER if behavior is untested**
- Every new branch has a test, including error paths. The JaCoCo gate is 98%.
- Tests are scoped to the change: unit tests in the touched class's `*Test`, with no tests added for unchanged code and no
  whole-stack tests where a unit test would do. WARNING if not.
- Command tests: the exact wire string with `\r\n`, `getCommandType()`, cleanup reflection, and CR/LF/NUL/non-ASCII cases. New
  arguments are added to `CommandArgumentValidationTest` and `CommandArgumentCompatibilityTest`.
- `.claude/rules/testing.md`: TestNG assert order with messages, asserts only in `@Test`, instance helpers (no static), no threads
  (mock instead), no network or sleeps,
  independent tests.

**Hygiene**
- Commit subject is imperative and ≤ 72 chars, and the body says why. One logical change.
- No `target/`, IDE files, key material or scratch files in the diff.

## 3. Run the gate

JDK 17:

```bash
mvn clean install -Dgpg.skip=true
```

Report the test count and pass/fail. If the build cannot run, say so explicitly rather than skipping silently.

## 4. Report

```
## Review: <branch>

### Findings
- BLOCKER: <what is wrong and why it matters> (path/File.java:line)
- WARNING: <...> (path/File.java:line)

### Build
- Tests run: N, Failures: N. Checkstyle / JaCoCo / SpotBugs / PMD: pass | fail (<first error>)

### Verdict
READY FOR PR | NEEDS WORK (N blockers, N warnings)
```

Flag only real violations, each with a cited rule and a file:line. Don't pad the report with style nits that the build already enforces.
