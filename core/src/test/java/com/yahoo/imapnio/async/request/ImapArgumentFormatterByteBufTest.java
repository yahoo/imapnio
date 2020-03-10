package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit test for {@link ImapArgumentFormatter}.
 */
public class ImapArgumentFormatterByteBufTest {

    /**
     * Tests when source string has space.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testSourceNoQuoteNeeded() throws ImapAsyncClientException {
        final ImapArgumentFormatter writer = new ImapArgumentFormatter();
        final String src = "Bulk";
        final ByteBuf out = Unpooled.buffer();
        final boolean doQuote = false;
        writer.formatArgument(src, out, doQuote);
        Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "Bulk", "Encoded result mismatched.");
    }

    /**
     * Tests when source string has CR or LF or \0 or special chars(>127).
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testSourceAsItIs() throws ImapAsyncClientException {
        // \r
        {
            final ImapArgumentFormatter writer = new ImapArgumentFormatter();
            final byte[] bytes = { 'a', '\r' };
            final String src = new String(bytes);
            final ByteBuf out = Unpooled.buffer();
            final boolean doQuote = false;
            writer.formatArgument(src, out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "a\r", "Encoded result mismatched.");
        }
        // \n
        {
            final ImapArgumentFormatter writer = new ImapArgumentFormatter();
            final byte[] bytes = { 'a', '\n' };
            final String src = new String(bytes, StandardCharsets.US_ASCII);
            final ByteBuf out = Unpooled.buffer();
            final boolean doQuote = false;
            writer.formatArgument(src, out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "a\n", "Encoded result mismatched.");
        }
        // \0
        {
            final ImapArgumentFormatter writer = new ImapArgumentFormatter();
            final byte[] bytes = { 'a', '\0' };
            final String src = new String(bytes, StandardCharsets.US_ASCII);
            final ByteBuf out = Unpooled.buffer();
            final boolean doQuote = false;
            writer.formatArgument(src, out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "a\0", "Encoded result mismatched.");
        }
        // Char ascii code > 127
        {
            final ImapArgumentFormatter writer = new ImapArgumentFormatter();
            final char c = '\u03A9'; // Omega character
            final String src = Character.toString(c);
            final ByteBuf out = Unpooled.buffer();
            final boolean doQuote = false;
            ImapAsyncClientException actual = null;
            try {
                writer.formatArgument(src, out, doQuote);
            } catch (final ImapAsyncClientException e) {
                actual = e;
            }
            Assert.assertNotNull(actual, "Should throw exception");
            Assert.assertEquals(actual.getFailureType(), ImapAsyncClientException.FailureType.INVALID_INPUT, "Should throw exception");
        }
    }

    /**
     * Tests when source string has character that needs to be double quoted.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testSourceNeedsQuote() throws ImapAsyncClientException {
        // if (b == '*' || b == '%' || b == '(' || b == ')' || b == '{' || b == '"' || b == '\\' || ((b & 0xff) <= ' ')) {
        final ImapArgumentFormatter writer = new ImapArgumentFormatter();
        final boolean doQuote = false;
        {
            final ByteBuf out = Unpooled.buffer();
            writer.formatArgument("B*", out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "\"B*\"", "Encoded result mismatched.");
        }
        {
            final ByteBuf out = Unpooled.buffer();
            writer.formatArgument("B%", out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "\"B%\"", "Encoded result mismatched.");
        }
        {
            final ByteBuf out = Unpooled.buffer();
            writer.formatArgument("B(", out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "\"B(\"", "Encoded result mismatched.");
        }
        {
            final ByteBuf out = Unpooled.buffer();
            writer.formatArgument("B)", out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "\"B)\"", "Encoded result mismatched.");
        }
        {
            final ByteBuf out = Unpooled.buffer();
            writer.formatArgument("B{", out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "\"B{\"", "Encoded result mismatched.");
        }
        {
            final ByteBuf out = Unpooled.buffer();
            writer.formatArgument("B\"", out, doQuote); // expects double quote and escape
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "\"B\\\"\"", "Encoded result mismatched.");
        }
        {
            final ByteBuf out = Unpooled.buffer();
            writer.formatArgument("B\\", out, doQuote); // expects double quote and escape
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "\"B\\\\\"", "Encoded result mismatched.");
        }
        {
            final ByteBuf out = Unpooled.buffer();
            final char c = '\u0011'; // special char less than space ascii code
            final String src = Character.toString(c);
            writer.formatArgument(src, out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "\"\"", "Encoded result mismatched.");
        }
        {
            final ByteBuf out = Unpooled.buffer();
            final String src = ""; // empty string
            writer.formatArgument(src, out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "\"\"", "Encoded result mismatched.");
        }
    }

    /**
     * Tests when source has NIL literal.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testHasLiteralNIL() throws ImapAsyncClientException {
        final ImapArgumentFormatter writer = new ImapArgumentFormatter();
        final boolean doQuote = false;
        {
            final ByteBuf out = Unpooled.buffer();
            writer.formatArgument("NIL", out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "\"NIL\"", "Encoded result mismatched.");
        }
        {
            final ByteBuf out = Unpooled.buffer();
            writer.formatArgument("nil", out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "\"nil\"", "Encoded result mismatched.");
        }
        // test false positive: qualify first 2 letters
        {
            final ByteBuf out = Unpooled.buffer();
            writer.formatArgument("NIi", out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "NIi", "Encoded result mismatched.");
        }
        // test false positive: qualify first letter
        {
            final ByteBuf out = Unpooled.buffer();
            writer.formatArgument("NxL", out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "NxL", "Encoded result mismatched.");
        }
        // test false positive: 3 chars
        {
            final ByteBuf out = Unpooled.buffer();
            writer.formatArgument("LIN", out, doQuote);
            Assert.assertEquals(out.toString(StandardCharsets.US_ASCII), "LIN", "Encoded result mismatched.");
        }
    }

}
