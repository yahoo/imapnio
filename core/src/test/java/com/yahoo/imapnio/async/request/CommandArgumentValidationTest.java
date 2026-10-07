package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Flags;

import org.apache.commons.codec.binary.Base64;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.imapnio.async.data.Capability;
import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;
import com.yahoo.imapnio.command.Argument;

import io.netty.buffer.ByteBuf;

/**
 * Unit tests for the validation of caller-supplied command arguments.
 *
 * <p>
 * A caller-supplied argument used to reach the wire verbatim, so a CR or LF in one terminated the command line and put a second, caller-chosen
 * IMAP command on the connection. These tests cover the reported vectors, the further ones found while triaging, and a regression guard that
 * legitimate arguments are still written out unchanged.
 * </p>
 */
public class CommandArgumentValidationTest {

    /** Payload that closes the current command line and opens another one. */
    private static final String CRLF_PAYLOAD = "a\r\nX1 LOGOUT";

    /** Payload carrying a bare NUL, which no astring form can represent. */
    private static final String NUL_PAYLOAD = "a\u0000b";

    /** Payload carrying SOH, the field separator inside the XOAUTH2 and OAUTHBEARER SASL payloads. */
    private static final String SOH_PAYLOAD = "a\u0001b";

    /**
     * A command whose command line is built lazily, so the validation can be driven uniformly.
     */
    private interface CommandLine {
        /**
         * Builds the command line.
         *
         * @return the built command line
         * @throws ImapAsyncClientException when an argument is refused
         */
        ByteBuf build() throws ImapAsyncClientException;
    }

    /**
     * Asserts that building the given command line is refused as invalid input.
     *
     * @param label what is being rejected, used in the assertion message
     * @param cmd the command line to build
     */
    private void assertRejected(final String label, final CommandLine cmd) {
        ImapAsyncClientException actual = null;
        try {
            cmd.build();
        } catch (final ImapAsyncClientException e) {
            actual = e;
        }
        Assert.assertNotNull(actual, label + " should be rejected.");
        Assert.assertEquals(actual.getFailureType(), FailureType.INVALID_INPUT, label + " failure type mismatched.");
        Assert.assertFalse(actual.getMessage().contains("\r"), label + " message should not echo the rejected value.");
    }

    /**
     * Builds a capability advertising SASL-IR, so the AUTH commands emit their client response in the initial command line.
     *
     * @return the capability
     */
    private Capability saslIrCapability() {
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put("SASL-IR", Collections.<String>emptyList());
        return new Capability(capas);
    }

    /**
     * Collects every piece of a command line, sending a continuation request until the command has nothing left to give.
     *
     * @param cmd the command to drain
     * @return the pieces, in the order they go on the wire
     * @throws ImapAsyncClientException when an argument is refused
     */
    private List<String> drain(final ImapRequest cmd) throws ImapAsyncClientException {
        final List<String> out = new ArrayList<String>();
        out.add(cmd.getCommandLineBytes().toString(StandardCharsets.US_ASCII));
        while (true) {
            try {
                out.add(cmd.getNextCommandLineAfterContinuation(null).toString(StandardCharsets.US_ASCII));
            } catch (final ImapAsyncClientException e) {
                Assert.assertEquals(e.getFailureType(), FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND,
                        "Draining should end by reporting there is nothing more to send.");
                return out;
            }
        }
    }

