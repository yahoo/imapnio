package com.yahoo.imapnio.client;

import javax.annotation.Nonnull;

import com.sun.mail.util.ASCIIUtility;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;

/**
 * Basic response reader, read response from channel and decode based on line delimiter, also could handle IMAP literal response.
 *
 * @author kaituo
 *
 */
public class ImapClientRespReader extends DelimiterBasedFrameDecoder {

    /** Constant for 4. */
    private static final int FOUR = 4;

    /** Constant for 3. */
    private static final int THREE = 3;

    /** Constant for 5. */
    private static final int FIVE = 5;

    /** Extra length to accommodate CRLF. */
    private static final int EXTRA_PADDING_LEN = 16;

    /** Literal response remaining bytes size. */
    private int literalCount;

    /** Literal response buffer. */
    private ByteBuf literalBuf;

    /** IMAP response line delimiter, carriage return - new line. */
    private static final ByteBuf[] DELIMITER = new ByteBuf[] { Unpooled.wrappedBuffer(new byte[] { '\r', '\n' }) };

    /**
     * Constructor of IMAP client response reader.
     *
     * @param maxLineLength maximum response line length
     */
    public ImapClientRespReader(final int maxLineLength) {
        super(maxLineLength, false, DELIMITER);
        literalCount = -1;
    }

    /**
     * Decode has 2 modes:
     *
     * (a) line mode : The client will return the decoded line if it ends with CRLF and no literal preceding CRLF.
     *
     * (b) literal mode : When the client receiving a literal response({digits}), it will keep buffering the bytes until all literal data read and
     * then continue read the next CRLF.
     *
     * @param ctx the {@link ChannelHandlerContext} which this decoder belongs to
     * @param inputBuf the {@link ByteBuf} from which to read data
     * @return the {@link ByteBuf} which represent the frame or {@code null} if no frame could be created.
     * @throws Exception on decode failure
     */
    @Override
    protected Object decode(final ChannelHandlerContext ctx, final ByteBuf inputBuf) throws Exception {

        while (inputBuf.readableBytes() > 0) {

            if (literalCount <= 0) { // LINE mode - read until CRLF

                final ByteBuf lineBuf = (ByteBuf) super.decode(ctx, inputBuf); // Read a CRLF terminated line from inputBuf

                if (lineBuf == null) { // no CRLF seen in this case, don't return existing buffer since it is not done
                    return null;
                }
                // Now lets check for literals : {<digits>}CRLF
                // Note: index needs to >= 5 for the above sequence to occur
                final int lineLen = lineBuf.readableBytes();
                final boolean noLiterals = (lineLen < FIVE || lineBuf.getByte(lineLen - THREE) != '}'); // getByte() is peek operation

                if (noLiterals) { // enough for a complete IMAP response, return the data
                    return getFinalResponse(lineBuf);
                }

                // extracting literal count between curly braces
                this.literalCount = getLiteralCount(lineBuf, lineLen);
                if (this.literalCount < 0) { // Nope, not a literal ?
                    return getFinalResponse(lineBuf);
                }

                // literals follows if reaching here
                if (literalBuf == null) {
                    literalBuf = Unpooled.buffer(lineLen + literalCount + EXTRA_PADDING_LEN);
                }
                writeLiteralBufFromLineBuf(lineBuf); // add current line (ex: "* 1 FETCH (FLAGS (\Seen $NotJunk) BODY[] {4495}\r\n")
                // back to top of loop to enter literal mode block

            } else { // LITERAL mode - read till reaching the count or end of inputBuf
                final int avail = inputBuf.readableBytes(); // available bytes unread in inputBuf
                final int actual = (literalCount <= avail) ? literalCount : avail; // actual length to copy
                literalBuf.writeBytes(inputBuf, actual);
                literalCount -= actual;
            }
        }

        return null;
    }

    /**
     * Writes the given lineBuf to literalBuf. When writing is finished, we need to release the given lineBuf since this lineBuf will not be given to
     * StringDecoder to release.
     *
     * @param lineBuf the line buffer obtained from the super.decode
     */
    private void writeLiteralBufFromLineBuf(@Nonnull final ByteBuf lineBuf) {
        literalBuf.writeBytes(lineBuf);
        ReferenceCountUtil.release(lineBuf); // Decreases the reference count by {@link 1}
    }

    /**
     * Prepares final response and clear the holding buffer.
     *
     * @param lineBuf the current line obtained from input buffer
     * @return final response in ByteBuf
     */
    private ByteBuf getFinalResponse(@Nonnull final ByteBuf lineBuf) {
        if (literalBuf == null) { // literalBuf is empty
            return lineBuf;
        }
        writeLiteralBufFromLineBuf(lineBuf);
        final ByteBuf result = literalBuf;
        // reset existing
        literalBuf = null;
        literalCount = -1;
        return result;
    }

    /**
     * @param lineBuf buffer for the line ended with CRLF
     * @param lineLen line length of the above lineBuf
     * @return literal in int data type between curly braces; return -1 if we cannot extract literal
     */
    private int getLiteralCount(@Nonnull final ByteBuf lineBuf, @Nonnull final int lineLen) {
        int i;
        // look for left curly
        for (i = lineLen - FOUR; i >= 0; i--) {
            if (lineBuf.getByte(i) == '{') {
                break;
            }
        }
        if (i < 0) { // Nope, not a literal ?
            return -1;
        }
        // OK, extract the count ..
        try {
            final int numDigits = lineLen - THREE - (i + 1);
            final byte[] digits = new byte[numDigits];
            lineBuf.getBytes(i + 1, digits, 0, numDigits); // only copying the digits part
            return ASCIIUtility.parseInt(digits, 0, digits.length);
        } catch (final NumberFormatException e) {
            return -1;
        }
    }

}
