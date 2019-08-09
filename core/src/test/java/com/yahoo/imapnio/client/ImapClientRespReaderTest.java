package com.yahoo.imapnio.client;

import java.nio.charset.StandardCharsets;

import org.testng.Assert;
import org.testng.annotations.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

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
        Assert.assertEquals(result, "NO CRLF YET, and here u go with CRLF!\r\n", "decode() result mismatched.");
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
        Assert.assertEquals(inputBuf1.readerIndex(), 0, "ReaderIndex should not be moved.");
        Assert.assertNull(resultBuf1, "Should return null because not seeing CRLF yet");
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
        Assert.assertEquals(inputBuf1.readerIndex(), 0, "ReaderIndex should not be moved.");
        Assert.assertNull(resultBuf1, "Should return null because not seeing CRLF yet");
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
        Assert.assertEquals(inputBuf2.readerIndex(), 0, "ReaderIndex should not be moved.");
        Assert.assertNull(resultBuf2, "Should return null because not seeing CRLF yet");

        final ByteBuf resultBuf3 = (ByteBuf) respReader.decode(null, inputBuf3);
        Assert.assertEquals(inputBuf3.readableBytes(), 0, "readableBytes should be exhausted.");
        Assert.assertNotNull(resultBuf3, "Should have data returned");
        final String result = resultBuf3.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result, literalResponse3, "decode() result mismatched.");
    }
}
