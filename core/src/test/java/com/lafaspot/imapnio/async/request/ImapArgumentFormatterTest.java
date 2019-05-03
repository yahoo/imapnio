package com.lafaspot.imapnio.async.request;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.mail.Flags;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.async.request.ImapArgumentFormatter;

/**
 * Unit test for {@code ImapArgumentFormatter}.
 */
public class ImapArgumentFormatterTest {

    /**
     * Tests when source string has space.
     *
     * @throws IOException will not throw
     */
    @Test
    public void testSourceNoQuoteNeeded() throws IOException {
        final ImapArgumentFormatter writer = new ImapArgumentFormatter();
        final String src = "Bulk";
        final StringBuilder out = new StringBuilder();
        final boolean doQuote = false;
        writer.formatArgument(src, out, doQuote);
        Assert.assertEquals(out.toString(), "Bulk", "Encoded result mismatched.");
    }

    /**
     * Tests when source string has CR or LF or \0 or special chars(>127).
     *
     * @throws IOException will not throw
     */
    @Test
    public void testSourceAsItIs() throws IOException {
        // \r
        {
            final ImapArgumentFormatter writer = new ImapArgumentFormatter();
            final byte[] bytes = { 'a', '\r' };
            final String src = new String(bytes);
            final StringBuilder out = new StringBuilder();
            final boolean doQuote = false;
            writer.formatArgument(src, out, doQuote);
            Assert.assertEquals(out.toString(), "a\r", "Encoded result mismatched.");
        }
        // \n
        {
            final ImapArgumentFormatter writer = new ImapArgumentFormatter();
            final byte[] bytes = { 'a', '\n' };
            final String src = new String(bytes, StandardCharsets.US_ASCII);
            final StringBuilder out = new StringBuilder();
            final boolean doQuote = false;
            writer.formatArgument(src, out, doQuote);
            Assert.assertEquals(out.toString(), "a\n", "Encoded result mismatched.");
        }
        // \0
        {
            final ImapArgumentFormatter writer = new ImapArgumentFormatter();
            final byte[] bytes = { 'a', '\0' };
            final String src = new String(bytes, StandardCharsets.US_ASCII);
            final StringBuilder out = new StringBuilder();
            final boolean doQuote = false;
            writer.formatArgument(src, out, doQuote);
            Assert.assertEquals(out.toString(), "a\0", "Encoded result mismatched.");
        }
        // Char ascii code > 127
        {
            final ImapArgumentFormatter writer = new ImapArgumentFormatter();
            final char c = '\u03A9'; // Omega character
            final String src = Character.toString(c);
            final StringBuilder out = new StringBuilder();
            final boolean doQuote = false;
            writer.formatArgument(src, out, doQuote);
            Assert.assertEquals(out.toString(), "Ω", "Encoded result mismatched.");
        }
    }

