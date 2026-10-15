---
paths:
  - "core/src/main/java/com/yahoo/imapnio/async/request/**"
---

# Command classes (`async/request/`)

Each class in `core/src/main/java/com/yahoo/imapnio/async/request/` builds the client side of one IMAP command. The package is the
security boundary: caller strings become wire bytes in it.

## Adding or changing a command

1. **Pick the closest base class** instead of implementing `ImapRequest` from scratch:

   | Base | Use for |
   |---|---|
   | `AbstractNoArgsCommand` | bare keyword: `NOOP`, `CHECK`, `CLOSE`, `CAPABILITY`, ... |
   | `AbstractFolderActionCommand` | `<KEYWORD> <mailbox>`: CREATE, DELETE, SUBSCRIBE, ... |
   | `OpenFolderActionCommand` | SELECT / EXAMINE, with the QRESYNC option |
   | `AbstractQueryFoldersCommand` | LIST / LSUB |
   | `AbstractMessageActionCommand` | `<KEYWORD> <set> <mailbox>`: COPY, MOVE and UID variants |
   | `AbstractFetchCommand` / `AbstractSearchCommand` / `AbstractStoreFlagsCommand` | FETCH, SEARCH, STORE families |
   | `AbstractAuthCommand` | SASL `AUTHENTICATE` with a continuation exchange |
   | `ImapRequestAdapter` | anything else; supplies safe defaults for the optional methods |

2. **Add an `ImapRFCSupportedCommandType` constant** and return it from `getCommandType()`. Callers may also supply their own
   `ImapCommandType`, so do not switch exhaustively on the enum elsewhere.
3. **Encode arguments safely**:
   - `astring` values (user names, passwords, folder names, ID fields): `new ImapCommandLineBuilder(literalOpt).raw(KEYWORD_B).astring(v, doQuote, "arg name")`.
     The builder picks atom, quoted, or literal form, and rejects NUL and non-ASCII.
   - Folder names: pass through `BASE64MailboxEncoder.encode(...)` first. The result can never need a literal, so finish with `finishSingle()`.
   - Bare tokens (atoms, sequence sets, fetch items, flags): validate with `ImapArgumentFormatter.validateAtom / validateSequenceSet /
     validateFetchAtt / buildFlagString`. Never write them unchecked.
   - Keywords: `private static final byte[] FOO_SP_B = "FOO ".getBytes(StandardCharsets.US_ASCII);`
4. **Literals and continuations.** If an argument may contain CR/LF it may need a synchronizing literal, which splits the line:
   - Use `finish()` (not `finishSingle()`), keep the `List<ByteBuf> segments` plus a `nextSegment` cursor, and return `segments.get(0)`
     from `getCommandLineBytes()`.
   - Override `getNextCommandLineAfterContinuation` to hand out the next segment. When none are left, throw
     `OPERATION_NOT_SUPPORTED_FOR_COMMAND`. `LoginCommand` and `IdCommand` are the reference implementations.
   - `getCommandLineBytes()` is called **twice** per `execute`, so rebuild the segments and reset the cursor on every call.
   - Accept a `LiteralSupport` constructor argument (keep the old constructor, defaulting to `DISABLE`) so callers that saw
     `LITERAL+` / `LITERAL-` in CAPABILITY avoid the round trip.
5. **Sensitive data.** If the line can carry a secret, return `true` from `isCommandLineDataSensitive()`, return redacted text from
   `getDebugData()`, and null the secret and the segments in `cleanup()`.
6. **`cleanup()`** must null every non-primitive, non-static field, including inherited ones. The command's test verifies this by reflection.
7. **Tests** in `core/src/test/java/com/yahoo/imapnio/async/request/<Name>Test.java`: the exact wire string including `\r\n`, the
   `getCommandType()`, the cleanup reflection check, and the error paths. For any new caller-supplied argument, add injection cases
   (CR, LF, NUL, non-ASCII) to `CommandArgumentValidationTest`, and confirm legitimate values still pass via `CommandArgumentCompatibilityTest`.

## Don'ts

- Don't build a line by concatenating caller input, or with `getBytes()` without `StandardCharsets.US_ASCII`.
- Don't widen visibility: `ImapCommandLineBuilder` and the `ImapArgumentFormatter` methods are package-private by design.
- Don't change an existing command's wire output for valid input. Callers and tests depend on it byte for byte.