    /**
     * Tests the reported vector: a CRLF in the LOGIN username is now carried as a synchronizing literal rather than splitting the line.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testLoginUsernameWithCrlfSentAsLiteral() throws ImapAsyncClientException {
        Assert.assertEquals(drain(new LoginCommand(CRLF_PAYLOAD, "s3cret")),
                Arrays.asList("LOGIN {12}\r\n", "a\r\nX1 LOGOUT s3cret\r\n"),
                "The username should be sent as a literal, so the CRLF is data rather than a line terminator.");
    }

    /**
     * Tests that a CRLF in the password is carried the same way.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testLoginPasswordWithCrlfSentAsLiteral() throws ImapAsyncClientException {
        Assert.assertEquals(drain(new LoginCommand("alice", CRLF_PAYLOAD)),
                Arrays.asList("LOGIN alice {12}\r\n", "a\r\nX1 LOGOUT\r\n"), "The password should be sent as a literal.");
    }

    /**
     * Tests that two literals in one command line produce two pauses, the shape RFC 7888 section 4 illustrates for LOGIN.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testLoginWithTwoLiterals() throws ImapAsyncClientException {
        Assert.assertEquals(drain(new LoginCommand("a\r\nb", "c\r\nd")),
                Arrays.asList("LOGIN {4}\r\n", "a\r\nb {4}\r\n", "c\r\nd\r\n"),
                "Each synchronizing literal should end a piece of the command line.");
    }

    /**
     * Tests that a server advertising LITERAL+ or LITERAL- is sent the whole line at once, with the count marked by a plus.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testLoginWithNonSynchronizingLiteral() throws ImapAsyncClientException {
        Assert.assertEquals(drain(new LoginCommand("a\r\nb", "s3cret", LiteralSupport.ENABLE_LITERAL_PLUS)),
                Arrays.asList("LOGIN {4+}\r\na\r\nb s3cret\r\n"), "LITERAL+ should need no continuation.");

        // RFC 7888: LITERAL- uses the same "+" marker, it only caps the size
        Assert.assertEquals(drain(new LoginCommand("a\r\nb", "s3cret", LiteralSupport.ENABLE_LITERAL_MINUS)),
                Arrays.asList("LOGIN {4+}\r\na\r\nb s3cret\r\n"), "LITERAL- under the cap should need no continuation.");
    }

    /**
     * Tests that a literal too large for LITERAL- falls back to the synchronizing form.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testLiteralMinusOverCapFallsBackToSynchronizing() throws ImapAsyncClientException {
        final StringBuilder big = new StringBuilder("a\r\n");
        for (int i = 0; i < 4100; i++) {
            big.append('x');
        }
        final List<String> pieces = drain(new LoginCommand(big.toString(), "s3cret", LiteralSupport.ENABLE_LITERAL_MINUS));
        Assert.assertEquals(pieces.size(), 2, "Over the cap, the literal should be synchronizing and so split the line.");
        Assert.assertEquals(pieces.get(0), "LOGIN {4103}\r\n", "The count should carry no plus when it exceeds the cap.");
    }

    /**
     * Tests the researcher's exact reported payload, verbatim. It no longer injects, because the username is framed by an octet count.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testReportedPayload() throws ImapAsyncClientException {
        Assert.assertEquals(drain(new LoginCommand("user\r\nA001 CAPABILITY", "password")),
                Arrays.asList("LOGIN {21}\r\n", "user\r\nA001 CAPABILITY password\r\n"),
                "The reported payload should become a literal, not a second command.");
    }

    /**
     * Tests the other reported vector, the ID command field values.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testIdParamsWithCrlfSentAsLiteral() throws ImapAsyncClientException {
        final Map<String, String> value = new LinkedHashMap<String, String>();
        value.put("name", CRLF_PAYLOAD);
        Assert.assertEquals(drain(new IdCommand(value)), Arrays.asList("ID (\"name\" {12}\r\n", "a\r\nX1 LOGOUT)\r\n"),
                "An ID value should be sent as a literal.");

        final Map<String, String> key = new LinkedHashMap<String, String>();
        key.put(CRLF_PAYLOAD, "1.0");
        Assert.assertEquals(drain(new IdCommand(key)), Arrays.asList("ID ({12}\r\n", "a\r\nX1 LOGOUT \"1.0\")\r\n"),
                "An ID field should be sent as a literal.");

        Assert.assertEquals(drain(new IdCommand((Map<String, String>) null)), Arrays.asList("ID NIL\r\n"),
                "A null parameter list should still produce NIL.");
    }

    /**
     * Tests that NUL is still refused, in both credentials, since a literal is *CHAR8 and CHAR8 excludes NUL.
     */
    @Test
    public void testNulIsStillRefused() {
        assertRejected("LOGIN username with NUL", () -> new LoginCommand(NUL_PAYLOAD, "s3cret").getCommandLineBytes());
        assertRejected("LOGIN password with NUL", () -> new LoginCommand("alice", NUL_PAYLOAD).getCommandLineBytes());
        final Map<String, String> value = new LinkedHashMap<String, String>();
        value.put("name", NUL_PAYLOAD);
        assertRejected("ID value with NUL", () -> new IdCommand(value).getCommandLineBytes());
    }

    /**
     * Tests that a value needing a literal is still held to ascii, since the octets are written as ascii and a wider character would be
     * silently replaced, changing the value and desynchronising the octet count.
     */
    @Test
    public void testLiteralIsStillHeldToAscii() {
        assertRejected("LOGIN username with CRLF and a wide character",
            () -> new LoginCommand("a\r\n\u00a9", "s3cret").getCommandLineBytes());
    }

