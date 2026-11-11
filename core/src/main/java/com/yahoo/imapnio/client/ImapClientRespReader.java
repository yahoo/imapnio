package com.yahoo.imapnio.client;

import javax.annotation.Nonnull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;

/**
 * Basic response reader, read response from channel and decode based on line, also could handle IMAP literal response.
 *
 * <p>
 * A line ending in {@code {n}} opens a literal only where the response grammar allows a string (RFC 3501 section 4.3, RFC 9051 section 4.3):
 * the brace follows SP or "(", optionally with the "~" of a literal8 (RFC 3516), and the digits form a valid count. In a status response or
 * continuation request (RFC 3501 section 7.1) a literal can appear only inside the bracketed response code, never in the free-form text that
 * follows it. Server text such as a folder name echoed in a NO response can end in {@code {n}}; reading it as a literal would merge the
 * following responses into one, or wait for bytes that never arrive.
 * </p>
 *
 * @author kaituo
 *
 */
public class ImapClientRespReader extends LineBasedFrameDecoder {

    /**
     * Where the current response stands with respect to resp-text, which decides whether a trailing {@code {n}} can open a literal.
     */
    private enum RespTextState {
        /** Not a status response or continuation request: a literal may appear wherever the grammar allows a string. */
        NOT_STATUS,
        /** Inside the bracketed response code of a status response, where a code such as BADCHARSET can carry an astring. */
        IN_CODE,
        /** In the human-readable text of a status response, which cannot carry a literal. */
        IN_TEXT
    }

    /** Status words whose remainder is resp-text, in upper case. */
    private static final String[] STATUS_WORDS = { "OK", "NO", "BAD", "BYE", "PREAUTH" };

    /** Number base for the literal count. */
    private static final int RADIX = 10;

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

    /** Whether the response being framed is in resp-text; set from its first line and updated by each line after a literal. */
    private RespTextState respTextState;

    /**
     * Constructor of IMAP client response reader.
     *
     * @param maxLineLength maximum response line length
     */
    public ImapClientRespReader(final int maxLineLength) {
        super(maxLineLength, false, true);
        literalCount = -1;
    }

    /**
     * Decode has 2 modes:
     *
     * (a) line mode : The client will return the decoded line if it ends with CRLF and no literal preceding CRLF.
     *
     * (b) literal mode : When the client receiving a literal response({digits}) at a position where the grammar allows one, it will keep buffering
     * the bytes until all literal data read and then continue read the next CRLF.
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
                final int lineLen = lineBuf.readableBytes();
                if (literalBuf == null) {
                    respTextState = getFirstLineState(lineBuf, lineLen);
                } else if (respTextState == RespTextState.IN_CODE) {
                    respTextState = scanRespCode(lineBuf, 0, lineLen);
                }

                this.literalCount = (respTextState == RespTextState.IN_TEXT) ? -1 : getLiteralCount(lineBuf, lineLen);
                if (this.literalCount < 0) { // not a literal, so the response is complete
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
     * Classifies the first line of a response: a status response (RFC 3501 section 7.1, tagged or untagged) or a continuation request continues
     * with resp-text, where a literal can appear only inside the bracketed response code.
     *
     * @param lineBuf buffer for the first line of a response
     * @param lineLen line length of the above lineBuf
     * @return the state of the response at the end of this line
     */
    private RespTextState getFirstLineState(@Nonnull final ByteBuf lineBuf, final int lineLen) {
        final int tagEnd = lineBuf.indexOf(0, lineLen, (byte) ' ');
        if (tagEnd < 0) {
            return RespTextState.NOT_STATUS;
        }
        final int textStart;
        if (tagEnd == 1 && lineBuf.getByte(0) == '+') {
            textStart = tagEnd + 1;
        } else {
            final int wordEnd = getStatusWordEnd(lineBuf, tagEnd + 1, lineLen);
            if (wordEnd < 0) {
                return RespTextState.NOT_STATUS;
            }
            textStart = wordEnd + 1;
        }
        if (textStart < lineLen && lineBuf.getByte(textStart) == '[') {
            return scanRespCode(lineBuf, textStart + 1, lineLen);
        }
        return RespTextState.IN_TEXT;
    }