    /**
     * Tests when source string has character that needs to be double quoted.
     *
     * @throws IOException will not throw
     */
    @Test
    public void testSourceNeedsQuote() throws IOException {
        // if (b == '*' || b == '%' || b == '(' || b == ')' || b == '{' || b == '"' || b == '\\' || ((b & 0xff) <= ' ')) {
        final ImapArgumentFormatter writer = new ImapArgumentFormatter();
        final boolean doQuote = false;
        {
            final StringBuilder out = new StringBuilder();
            writer.formatArgument("B*", out, doQuote);
            Assert.assertEquals(out.toString(), "\"B*\"", "Encoded result mismatched.");
        }
        {
            final StringBuilder out = new StringBuilder();
            writer.formatArgument("B%", out, doQuote);
            Assert.assertEquals(out.toString(), "\"B%\"", "Encoded result mismatched.");
        }
        {
            final StringBuilder out = new StringBuilder();
            writer.formatArgument("B(", out, doQuote);
            Assert.assertEquals(out.toString(), "\"B(\"", "Encoded result mismatched.");
        }
        {
            final StringBuilder out = new StringBuilder();
            writer.formatArgument("B)", out, doQuote);
            Assert.assertEquals(out.toString(), "\"B)\"", "Encoded result mismatched.");
        }
        {
            final StringBuilder out = new StringBuilder();
            writer.formatArgument("B{", out, doQuote);
            Assert.assertEquals(out.toString(), "\"B{\"", "Encoded result mismatched.");
        }
        {
            final StringBuilder out = new StringBuilder();
            writer.formatArgument("B\"", out, doQuote); // excpects double quote and escape
            Assert.assertEquals(out.toString(), "\"B\\\"\"", "Encoded result mismatched.");
        }
        {
            final StringBuilder out = new StringBuilder();
            writer.formatArgument("B\\", out, doQuote); // excpects double quote and escape
            Assert.assertEquals(out.toString(), "\"B\\\\\"", "Encoded result mismatched.");
        }
        {
            final StringBuilder out = new StringBuilder();
            final char c = '\u0011'; // spcial char less than space ascii code
            final String src = Character.toString(c);
            writer.formatArgument(src, out, doQuote);
            Assert.assertEquals(out.toString(), "\"\"", "Encoded result mismatched.");
        }
        {
            final StringBuilder out = new StringBuilder();
            final String src = ""; // empty string
            writer.formatArgument(src, out, doQuote);
            Assert.assertEquals(out.toString(), "\"\"", "Encoded result mismatched.");
        }
    }

    /**
     * Tests when source has NIL literal.
     *
     * @throws IOException will not throw
     */
    @Test
    public void testHasLiteralNIL() throws IOException {
        final ImapArgumentFormatter writer = new ImapArgumentFormatter();
        final boolean doQuote = false;
        {
            final StringBuilder out = new StringBuilder();
            writer.formatArgument("NIL", out, doQuote);
            Assert.assertEquals(out.toString(), "\"NIL\"", "Encoded result mismatched.");
        }
        {
            final StringBuilder out = new StringBuilder();
            writer.formatArgument("nil", out, doQuote);
            Assert.assertEquals(out.toString(), "\"nil\"", "Encoded result mismatched.");
        }
        // test false positive: qualify first 2 letters
        {
            final StringBuilder out = new StringBuilder();
            writer.formatArgument("NIi", out, doQuote);
            Assert.assertEquals(out.toString(), "NIi", "Encoded result mismatched.");
        }
        // test false positive: qualify first letter
        {
            final StringBuilder out = new StringBuilder();
            writer.formatArgument("NxL", out, doQuote);
            Assert.assertEquals(out.toString(), "NxL", "Encoded result mismatched.");
        }
        // test false positive: 3 chars
        {
            final StringBuilder out = new StringBuilder();
            writer.formatArgument("LIN", out, doQuote);
            Assert.assertEquals(out.toString(), "LIN", "Encoded result mismatched.");
        }
    }

    /**
     * Tests buildFlagString with all flags.
     */
    @Test
    public void testBuildFlagString() {
        final Flags flags = new Flags();
        flags.add(Flags.Flag.ANSWERED);
        flags.add(Flags.Flag.DELETED);
        flags.add(Flags.Flag.DRAFT);
        flags.add(Flags.Flag.FLAGGED);
        flags.add(Flags.Flag.RECENT);
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.USER); // for continue
        final ImapArgumentFormatter writer = new ImapArgumentFormatter();
        final String s = writer.buildFlagString(flags);
        Assert.assertNotNull(s, "buildFlagString() should not return null.");
        Assert.assertEquals(s, "(\\Answered \\Deleted \\Draft \\Flagged \\Recent \\Seen)", "result mismatched.");
    }

    /**
     * Tests buildFlagString with user flags.
     */
    @Test
    public void testBuildFlagStringWithUserFlagString() {
        final Flags flags = new Flags();
        flags.add("userflag1");
        flags.add("userflag2");
        final ImapArgumentFormatter writer = new ImapArgumentFormatter();
        final String s = writer.buildFlagString(flags);
        Assert.assertNotNull(s, "buildFlagString() should not return null.");
        Assert.assertEquals(s, "(userflag1 userflag2)", "result mismatched.");
    }
}
