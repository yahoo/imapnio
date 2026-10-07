package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

/**
 * Unit tests for {@link ImapCommandLineBuilder}, which decides between the atom, quoted and literal forms of an astring.
 *
 * <p>
 * The inline cases here were previously written against the argument formatter directly. Choosing the form is this class's job now, so they
 * are exercised through it; the expected bytes are unchanged.
 * </p>
 */
public class ImapCommandLineBuilderTest {

    /**
     * Writes one astring and returns the command line it produced, without the terminating CRLF.
     *
     * @param src the value to write
     * @param doQuote whether to quote it when it is written inline
     * @return the command line, less its terminator
     * @throws ImapAsyncClientException when the value is refused
     */
    private String inline(final String src, final boolean doQuote) throws ImapAsyncClientException {
        final String line = new ImapCommandLineBuilder(LiteralSupport.DISABLE).astring(src, doQuote, "arg").finishSingle()
                .toString(StandardCharsets.US_ASCII);
        return line.substring(0, line.length() - 2);
    }

    /**
     * Asserts that writing the given value is refused with the given failure type.
     *
     * @param src the value to write
     * @param expected the failure type expected
     * @param message what the assertion is showing
     */
    private void assertRefused(final String src, final FailureType expected, final String message) {
        ImapAsyncClientException actual = null;
        try {
            new ImapCommandLineBuilder(LiteralSupport.DISABLE).astring(src, false, "arg");
        } catch (final ImapAsyncClientException e) {
            actual = e;
        }
        Assert.assertNotNull(actual, message);
        Assert.assertEquals(actual.getFailureType(), expected, message);
    }

    /**
     * Tests a value that needs neither quoting nor a literal.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testSourceNoQuoteNeeded() throws ImapAsyncClientException {
        Assert.assertEquals(inline("Bulk", false), "Bulk", "Encoded result mismatched.");
    }

    /**
     * Tests the characters that force a value to be quoted, and the two that are also escaped.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testSourceNeedsQuote() throws ImapAsyncClientException {
        // if (b == '*' || b == '%' || b == '(' || b == ')' || b == '{' || b == '"' || b == '\\' || ((b & 0xff) <= ' ')) {
        Assert.assertEquals(inline("B*", false), "\"B*\"", "Encoded result mismatched.");
        Assert.assertEquals(inline("B%", false), "\"B%\"", "Encoded result mismatched.");
        Assert.assertEquals(inline("B(", false), "\"B(\"", "Encoded result mismatched.");
        Assert.assertEquals(inline("B)", false), "\"B)\"", "Encoded result mismatched.");
        Assert.assertEquals(inline("B{", false), "\"B{\"", "Encoded result mismatched.");
        Assert.assertEquals(inline("B\"", false), "\"B\\\"\"", "Encoded result mismatched."); // expects double quote and escape
        Assert.assertEquals(inline("B\\", false), "\"B\\\\\"", "Encoded result mismatched."); // expects double quote and escape

        final char c = '\u0011'; // special char less than space ascii code
        Assert.assertEquals(inline(Character.toString(c), false), "\"\u0011\"", "Encoded result mismatched.");

        Assert.assertEquals(inline("", false), "\"\"", "Encoded result mismatched."); // empty string
    }

    /**
     * Tests that a value spelling NIL is quoted so it cannot be read as a real NIL.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testHasLiteralNIL() throws ImapAsyncClientException {
        Assert.assertEquals(inline("NIL", false), "\"NIL\"", "Encoded result mismatched.");
        Assert.assertEquals(inline("nil", false), "\"nil\"", "Encoded result mismatched.");
        // test false positive: qualify first 2 letters
        Assert.assertEquals(inline("NIi", false), "NIi", "Encoded result mismatched.");
        // test false positive: qualify first letter
        Assert.assertEquals(inline("NxL", false), "NxL", "Encoded result mismatched.");
        // test false positive: 3 chars
        Assert.assertEquals(inline("LIN", false), "LIN", "Encoded result mismatched.");
    }

    /**
     * Tests that a control character a quoted string can legitimately carry is quoted rather than refused.
     *
     * <p>
     * A quoted string is built from TEXT-CHAR, "any CHAR except CR and LF", so every control character other than those two, and other than
     * NUL which CHAR is already without, is valid inside one.
     * </p>
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testControlCharsOtherThanCrLfAreQuoted() throws ImapAsyncClientException {
        Assert.assertEquals(inline("a\u0001b", false), "\"a\u0001b\"", "SOH is valid inside a quoted string.");
        Assert.assertEquals(inline("a\u001fb", false), "\"a\u001fb\"", "0x1F is valid inside a quoted string.");
        Assert.assertEquals(inline("a\u007fb", false), "\"a\u007fb\"", "DEL is valid inside a quoted string.");
    }

    /**
     * Tests that CR and LF are carried by a literal rather than refused, and that the value follows in the next piece of the line.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testCrLfIsCarriedByALiteral() throws ImapAsyncClientException {
        final java.util.List<io.netty.buffer.ByteBuf> segments = new ImapCommandLineBuilder(LiteralSupport.DISABLE)
                .astring("a\r\nb", false, "arg").finish();
        Assert.assertEquals(segments.size(), 2, "A synchronizing literal should split the command line.");
        Assert.assertEquals(segments.get(0).toString(StandardCharsets.US_ASCII), "{4}\r\n", "The octet count should come first.");
        Assert.assertEquals(segments.get(1).toString(StandardCharsets.US_ASCII), "a\r\nb\r\n", "The octets should follow the continuation.");
    }

    /**
     * Tests the two values no form of an astring can carry, and the one this builder holds to ascii.
     */
    @Test
    public void testRefusedValues() {
        // CHAR8 excludes NUL, so not even a literal can carry it
        assertRefused("a\u0000b", FailureType.INVALID_INPUT, "NUL cannot be represented by any astring form.");
        // the inline path writes ascii, so a wider character would be silently replaced
        assertRefused("a©b", FailureType.INVALID_INPUT, "A character above ascii 127 is outside what is written.");
        // the literal path writes ascii too, and the octet count has to match what is written
        assertRefused("a\r\n©", FailureType.INVALID_INPUT, "A literal is held to ascii as well.");
    }
}