    /**
     * Tests that a continuation request arriving before the command line was built is reported rather than acted on.
     */
    @Test
    public void testContinuationBeforeCommandLineIsRefused() {
        ImapAsyncClientException actual = null;
        try {
            new LoginCommand("alice", "s3cret").getNextCommandLineAfterContinuation(null);
        } catch (final ImapAsyncClientException e) {
            actual = e;
        }
        Assert.assertNotNull(actual, "A continuation before the command line should be refused.");
        Assert.assertEquals(actual.getFailureType(), FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND, "Failure type mismatched.");
    }

    /**
     * Tests the flag vectors. A flag-keyword is an atom, so it has no quoted or literal form to fall back on and every control character is
     * refused, not only CR and LF.
     */
    @Test
    public void testUserFlagsRejectControlChars() {
        final Flags crlf = new Flags();
        crlf.add(CRLF_PAYLOAD);
        assertRejected("STORE user flag with CRLF", () -> new StoreFlagsCommand("1:2", crlf, FlagsAction.ADD, false).getCommandLineBytes());
        assertRejected("APPEND user flag with CRLF", () -> new AppendCommand("INBOX", crlf, new Date(0), "hi".getBytes()).getCommandLineBytes());

        final Flags soh = new Flags();
        soh.add(SOH_PAYLOAD);
        assertRejected("STORE user flag with SOH", () -> new StoreFlagsCommand("1:2", soh, FlagsAction.ADD, false).getCommandLineBytes());
    }

    /**
     * Tests the remaining atom-typed arguments found while triaging.
     */
    @Test
    public void testAtomArgumentsRejectControlChars() {
        assertRejected("ENABLE capability", () -> new EnableCommand(new String[] { CRLF_PAYLOAD }).getCommandLineBytes());
        assertRejected("FETCH data item",
            () -> new FetchCommand(new MessageNumberSet[] { new MessageNumberSet(1, 2) }, CRLF_PAYLOAD).getCommandLineBytes());
        assertRejected("UID EXPUNGE uids", () -> new UidExpungeCommand(CRLF_PAYLOAD).getCommandLineBytes());
        assertRejected("STATUS item", () -> new StatusCommand("INBOX", new String[] { CRLF_PAYLOAD }).getCommandLineBytes());
        assertRejected("STORE message numbers",
            () -> new StoreFlagsCommand(CRLF_PAYLOAD, new Flags(Flags.Flag.SEEN), FlagsAction.ADD, false).getCommandLineBytes());
        assertRejected("SEARCH message numbers", () -> new SearchCommand(CRLF_PAYLOAD, null, new Argument(), null).getCommandLineBytes());
    }

    /**
     * Tests the SASL payload vectors. Base64 stops these from breaking the command line, but the separators inside the decoded payload are
     * still caller-controlled: NUL splits the AUTH=PLAIN fields and SOH splits the XOAUTH2 and OAUTHBEARER ones.
     */
    @Test
    public void testSaslFieldsRejectSeparators() {
        final Capability capa = saslIrCapability();
        assertRejected("AUTH PLAIN username with NUL", () -> new AuthPlainCommand(NUL_PAYLOAD, "pw", capa).getCommandLineBytes());
        assertRejected("AUTH PLAIN password with NUL", () -> new AuthPlainCommand("alice", NUL_PAYLOAD, capa).getCommandLineBytes());
        assertRejected("AUTH PLAIN authid with NUL", () -> new AuthPlainCommand(NUL_PAYLOAD, "alice", "pw", capa).getCommandLineBytes());
        assertRejected("XOAUTH2 username with SOH", () -> new AuthXoauth2Command(SOH_PAYLOAD, "tok", capa).getCommandLineBytes());
        assertRejected("XOAUTH2 token with SOH", () -> new AuthXoauth2Command("alice", SOH_PAYLOAD, capa).getCommandLineBytes());
        assertRejected("OAUTHBEARER email with SOH", () -> new AuthOauthBearerCommand(SOH_PAYLOAD, "host", 993, "tok", capa).getCommandLineBytes());
    }

