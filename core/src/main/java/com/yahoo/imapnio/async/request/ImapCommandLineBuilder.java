package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class builds a command line, splitting it wherever a synchronizing literal forces the client to wait for the server.
 *
 * <p>
 * An {@code astring} carrying CR or LF cannot be written as a quoted string, since {@code quoted} is built from {@code TEXT-CHAR}, "any CHAR
 * except CR and LF". RFC 3501 section 4.3 provides the {@code literal} form for exactly this case: an octet count in braces, a CRLF, then the
 * octets themselves, which may include CR and LF. This builder emits that form, so an argument the protocol can carry is carried rather than
 * refused.
 * </p>
 *
 * <p>
 * A synchronizing literal obliges the client to stop after the octet count and wait for the server's command continuation request before
 * sending the octets and the rest of the line. The line is therefore produced as a list of segments, split at each such pause; the caller
 * sends the first and releases the next one on every continuation. When the server has advertised LITERAL+ or LITERAL- the count is written
 * with a "+" instead, the pause is not required, and the whole line stays in a single segment.
 * </p>
 *
 * <p>
 * NUL is refused even here. A literal is {@code *CHAR8} and {@code CHAR8} is "%x01-ff, any OCTET except NUL", so no form of an astring can
 * carry it.
 * </p>
 */
final class ImapCommandLineBuilder {

    /** Largest literal a non-synchronizing form may carry, from RFC 7888 section 4. */
    private static final int MAX_NON_SYNC_LITERAL_LEN = 4096;

    /** Ascii code 127, chars after that are symbols. */
    private static final int ASCII_CODE_127 = 0177;

    /** Low bits mask. */
    private static final int MASK = 0xff;

    /** Primitive int 3. */
    private static final int THREE = 3;

    /** The DEL control character, which atom-specials excludes from an atom but a quoted string may carry. */
    private static final char DEL_CHAR = 0x7F;

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** The segments completed so far, each one ending where the client must wait for a continuation. */
    private final List<ByteBuf> segments = new ArrayList<>();

    /** Which literal form the server has been found to accept. */
    private final LiteralSupport literalOpt;

    /** The segment being built. */
    private ByteBuf cur = Unpooled.buffer();

    /**
     * Initializes a {@link ImapCommandLineBuilder}.
     *
     * @param literalOpt the literal form the server accepts
     */
    ImapCommandLineBuilder(@Nonnull final LiteralSupport literalOpt) {
        this.literalOpt = literalOpt;
    }

    /**
     * Writes bytes that are part of the command itself, not a caller-supplied argument.
     *
     * @param b the bytes to write
     * @return this builder
     */
    ImapCommandLineBuilder raw(@Nonnull final byte[] b) {
        cur.writeBytes(b);
        return this;
    }

    /**
     * Writes a single byte that is part of the command itself, not a caller-supplied argument.
     *
     * @param b the byte to write
     * @return this builder
     */
    ImapCommandLineBuilder raw(final byte b) {
        cur.writeByte(b);
        return this;
    }

    /**
     * Writes a caller-supplied {@code astring}, as a quoted string or an atom when it can be one, and as a literal when it cannot.
     *
     * @param src the value to write
     * @param doQuote whether to quote the value when it is written in the non-literal form
     * @param name the name of the argument, used in the failure detail
     * @return this builder
     * @throws ImapAsyncClientException when the value carries NUL, or a character above ascii 127
     */
    ImapCommandLineBuilder astring(@Nonnull final String src, final boolean doQuote, @Nonnull final String name)
            throws ImapAsyncClientException {
        if (src.indexOf('\0') >= 0) {
            // a literal is *CHAR8 and CHAR8 excludes NUL, so no astring form can carry it
            throw new ImapAsyncClientException(FailureType.INVALID_INPUT,
                    new StringBuilder(name).append(" argument contains NUL, which no astring form can represent.").toString());
        }

        if (src.indexOf('\r') < 0 && src.indexOf('\n') < 0) {
            formatInline(src, doQuote); // no literal needed, so the atom or quoted form is written
            return this;
        }

        for (int i = 0; i < src.length(); i++) {
            if ((src.charAt(i) & 0xff) > ASCII_CODE_127) {
                // the octets are written as ascii below, so a wider character would be silently replaced
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT,
                        new StringBuilder(name).append(" argument contains a character above ascii 127 at index ").append(i).append('.')
                                .toString());
            }
        }

        final byte[] octets = src.getBytes(StandardCharsets.US_ASCII);
        // RFC 7888: the non-synchronizing form is marked with "+" for LITERAL- exactly as for LITERAL+, the two differ only in the size cap
        final boolean isNonSync = literalOpt == LiteralSupport.ENABLE_LITERAL_PLUS
                || (literalOpt == LiteralSupport.ENABLE_LITERAL_MINUS && octets.length <= MAX_NON_SYNC_LITERAL_LEN);

