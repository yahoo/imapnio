package com.yahoo.imapnio.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * Unit test for {@link ImapClientRespReader}.
 *
 * @author kaituo
 *
 */
public class ImapClientRespReaderTest {

    /**
     * Tests parsing single line response.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testDecodeLineResponse() throws Exception {
        final String response = "A1 OK LOGIN completed\r\n";
        final byte[] responseBytes = response.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf = Unpooled.copiedBuffer(responseBytes, 0, responseBytes.length);
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final ByteBuf resultBuf = (ByteBuf) respReader.decode(null, inputBuf);
        Assert.assertEquals(inputBuf.readableBytes(), 0, "readable bytes should be exhausted since there is a line.");
        final String result = resultBuf.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result, response);
    }

    /**
     * Tests parsing literal response.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testDecodeLiteralResponse() throws Exception {
        final String literalResponse = "* 1 FETCH BODY[HEADER] {10}\r\n" + "he: ader\r\n" + "\r\n";
        final byte[] responseBytes = literalResponse.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf = Unpooled.copiedBuffer(responseBytes, 0, responseBytes.length);
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final ByteBuf resultBuf = (ByteBuf) respReader.decode(null, inputBuf);
        Assert.assertEquals(inputBuf.readableBytes(), 0, "readable bytes should be exhausted.");
        final String result = resultBuf.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result, literalResponse);
    }

    /**
     * Tests parsing multiple literal responses.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testDecodeMultipleLiteralResponse() throws Exception {
        final String literalResponse = "* 1 FETCH BODY {15}\r\n" + "abc\r\n" + "defghijklm BODY[HEADER] {5}\r\n" + "h:j\r\n" + "\r\n";
        final byte[] responseBytes = literalResponse.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf = Unpooled.copiedBuffer(responseBytes, 0, responseBytes.length);
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final ByteBuf resultBuf = (ByteBuf) respReader.decode(null, inputBuf);
        Assert.assertEquals(inputBuf.readableBytes(), 0, "readable bytes should be exhausted.");
        final String result = resultBuf.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result, literalResponse, "Data mismatched.");
    }

    /**
     * Tests when all literals data is in one input inputBuf but the newline is second.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testDecodeLiteralResponseEndingInNextBuffer() throws Exception {
        // first buffer has all the literals (aka 15 bytes)
        final String literalResponse1 = "* 1 FETCH (BODY[] {15}\r\n" + "abcdefghijklm\r\n";
        final byte[] responseBytes1 = literalResponse1.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf1 = Unpooled.copiedBuffer(responseBytes1, 0, responseBytes1.length);
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final ByteBuf resultBuf1 = (ByteBuf) respReader.decode(null, inputBuf1);
        Assert.assertEquals(inputBuf1.readableBytes(), 0, "readable bytes should be exhausted.");
        Assert.assertNull(resultBuf1, "should not return the bytebuf");

        final String literalResponse2 = ")\r\n";
        final byte[] responseBytes2 = literalResponse2.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf2 = Unpooled.copiedBuffer(responseBytes2, 0, responseBytes2.length);
        final ByteBuf resultBuf2 = (ByteBuf) respReader.decode(null, inputBuf2);
        Assert.assertEquals(inputBuf2.readableBytes(), 0, "readable bytes should be exhausted.");
        final String result2 = resultBuf2.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result2, literalResponse1 + literalResponse2, "data mismatched.");
    }

    /**
     * Tests when literals data spans over 2 input buffers.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testDecodeLiteralResponseSpans2Buffer() throws Exception {
        // first buffer has partial literals we need
        final String literalResponse1 = "* 1 FETCH (BODY[] {15}\r\n" + "abcdefgh";
        final byte[] responseBytes1 = literalResponse1.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf1 = Unpooled.copiedBuffer(responseBytes1, 0, responseBytes1.length);
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final ByteBuf resultBuf1 = (ByteBuf) respReader.decode(null, inputBuf1);
        Assert.assertEquals(inputBuf1.readableBytes(), 0, "readable bytes should be exhausted.");
        Assert.assertNull(resultBuf1, "should not return the result");

        // second round completes all
        final String literalResponse2 = "ijklm\r\n)\r\n";
        final byte[] responseBytes2 = literalResponse2.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf2 = Unpooled.copiedBuffer(responseBytes2, 0, responseBytes2.length);
        final ByteBuf resultBuf2 = (ByteBuf) respReader.decode(null, inputBuf2);
        Assert.assertEquals(inputBuf2.readableBytes(), 0, "readable bytes should be exhausted.");
        final String result2 = resultBuf2.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result2, literalResponse1 + literalResponse2, "data mismatched.");
    }

    /**
     * Tests parsing multiple literal responses, but the second literal '{5}' is still in first literal response count. Expects to treat them as
     * literals, also it has extra \r\n.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testDecodeLiteralResponseWhenSecondLiteralStillInFirstLiteralCount() throws Exception {
        final String literalResponse = "* 1 FETCH BODY {20}\r\n" + "abc\r\n" + " BODY[HEADER]{5}\r\n" + "\r\n";
        final byte[] responseBytes = literalResponse.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf = Unpooled.copiedBuffer(responseBytes, 0, responseBytes.length);
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final ByteBuf resultBuf = (ByteBuf) respReader.decode(null, inputBuf);
        final String result = resultBuf.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result, "* 1 FETCH BODY {20}\r\nabc\r\n BODY[HEADER]{5}\r\n", "decode() result mismatched.");
        Assert.assertEquals(inputBuf.readableBytes(), 2, "should have 2 bytes left since it is extra.");
    }

    /**
     * Tests decode empty response.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testDecodeEmptyResponse() throws Exception {
        final String literalResponse = "";
        final byte[] responseBytes = literalResponse.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf = Unpooled.copiedBuffer(responseBytes, 0, responseBytes.length);
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final ByteBuf resultBuf = (ByteBuf) respReader.decode(null, inputBuf);
        Assert.assertNull(resultBuf, "Should return null");
        Assert.assertEquals(inputBuf.readerIndex(), 0, "ReaderIndex should not be moved.");
    }

    /**
     * Tests input buffer does not have CRLF. Expects not to loose the previous chunk and return complete response till next chunk has CRLF.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testDecodeDataNoCRLFInFirstDecode() throws Exception {
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final String literalResponse1 = "NO CRLF YET";
        final byte[] responseBytes1 = literalResponse1.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf1 = Unpooled.copiedBuffer(responseBytes1, 0, responseBytes1.length);

        final String literalResponse2 = literalResponse1 + ", and here u go with CRLF!\r\n";
        final byte[] responseBytes2 = literalResponse2.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf2 = Unpooled.copiedBuffer(responseBytes2, 0, responseBytes2.length);

        final ByteBuf resultBuf1 = (ByteBuf) respReader.decode(null, inputBuf1);
        Assert.assertNull(resultBuf1, "Should return null because not reaching a line yet");
        Assert.assertEquals(inputBuf1.readerIndex(), 0, "ReaderIndex should not be moved.");

        final ByteBuf resultBuf2 = (ByteBuf) respReader.decode(null, inputBuf2);
        Assert.assertNotNull(resultBuf2, "Should return null because not reaching a line yet");
        Assert.assertEquals(inputBuf2.readableBytes(), 0, "should exhaust readable bytes");
        final String result = resultBuf2.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result, literalResponse2, "decode() result mismatched.");
    }

    /**
     * Tests input buffer has "}\r\n" where there is no left curly brace, we treat it not a literal, just return as it is.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testNoLeftCurly() throws Exception {
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final String literalResponse1 = "* LIST (\\Noselect) \"/\" foo}\r\n";
        final byte[] responseBytes1 = literalResponse1.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf = Unpooled.copiedBuffer(responseBytes1, 0, responseBytes1.length);

        final ByteBuf resultBuf1 = (ByteBuf) respReader.decode(null, inputBuf);
        Assert.assertNotNull(resultBuf1, "Should return null because not reaching a line yet");
        Assert.assertEquals(inputBuf.readableBytes(), 0, "should exhaust readable bytes");
        final String result = resultBuf1.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result, literalResponse1, "decode() result mismatched.");
    }

    /**
     * Tests input buffer has "{abc}\r\n" where curly braces do not have digits inside, we treat it not a literal, just return as it is.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testNotDigitsInCurly() throws Exception {
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final String literalResponse1 = "* LIST (\\\\Noselect) \\\"/\\\" {abc}\r\n";
        final byte[] responseBytes1 = literalResponse1.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf = Unpooled.copiedBuffer(responseBytes1, 0, responseBytes1.length);

        final ByteBuf resultBuf1 = (ByteBuf) respReader.decode(null, inputBuf);
        Assert.assertNotNull(resultBuf1, "Should return null because not reaching a line yet");
        Assert.assertEquals(inputBuf.readableBytes(), 0, "should exhaust readable bytes");
        final String result = resultBuf1.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result, literalResponse1, "decode() result mismatched.");
    }

    /**
     * Tests input buffer that has only CR or LF. Expects it is not returning the response.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testDataCrDataLF() throws Exception {
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final String literalResponse1 = "hello\rWorld\n";
        final byte[] responseBytes1 = literalResponse1.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf1 = Unpooled.copiedBuffer(responseBytes1, 0, responseBytes1.length);

        final ByteBuf resultBuf1 = (ByteBuf) respReader.decode(null, inputBuf1);
        Assert.assertEquals(inputBuf1.readerIndex(), 12, "ReaderIndex should not be moved.");
        Assert.assertNotNull(resultBuf1, "Should return null because not seeing CRLF yet");
        final String result2 = resultBuf1.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result2, literalResponse1, "response doesn't match");
    }

    /**
     * Tests input buffer that has the pattern of dataCRdataLFdata. Expects it is not returning the response.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testDataCrDataLFData() throws Exception {
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final String literalResponse1 = "Hello\rWorld\nTomorrow";
        final byte[] responseBytes1 = literalResponse1.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf1 = Unpooled.copiedBuffer(responseBytes1, 0, responseBytes1.length);

        final ByteBuf resultBuf1 = (ByteBuf) respReader.decode(null, inputBuf1);
        Assert.assertEquals(inputBuf1.readerIndex(), 12, "ReaderIndex should not be moved.");
        Assert.assertNotNull(resultBuf1, "Should return null because not seeing CRLF yet");

        final String result2 = resultBuf1.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result2, "Hello\rWorld\n", "response doesn't match");

        final ByteBuf resultBuf2 = (ByteBuf) respReader.decode(null, inputBuf1);
        Assert.assertEquals(inputBuf1.readerIndex(), 12, "ReaderIndex should not be moved.");
        Assert.assertNull(resultBuf2, "Should return null because not seeing CRLF yet");
    }

    /**
     * Tests 1st buffer ends with CR without LF, the reader index should not change. 2nd time LF comes, it should return complete result.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testLFComesInNext() throws Exception {
        final String literalResponse1 = "Hello\r";
        final byte[] responseBytes1 = literalResponse1.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf1 = Unpooled.copiedBuffer(responseBytes1, 0, responseBytes1.length);

        final String literalResponse2 = literalResponse1 + "\n";
        final byte[] responseBytes2 = literalResponse2.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf2 = Unpooled.copiedBuffer(responseBytes2, 0, responseBytes2.length);

        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final ByteBuf resultBuf1 = (ByteBuf) respReader.decode(null, inputBuf1);
        Assert.assertEquals(inputBuf1.readerIndex(), 0, "ReaderIndex should not be moved.");
        Assert.assertNull(resultBuf1, "Should return null because not seeing CRLF yet");

        final ByteBuf resultBuf2 = (ByteBuf) respReader.decode(null, inputBuf2);
        Assert.assertEquals(inputBuf2.readableBytes(), 0, "readableBytes should be exhausted.");
        Assert.assertNotNull(resultBuf2, "Should have data returned");
        final String result = resultBuf2.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result, literalResponse2, "decode() result mismatched.");
    }

    /**
     * Tests 3 buffers comes, 1st: ends with CR. 2nd: ends with LF. 3rd: ends with CRLF. Expects only the 3rd one will get full result.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testBuffer1DataCrBuffer2DataLFBuffer3DataCRLF() throws Exception {
        final String literalResponse1 = "Hello\r";
        final byte[] responseBytes1 = literalResponse1.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf1 = Unpooled.copiedBuffer(responseBytes1, 0, responseBytes1.length);

        final String literalResponse2 = literalResponse1 + "World\n";
        final byte[] responseBytes2 = literalResponse2.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf2 = Unpooled.copiedBuffer(responseBytes2, 0, responseBytes2.length);

        final String literalResponse3 = literalResponse2 + ", and here u go with CRLF!\r\n";
        final byte[] responseBytes3 = literalResponse3.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf inputBuf3 = Unpooled.copiedBuffer(responseBytes3, 0, responseBytes3.length);

        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final ByteBuf resultBuf1 = (ByteBuf) respReader.decode(null, inputBuf1);
        Assert.assertEquals(inputBuf1.readerIndex(), 0, "ReaderIndex should not be moved.");
        Assert.assertNull(resultBuf1, "Should return null because not seeing CRLF yet");

        final ByteBuf resultBuf2 = (ByteBuf) respReader.decode(null, inputBuf2);
        Assert.assertEquals(inputBuf2.readerIndex(), 12, "ReaderIndex should not be moved.");
        Assert.assertNotNull(resultBuf2, "Should return null because not seeing CRLF yet");

        final ByteBuf resultBuf3 = (ByteBuf) respReader.decode(null, inputBuf3);
        Assert.assertEquals(inputBuf3.readableBytes(), 28, "readableBytes should be exhausted.");
        Assert.assertNotNull(resultBuf3, "Should have data returned");
        final String result = resultBuf3.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result, literalResponse2, "decode() result mismatched.");

        final ByteBuf resultBuf4 = (ByteBuf) respReader.decode(null, inputBuf3);
        Assert.assertEquals(inputBuf3.readableBytes(), 0, "readableBytes should be exhausted.");
        Assert.assertNotNull(resultBuf4, "Should have data returned");
        final String result2 = resultBuf4.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result2, ", and here u go with CRLF!\r\n", "decode() result mismatched.");

    }

    /**
     * Feeds the input to a reader in one buffer and collects the frames it produces.
     *
     * @param input the bytes the server sends, as US-ASCII
     * @return the frames, in order
     */
    private List<String> decodeFrames(final String input) {
        final EmbeddedChannel channel = new EmbeddedChannel(new ImapClientRespReader(Integer.MAX_VALUE));
        channel.writeInbound(Unpooled.copiedBuffer(input, StandardCharsets.US_ASCII));
        final List<String> frames = new ArrayList<String>();
        ByteBuf frame = channel.readInbound();
        while (frame != null) {
            frames.add(frame.toString(StandardCharsets.US_ASCII));
            frame.release();
            frame = channel.readInbound();
        }
        channel.finishAndReleaseAll();
        return frames;
    }