    /**
     * Tests that the SASL fields are refused on the continuation path too, which is the path taken when the server does not advertise SASL-IR.
     */
    @Test
    public void testSaslFieldsRejectedOnContinuation() {
        final Capability noSaslIr = new Capability(new HashMap<String, List<String>>());
        final AuthXoauth2Command cmd = new AuthXoauth2Command(SOH_PAYLOAD, "tok", noSaslIr);
        ImapAsyncClientException actual = null;
        try {
            cmd.getCommandLineBytes(); // no SASL-IR, so this emits only "AUTHENTICATE XOAUTH2"
            cmd.getNextCommandLineAfterContinuation(null); // the credential reaches the wire here
        } catch (final ImapAsyncClientException e) {
            actual = e;
        }
        Assert.assertNotNull(actual, "XOAUTH2 username with SOH should be rejected on the continuation path.");
        Assert.assertEquals(actual.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests every code point that must be refused from an atom, and confirms the astring path still refuses the three it cannot represent.
     */
    @Test
    public void testEveryControlCharacterIsRejected() {
        final List<Character> rejected = new ArrayList<Character>();
        for (char c = 0; c <= 0x1F; c++) {
            rejected.add(c);
        }
        rejected.add((char) 0x7F);

        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
        for (final char c : rejected) {
            ImapAsyncClientException actual = null;
            try {
                formatter.validateAtom("a" + c + "b", "test");
            } catch (final ImapAsyncClientException e) {
                actual = e;
            }
            Assert.assertNotNull(actual, "atom with 0x" + Integer.toHexString(c) + " should be rejected.");
        }
    }

    /**
     * Tests that legitimate arguments are unaffected, including the ones that exercise quoting and escaping.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testLegitimateArgumentsUnchanged() throws ImapAsyncClientException {
        Assert.assertEquals(new LoginCommand("alice", "s3cret").getCommandLineBytes().toString(StandardCharsets.US_ASCII),
                "LOGIN alice s3cret\r\n", "A benign login should be unchanged.");

        // the control case from the report: quoting and escaping still apply, so only the refused characters were ever mishandled
        Assert.assertEquals(new LoginCommand("ali\"ce", "pa\\ss").getCommandLineBytes().toString(StandardCharsets.US_ASCII),
                "LOGIN \"ali\\\"ce\" \"pa\\\\ss\"\r\n", "Quoting and escaping should be unchanged.");

        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add("$Forwarded");
        Assert.assertEquals(new StoreFlagsCommand("1:2", flags, FlagsAction.ADD, false).getCommandLineBytes().toString(StandardCharsets.US_ASCII),
                "STORE 1:2 +FLAGS (\\Seen $Forwarded)\r\n", "A benign store should be unchanged.");

        Assert.assertEquals(new StatusCommand("INBOX", new String[] { "MESSAGES", "UIDNEXT" }).getCommandLineBytes()
                .toString(StandardCharsets.US_ASCII), "STATUS INBOX (MESSAGES UIDNEXT)\r\n", "A benign status should be unchanged.");

        Assert.assertEquals(new EnableCommand(new String[] { "CONDSTORE" }).getCommandLineBytes().toString(StandardCharsets.US_ASCII),
                "ENABLE CONDSTORE\r\n", "A benign enable should be unchanged.");

        final String plain = new AuthPlainCommand("alice", "s3cret", saslIrCapability()).getCommandLineBytes()
                .toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(new String(Base64.decodeBase64(plain.split(" ")[2].trim()), StandardCharsets.UTF_8), "\u0000alice\u0000s3cret",
                "A benign AUTH PLAIN payload should be unchanged.");
    }

    /**
     * Tests that a command which cannot send a synchronizing literal says so rather than sending a fragment of a command line.
     *
     * <p>
     * No caller can reach this today, since every mailbox name is run through the modified UTF-7 encoder first, but the guard is what keeps
     * a future change to UTF-8 mailbox names from silently truncating a command.
     * </p>
     */
    @Test
    public void testSingleSegmentCommandRefusesToSplit() {
        ImapAsyncClientException actual = null;
        try {
            new ImapCommandLineBuilder(LiteralSupport.DISABLE).astring("a\r\nb", false, "folder name").finishSingle();
        } catch (final ImapAsyncClientException e) {
            actual = e;
        }
        Assert.assertNotNull(actual, "A line that had to split cannot be returned whole.");
        Assert.assertEquals(actual.getFailureType(), FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND, "Failure type mismatched.");

        ImapAsyncClientException open = null;
        try {
            new ImapCommandLineBuilder(LiteralSupport.DISABLE).astring("a\r\nb", false, "folder name").openSingle();
        } catch (final ImapAsyncClientException e) {
            open = e;
        }
        Assert.assertNotNull(open, "An unterminated line that had to split cannot be returned whole either.");
        Assert.assertEquals(open.getFailureType(), FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND, "Failure type mismatched.");
    }

    /**
     * Tests that a sequence set is held to its own shape rather than to the atom rule.
     *
     * <p>
     * A sequence set holds digits, {@code :}, {@code ,} and {@code *}. The last of those is a list-wildcard that atom-specials keeps out of an
     * atom, so checking one as an atom would reject a perfectly ordinary {@code 1:*}.
     * </p>
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testSequenceSetHasItsOwnRule() throws ImapAsyncClientException {
        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
        Assert.assertEquals(formatter.validateSequenceSet("1:*", "uid"), "1:*", "A wildcard is part of a sequence set.");
        Assert.assertEquals(formatter.validateSequenceSet("*:4,5:7", "uid"), "*:4,5:7", "Ranges and lists are part of a sequence set.");

        assertRejected("a sequence set with a space", () -> new UidExpungeCommand("1:* 99").getCommandLineBytes());
        assertRejected("an alphabetic sequence set", () -> new UidExpungeCommand("DROP").getCommandLineBytes());
        assertRejected("a sequence set with a parenthesis", () -> new UidExpungeCommand("1:2)").getCommandLineBytes());
    }

    /**
     * Tests that an atom is held to atom-specials, not merely to being free of control characters.
     *
     * <p>
     * Letting a space or a parenthesis through would let a caller add a token of their own to the command, since these values are written into
     * the command line bare.
     * </p>
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testAtomIsHeldToAtomSpecials() throws ImapAsyncClientException {
        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
        Assert.assertEquals(formatter.validateAtom("CONDSTORE", "capability"), "CONDSTORE", "A plain keyword is an atom.");
        Assert.assertEquals(formatter.validateAtom("AUTH=PLAIN", "capability"), "AUTH=PLAIN", "'=' is not an atom-special.");
        Assert.assertEquals(formatter.validateAtom("$Forwarded", "flag"), "$Forwarded", "'$' is not an atom-special.");

        for (final String special : new String[] { "A B", "A(B", "A)B", "A{B", "A%B", "A*B", "A\"B", "A\\B", "A]B" }) {
            ImapAsyncClientException actual = null;
            try {
                formatter.validateAtom(special, "capability");
            } catch (final ImapAsyncClientException e) {
                actual = e;
            }
            Assert.assertNotNull(actual, "atom-specials should keep " + special + " out of an atom.");
            Assert.assertEquals(actual.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
        }

        assertRejected("a capability holding a space", () -> new EnableCommand(new String[] { "X Y" }).getCommandLineBytes());
        assertRejected("a status item holding a space", () -> new StatusCommand("INBOX", new String[] { "MESSAGES UIDNEXT" }).getCommandLineBytes());
    }

    /**
     * Tests that a fetch item may hold the delimiters its own production needs, but may not close a group it did not open.
     *
     * <p>
     * {@code FLAGS) (BODY} would otherwise escape the list the caller was given and add arguments of its own to the FETCH.
     * </p>
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testFetchItemDelimitersMustBalance() throws ImapAsyncClientException {
        final MessageNumberSet[] set = new MessageNumberSet[] { new MessageNumberSet(1, 2) };
        Assert.assertEquals(new FetchCommand(set, "FLAGS BODY[HEADER.FIELDS (DATE FROM)]").getCommandLineBytes()
                .toString(StandardCharsets.US_ASCII), "FETCH 1:2 (FLAGS BODY[HEADER.FIELDS (DATE FROM)])\r\n",
                "A fetch item may hold spaces, brackets and balanced parentheses.");

        assertRejected("a fetch item closing a group it did not open",
            () -> new FetchCommand(set, "FLAGS) (BODY").getCommandLineBytes());
        assertRejected("a fetch item leaving a group open",
            () -> new FetchCommand(set, "BODY[HEADER").getCommandLineBytes());
    }

    /**
     * Tests that a folder name carrying CRLF is still handled by the modified UTF-7 encoder rather than reaching the formatter raw.
     *
     * <p>
     * This is the behaviour that made the folder commands safe before the fix, and it is asserted here so that a future move to the UTF-8
     * mailbox names of RFC 9051, which would drop this encoder, cannot silently reopen the vector.
     * </p>
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testFolderNamesStillEncoded() throws ImapAsyncClientException {
        Assert.assertEquals(new SelectFolderCommand("INBOX\r\nX1 LOGOUT").getCommandLineBytes().toString(StandardCharsets.US_ASCII),
                "SELECT \"INBOX&AA0ACg-X1 LOGOUT\"\r\n", "CR and LF in a folder name should be encoded, not written raw.");
    }
}
