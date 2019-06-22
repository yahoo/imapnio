package com.yahoo.imapnio.client;

import java.nio.charset.StandardCharsets;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.imapnio.client.ImapClientRespReader;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit test for {@link ImapClientRespReader}.
 *
 * @author kaituo
 *
 */
public class ImapClientRespReaderIT {

    /**
     * Tests parsing single line response.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testDecodeLineResponse() throws Exception {
        final String response = "A1 OK LOGIN completed\r\n";
        final byte[] responseBytes = response.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf buffer = Unpooled.copiedBuffer(responseBytes, 0, responseBytes.length);
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final ByteBuf resultBuf = (ByteBuf) respReader.decode(null, buffer);
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
        final ByteBuf buffer = Unpooled.copiedBuffer(responseBytes, 0, responseBytes.length);
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final ByteBuf resultBuf = (ByteBuf) respReader.decode(null, buffer);
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
        final ByteBuf buffer = Unpooled.copiedBuffer(responseBytes, 0, responseBytes.length);
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final ByteBuf resultBuf = (ByteBuf) respReader.decode(null, buffer);
        final String result = resultBuf.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result, literalResponse);
    }

    /**
     * Tests parsing multiple literal responses, but the second literal '{5}' is still in first literal response count.
     *
     * @throws Exception not for this test
     */
    @Test
    public void testDecodeLiteralResponseWhenSecondLiteralStillInFirstLiteralCount() throws Exception {
        final String literalResponse = "* 1 FETCH BODY {20}\r\n" + "abc\r\n" + " BODY[HEADER]{5}\r\n" + "\r\n";
        final byte[] responseBytes = literalResponse.getBytes(StandardCharsets.US_ASCII);
        final ByteBuf buffer = Unpooled.copiedBuffer(responseBytes, 0, responseBytes.length);
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final ByteBuf resultBuf = (ByteBuf) respReader.decode(null, buffer);
        final String result = resultBuf.toString(StandardCharsets.US_ASCII);
        Assert.assertEquals(result, literalResponse);
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
        final ByteBuf buffer = Unpooled.copiedBuffer(responseBytes, 0, responseBytes.length);
        final ImapClientRespReader respReader = new ImapClientRespReader(Integer.MAX_VALUE);
        final ByteBuf resultBuf = (ByteBuf) respReader.decode(null, buffer);
        Assert.assertNull(resultBuf, "Should return null");
    }

}