    /**
     * Tests that a folder name ending in {n} in the text of an untagged NO response does not open a literal, so the responses after it stay
     * separate.
     */
    @Test
    public void testStatusTextEndingInBracedCountIsNotLiteral() {
        final List<String> expected = Arrays.asList("* LIST () \"/\" \"Lists/Team{36}\"\r\n",
                "* NO Mailbox doesn't exist: Lists/Team{36}\r\n", "* LIST () \"/\" \"Lists/Other\"\r\n",
                "* STATUS \"Lists/Other\" (MESSAGES 3 UNSEEN 1)\r\n", "a3 OK List completed.\r\n");
        Assert.assertEquals(decodeFrames(String.join("", expected)), expected, "Each response should be its own frame.");
    }

    /**
     * Tests that tagged, lower-case and continuation responses whose text ends in a braced count, even after a space, do not open a literal.
     */
    @Test
    public void testStatusAndContinuationTextIsNotLiteral() {
        final List<String> expected = Arrays.asList("a1 OK done {5}\r\n", "* no such folder {5}\r\n", "* BYE {5}\r\n",
                "+ go ahead {5}\r\n", "* OK [ALERT] see {5}\r\n", "a2 OK\r\n");
        Assert.assertEquals(decodeFrames(String.join("", expected)), expected, "Each response should be its own frame.");
    }