        cur.writeByte(ImapClientConstants.L_BRACE);
        cur.writeBytes(Integer.toString(octets.length).getBytes(StandardCharsets.US_ASCII));
        if (isNonSync) {
            cur.writeByte(ImapClientConstants.PLUS);
        }
        cur.writeByte(ImapClientConstants.R_BRACE);
        cur.writeBytes(CRLF_B);

        if (isNonSync) {
            cur.writeBytes(octets); // the server will not send a continuation request, so the line carries on
        } else {
            segments.add(cur); // the client must stop here until the server asks for the rest
            cur = Unpooled.buffer();
            cur.writeBytes(octets);
        }
        return this;
    }

    /**
     * Writes a value as an atom or a quoted string, the two forms that sit inline in the command line.
     *
     * <p>
     * Neither form can hold CR or LF, since {@code quoted} is built from {@code TEXT-CHAR}, "any CHAR except CR and LF". There is no guard
     * for that here, because {@link #astring} is the only caller and it sends such a value down the literal path instead. Quoting alone would
     * not be enough to make one safe: a quoted string containing a raw CRLF still ends the command line, so only the octet count of a literal
     * can carry one.
     * </p>
     *
     * @param src the value to write, free of CR and LF
     * @param doQuote whether to quote the value
     * @throws ImapAsyncClientException when the value carries a character above ascii 127
     */
    private void formatInline(@Nonnull final String src, final boolean doQuote) throws ImapAsyncClientException {
        final int len = src.length();

        // if 0 length, send as quoted-string
        boolean quote = len == 0 ? true : doQuote;
        boolean escape = false;

        char b;
        for (int i = 0; i < len; i++) {
            b = src.charAt(i);
            if ((b & MASK) > ASCII_CODE_127) {
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT);
            }
            // DEL is a CTL, so atom-specials keeps it out of an atom, but TEXT-CHAR lets a quoted string carry it
            if (b == '*' || b == '%' || b == '(' || b == ')' || b == '{' || b == '"' || b == '\\' || ((b & MASK) <= ' ')
                    || b == DEL_CHAR) {
                quote = true;
                if (b == '"' || b == '\\') {
                    escape = true;
                }
            }
        }

        /*
         * Make sure the (case-independent) string "NIL" is always quoted, so as not to be confused with a real NIL (handled above in nstring). This
         * is more than is necessary, but it's rare to begin with and this makes it safer than doing the test in nstring above in case some code calls
         * writeString when it should call writeNString.
         */
        if (!quote && len == THREE && (src.charAt(0) == 'N' || src.charAt(0) == 'n') && (src.charAt(1) == 'I' || src.charAt(1) == 'i')
                && (src.charAt(2) == 'L' || src.charAt(2) == 'l')) {
            quote = true;
        }

        if (quote) {
            cur.writeByte('"');
        }

        if (escape) {
            // already quoted
            for (int i = 0; i < len; i++) {
                b = src.charAt(i);
                if (b == '"' || b == '\\') {
                    cur.writeByte('\\');
                }
                cur.writeByte(b);
            }
        } else {
            cur.writeBytes(src.getBytes(StandardCharsets.US_ASCII));
        }

        if (quote) {
            cur.writeByte('"');
        }
    }

    /**
     * Terminates the command line and returns its segments in the order they are to be sent.
     *
     * @return the segments, never empty
     */
    List<ByteBuf> finish() {
        cur.writeBytes(CRLF_B);
        segments.add(cur);
        return segments;
    }

    /**
     * Terminates the command line and returns it whole, for a command none of whose arguments can need a synchronizing literal.
     *
     * <p>
     * Every mailbox name is run through {@link com.sun.mail.imap.protocol.BASE64MailboxEncoder} first, which turns CR and LF into modified
     * UTF-7, and every atom is checked beforehand, so the commands using this method cannot produce a literal. Should that ever stop holding,
     * for instance if the UTF-8 mailbox names of RFC 9051 are adopted and the encoder is dropped, this fails loudly rather than sending a
     * fragment of a command line.
     * </p>
     *
     * @return the whole command line
     * @throws ImapAsyncClientException when an argument did need a synchronizing literal, so the line cannot be sent in one piece
     */
    ByteBuf finishSingle() throws ImapAsyncClientException {
        final List<ByteBuf> all = finish();
        if (all.size() > 1) {
            throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND,
                    "an argument needs a synchronizing literal, which this command cannot send.");
        }
        return all.get(0);
    }

    /**
     * Returns the command line so far without terminating it, for a caller that appends the rest itself.
     *
     * @return the buffer holding the command line so far
     * @throws ImapAsyncClientException when an argument did need a synchronizing literal, so the line cannot be sent in one piece
     */
    ByteBuf openSingle() throws ImapAsyncClientException {
        if (!segments.isEmpty()) {
            throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND,
                    "an argument needs a synchronizing literal, which this command cannot send.");
        }
        return cur;
    }
}