    /**
     * Matches a status word, case-insensitively, at the given index.
     *
     * @param lineBuf buffer for the line ended with CRLF
     * @param from index where the status word would start
     * @param lineLen line length of the above lineBuf
     * @return index of the byte after the status word, or -1 if no status word followed by SP or the line end starts there
     */
    private int getStatusWordEnd(@Nonnull final ByteBuf lineBuf, final int from, final int lineLen) {
        for (final String word : STATUS_WORDS) {
            final int end = from + word.length();
            if (end < lineLen && matchesIgnoreCase(lineBuf, from, word) && isWordEnd(lineBuf.getByte(end))) {
                return end;
            }
        }
        return -1;
    }

    /**
     * @param lineBuf buffer for the line
     * @param from index to compare from
     * @param word the upper-case ASCII word to compare against
     * @return true if the bytes at the index equal the word, ignoring ASCII case
     */
    private boolean matchesIgnoreCase(@Nonnull final ByteBuf lineBuf, final int from, @Nonnull final String word) {
        for (int i = 0; i < word.length(); i++) {
            if (Character.toUpperCase((char) lineBuf.getByte(from + i)) != word.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param b the byte after a candidate status word
     * @return true if the byte ends the word: SP or the line terminator
     */
    private boolean isWordEnd(final byte b) {
        return b == ' ' || b == '\r' || b == '\n';
    }

    /**
     * Scans part of a line inside a response code for the closing "]", skipping quoted strings, which may contain one.
     *
     * @param lineBuf buffer for the line ended with CRLF
     * @param from index to scan from
     * @param lineLen line length of the above lineBuf
     * @return {@link RespTextState#IN_TEXT} if the response code closes on this line, otherwise {@link RespTextState#IN_CODE}
     */
    private RespTextState scanRespCode(@Nonnull final ByteBuf lineBuf, final int from, final int lineLen) {
        boolean quoted = false;
        int i = from;
        while (i < lineLen) {
            final byte b = lineBuf.getByte(i);
            if (quoted) {
                if (b == '\\') {
                    i++;
                } else if (b == '"') {
                    quoted = false;
                }
            } else if (b == '"') {
                quoted = true;
            } else if (b == ']') {
                return RespTextState.IN_TEXT;
            }
            i++;
        }
        return RespTextState.IN_CODE;
    }

    /**
     * Extracts the count of a literal ending the line: {@code SP {digits}CRLF} or {@code ({digits}CRLF}, with an optional "~" before the brace.
     *
     * @param lineBuf buffer for the line ended with CRLF
     * @param lineLen line length of the above lineBuf
     * @return literal count between curly braces; return -1 if the line does not end with a literal
     */
    private int getLiteralCount(@Nonnull final ByteBuf lineBuf, final int lineLen) {
        if (lineLen < FIVE || lineBuf.getByte(lineLen - THREE) != '}') {
            return -1;
        }
        final int digitsEnd = lineLen - THREE;
        int open = digitsEnd - 1;
        while (open >= 0 && isDigit(lineBuf.getByte(open))) {
            open--;
        }
        if (open < 1 || open == digitsEnd - 1 || lineBuf.getByte(open) != '{') {
            return -1;
        }
        final int before = (lineBuf.getByte(open - 1) == '~') ? open - 2 : open - 1;
        if (before < 0 || (lineBuf.getByte(before) != ' ' && lineBuf.getByte(before) != '(')) {
            return -1;
        }
        long count = 0;
        for (int i = open + 1; i < digitsEnd; i++) {
            count = count * RADIX + (lineBuf.getByte(i) - '0');
            if (count > Integer.MAX_VALUE) {
                return -1;
            }
        }
        return (int) count;
    }

    /**
     * @param b the byte to check
     * @return true if the byte is an ASCII digit
     */
    private boolean isDigit(final byte b) {
        return b >= '0' && b <= '9';
    }
}