    /**
     * Tests that a capability atom ending in {n} does not open a literal, so the tagged completion is framed instead of awaited as literal data.
     */
    @Test
    public void testBraceAfterAtomCharIsNotLiteral() {
        final List<String> expected = Arrays.asList("* CAPABILITY IMAP4rev1 IDLE X-VENDOR{28}\r\n", "a1 OK CAPABILITY completed\r\n");
        Assert.assertEquals(decodeFrames(String.join("", expected)), expected, "Each response should be its own frame.");
    }

    /**
     * Tests braced counts that are not literals: at the start of a line, with no digits, a "~" with nothing before it, after a "~" that follows an
     * atom char, and a count beyond int range.
     */
    @Test
    public void testMalformedBracedCountIsNotLiteral() {
        final List<String> expected = Arrays.asList("{3}\r\n", "* LIST () \"/\" {}\r\n", "~{3}\r\n", "* 1 FETCH (BINARY[1] x~{3}\r\n",
                "* 1 FETCH (BODY[] {99999999999}\r\n", "a1 OK\r\n");
        Assert.assertEquals(decodeFrames(String.join("", expected)), expected, "Each line should be its own frame.");
    }

    /**
     * Tests that literals in the positions the grammar allows still frame with their data: after "(", a literal8 after SP, a literal in an
     * untagged response whose name only starts like a status word, and a literal inside a BADCHARSET response code that continues past it.
     */
    @Test
    public void testLiteralsWhereGrammarAllowsThem() {
        final List<String> expected = Arrays.asList("* 1 FETCH (BODY[HEADER.FIELDS ({7}\r\nSUBJECT)] \"x\")\r\n",
                "* 2 FETCH (BINARY[1] ~{3}\r\nabc)\r\n", "* OKAY {2}\r\nab\r\n",
                "* NO [BADCHARSET (\"a]\\\"b\" {5}\r\nUTF-8 {3}\r\nabc)] unsupported {3}\r\n", "a1 OK\r\n");
        Assert.assertEquals(decodeFrames(String.join("", expected)), expected, "Each response should be one frame with its literals.");
    }
}
