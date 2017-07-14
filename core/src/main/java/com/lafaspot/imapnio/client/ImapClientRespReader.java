package com.lafaspot.imapnio.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

import com.sun.mail.util.ASCIIUtility;

/**
 * Basic response reader, read response from channel and decode based on line delimiter, also could handle imap literal response.
 *
 * @author kaituo
 *
 */
public class ImapClientRespReader extends DelimiterBasedFrameDecoder {

    /** Literal response remaining bytes size. */
    private int literalCount;
    /** Literal response buffer. */
    private ByteBuf literalBuffer;
    /** Minimal literal response  length. */
    private static final int MINIMAL_LITERAL_LENGTH = 5;
    /** Literal response '}' index from tail. */
    private static final int LITERAL_INDEX_FROM_TAIL = 3;
    /** Imap response line delimiter, carriage return - new line. */
    private static final ByteBuf[] DELIMITER = new ByteBuf[] { Unpooled.wrappedBuffer(new byte[] { '\r', '\n' }) };

    /**
     * Constructor of imap client response reader.
     *
     * @param maxLineLength maximum response line length
     */
    public ImapClientRespReader(final int maxLineLength) {
        super(maxLineLength, false, DELIMITER);
    }

    /**
     * Decode has 2 modes:
     *
     * (a) line mode : The client will return the decoded line if it ends with CRLF and no literal preceding CRLF.
     *
     * (b) literal mode : When the client receiving a literal response("{" number "}"), it will keep buffering the bytes until all literal data read
     * and then continue read the next CRLF.
     */
    @Override
    protected Object decode(final ChannelHandlerContext ctx, final ByteBuf buffer) throws Exception {
        final int bufferBytes = buffer.readableBytes();
        if (bufferBytes > 0) {
            ByteBuf lineBuffer = null;
            do {
                lineBuffer = (ByteBuf) super.decode(ctx, buffer);
                // lineBuffer might be null(no delimiter in the buffer) when receiving partial response,
                // skip current decoding and wait until next CRLF.
                if (lineBuffer != null) {
                    final int readableBytes = lineBuffer.readableBytes();

                    if (literalCount > 0) {
                        // Continue buffering literal response.
                        literalCount = literalCount - readableBytes;
                        if (literalCount <= 0) {
                            // Already read all literal response, check if current line contains new literal response.
                            literalCount = getLiteralCount(lineBuffer, readableBytes, readableBytes + literalCount);
                        }
                        literalBuffer.writeBytes(lineBuffer);
                    } else {
                        literalCount = getLiteralCount(lineBuffer, readableBytes, 0);
                        if (literalCount > 0) {
                            // Init literal buffer when receiving first literal response.
                            literalBuffer = Unpooled.buffer(readableBytes + literalCount);
                            literalBuffer.writeBytes(lineBuffer);
                        }
                    }
                }
            } while (literalCount > 0 && buffer.readableBytes() > 0 && lineBuffer != null);

            if (literalBuffer != null && literalCount == 0) {
                // Return literal mode result.
                final ByteBuf lastLineBuffer = (ByteBuf) super.decode(ctx, buffer);
                final int lastLineIndex = lastLineBuffer.readableBytes();
                final byte[] lastLineBytes = new byte[lastLineIndex];
                lastLineBuffer.getBytes(lastLineBuffer.readerIndex(), lastLineBytes);
                literalBuffer.writeBytes(lastLineBytes);
                final ByteBuf result = literalBuffer.copy();
                literalBuffer = null;
                return result;
            } else {
                // Return line mode result.
                return lineBuffer;
            }
        } else {
            return null;
        }
    }

    /**
     * Calculate literal response size, return 0 if it is non literal response.
     *
     * @param lineBuffer line delimiter(CRLF) decoded response
     * @param readableBytes lineBuffer readable bytes
     * @param startIndex the available start index for calculating literal size
     * @return literal response size
     */
    private int getLiteralCount(final ByteBuf lineBuffer, final int readableBytes, final int startIndex) {
        int literalCount = 0;
        final byte[] response = new byte[readableBytes];
        lineBuffer.getBytes(lineBuffer.readerIndex(), response);
        if (readableBytes > MINIMAL_LITERAL_LENGTH && response[readableBytes - LITERAL_INDEX_FROM_TAIL] == '}') {
            int i;
            for (i = readableBytes - LITERAL_INDEX_FROM_TAIL - 1; i >= startIndex; i--) {
                if (response[i] == '{') {
                    break;
                }
            }
            if (i >= startIndex) {
                literalCount = ASCIIUtility.parseInt(response, i + 1, readableBytes - LITERAL_INDEX_FROM_TAIL);
            }
        }
        return literalCount;
    }

}
