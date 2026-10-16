---
paths:
  - "core/src/test/**"
---

# Testing conventions (`core/src/test/`)

- **Framework:** TestNG (`org.testng.annotations.Test`, `org.testng.Assert`). Argument order is `Assert.assertEquals(actual, expected, message)`,
  the reverse of JUnit. Always pass a message.
- **Mocking:** Mockito **1.10.19** (`mockito-all`). Use `org.mockito.Mockito`, `org.mockito.Matchers` and `ArgumentCaptor`,
  as the existing tests do.
- **Layout:** one `<ClassName>Test` per main class, same package, so package-private members are reachable. Constructors that take a
  `Clock`, `Logger`, `Bootstrap` or `EventLoopGroup` exist for injection. Use them instead of reflection where possible.
- **No threads.** Don't start a `Thread`, executor or event loop in a unit test. Mock the collaborator (`Channel`, `ChannelPromise`,
  `EventLoopGroup`), capture its listener with `ArgumentCaptor`, and call it from the test thread. The one existing exception is the
  public-constructor test in `ImapAsyncClientTest`, which needs a real event loop. Don't copy it.
- **No network, no sleeps.** Drive Netty with mocked `Channel` / `ChannelPipeline` / `ChannelHandlerContext` and captured listeners. Time
  comes from a mocked `Clock`. The whole suite runs in ~2s; keep it that way.
- **Test the change, not the stack.** Add unit tests only for the code the change adds or alters, in the `<ClassName>Test` of the class
  you touched. Don't add tests for unchanged code, don't reorganize existing tests, and don't drive a change through the whole stack
  (client → session → handler → command) when a unit test of the changed class covers it. The coverage gate is met by covering the
  new branches.
- **Independent tests.** Every `@Test` builds its own state. Fields set once in `@BeforeClass` and never changed are fine (e.g. the
  cleanup-check field set). Don't add new fields that one test changes and another reads. Never change static or shared state.
- **Mocks only where needed.** Prefer real objects: `new IMAPResponse("* OK ...")`, `Unpooled` buffers, real commands. Mock
  `Channel`, `Clock` and the like, and create those mocks inside the test. Don't mock the internals of Netty or javax.mail.
- **Hostile and non-ASCII inputs.** Any code that encodes caller text needs cases with accented Latin, Cyrillic, CJK, right-to-left
  text and emoji (folder names go through modified UTF-7), the IMAP specials `( ) { } " \ % * ]` and SP, and the control characters
  CR, LF, NUL and DEL.
- **Asserts only in `@Test` methods.** Helpers return values (e.g. a boolean or the captured object) and the test asserts on them.
- **Helpers are instance methods**, not `static` (constants may be `private static final`).
- **Javadoc and checkstyle apply to tests too:** every test method gets a Javadoc with `@throws` for each declared exception (the house
  style writes "will not throw"), and parameters are `final`. `MagicNumber` and `JavadocPackage` are suppressed under `test/`.
- **Command tests** check the exact wire string (`"RENAME a b\r\n"`), `getCommandType()`, and that `cleanup()` nulled every non-primitive
  field. Copy the reflection block in `@BeforeClass` from an existing command test.
- **Coverage:** the build fails below 98% line/branch coverage. After `mvn clean install -Dgpg.skip=true`, read
  `core/target/jacoco/index.html` to find uncovered branches.
- `testng` groups `notIsolate` and `EventListenersRegression` are excluded by surefire. Don't tag normal tests with them.
