package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.mail.Flags;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

import io.netty.buffer.ByteBuf;

/**
 * This class encodes/formats imap command arguments properly based on the data value. The input data should be within ASCII chars.
 */
public class ImapArgumentFormatter {

    /** Primitive int 3. */
    private static final int THREE = 3;

    /** Ascii code 127, chars after that are symbols. */
    private static final int ASCII_CODE_127 = 0177;

    /** Low bits mask. */
    private static final int MASK = 0xff;

    /** Literal. */
    private static final String SEEN = "\\Seen";

    /** Literal. */
    private static final String RECENT = "\\Recent";

    /** Literal. */
    private static final String FLAGGED = "\\Flagged";

    /** Literal. */
    private static final String DRAFT = "\\Draft";

    /** Literal. */
    private static final String DELETED = "\\Deleted";

    /** Literal. */
    private static final String ANSWERED = "\\Answered";

    /**
     * Writes out given imap (UTF-7) String. An imap String is defined in RFC 3501, page 16.
     *
     * @param src the source string, assuming it is from ascii code 0000 - 0177 already!
     * @param out the ButeBuf to write to
     * @param doQuote whether to quote or not
     * @throws ImapAsyncClientException when src String that is > 0177
     */
    void formatArgument(@Nonnull final String src, @Nonnull final ByteBuf out, final boolean doQuote) throws ImapAsyncClientException {
        int len = src.length();

        // if 0 length, send as quoted-string
        boolean quote = len == 0 ? true : doQuote;
        boolean escape = false;

        char b;
        for (int i = 0; i < len; i++) {
            b = src.charAt(i);
            if (b == '\0' || b == '\r' || b == '\n') {
                // NUL, CR or LF means the bytes need to be sent as literals
                out.writeBytes(src.getBytes(StandardCharsets.US_ASCII));
                return;
            }
            if ((b & MASK) > ASCII_CODE_127) {
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT);
            }
            if (b == '*' || b == '%' || b == '(' || b == ')' || b == '{' || b == '"' || b == '\\' || ((b & MASK) <= ' ')) {
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
            out.writeByte('"');
        }

        if (escape) {
            // already quoted
            for (int i = 0; i < len; i++) {
                b = src.charAt(i);
                if (b == '"' || b == '\\') {
                    out.writeByte('\\');
                }
                out.writeByte(b);
            }
        } else {
            out.writeBytes(src.getBytes(StandardCharsets.US_ASCII));
        }

        if (quote) {
            out.writeByte('"');
        }
    }

    /**
     * Writes out given imap (UTF-7) String. An imap String is defined in RFC 3501, page 16.
     *
     * @param src the source string, assuming it is UTF-7 already
     * @param out the StringBuilder to append to
     * @param doQuote whether to quote or not
     * @throws ImapAsyncClientException when src String that is > 0177
     */
    void formatArgument(@Nonnull final String src, @Nonnull final StringBuilder out, final boolean doQuote) throws ImapAsyncClientException {
        int len = src.length();

        // if 0 length, send as quoted-string
        boolean quote = len == 0 ? true : doQuote;
        boolean escape = false;

        char b;
        for (int i = 0; i < len; i++) {
            b = src.charAt(i);
            if (b == '\0' || b == '\r' || b == '\n') {
                // NUL, CR or LF means the bytes need to be sent as literals
                out.append(src);
                return;
            }

            if ((b & MASK) > ASCII_CODE_127) {
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT);
            }
            if (b == '*' || b == '%' || b == '(' || b == ')' || b == '{' || b == '"' || b == '\\' || ((b & MASK) <= ' ')) {
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
            out.append('"');
        }

        if (escape) {
            // already quoted
            for (int i = 0; i < len; i++) {
                b = src.charAt(i);
                if (b == '"' || b == '\\') {
                    out.append('\\');
                }
                out.append(b);
            }
        } else {
            out.append(src);
        }

        if (quote) {
            out.append('"');
        }
    }

    /**
     * Creates an IMAP flag_list from the given Flags object.
     *
     * @param flags the flags
     * @return the flag list string
     */
    String buildFlagString(@Nonnull final Flags flags) {
        final StringBuilder sb = new StringBuilder();
        sb.append(ImapClientConstants.L_PAREN); // start of flag_list

        Flags.Flag[] sf = flags.getSystemFlags(); // get the system flags
        boolean first = true;
        for (int i = 0; i < sf.length; i++) {
            String s;
            Flags.Flag f = sf[i];
            if (f == Flags.Flag.ANSWERED) {
                s = ANSWERED;
            } else if (f == Flags.Flag.DELETED) {
                s = DELETED;
            } else if (f == Flags.Flag.DRAFT) {
                s = DRAFT;
            } else if (f == Flags.Flag.FLAGGED) {
                s = FLAGGED;
            } else if (f == Flags.Flag.RECENT) {
                s = RECENT;
            } else if (f == Flags.Flag.SEEN) {
                s = SEEN;
            } else {
                continue; // skip it
            }
            if (first) {
                first = false;
            } else {
                sb.append(ImapClientConstants.SPACE);
            }
            sb.append(s);
        }

        String[] uf = flags.getUserFlags(); // get the user flag strings
        for (int i = 0; i < uf.length; i++) {
            if (first) {
                first = false;
            } else {
                sb.append(ImapClientConstants.SPACE);
            }
            sb.append(uf[i]);
        }

        sb.append(ImapClientConstants.R_PAREN); // terminate flag_list
        return sb.toString();
    }

    /**
     * Creates an IMAP entry-flag-name from the given Flags with only one flag object. The following is the ABNF from RFC7162
     * <pre>
     * entry-flag-name     = DQUOTE "/flags/" attr-flag DQUOTE
     *                        ;; Each system or user-defined flag <flag>
     *                        ;; is mapped to "/flags/<flag>".
     *                        ;;
     *                        ;; <entry-flag-name> follows the escape rules
     *                        ;; used by "quoted" string as described in
     *                        ;; Section 4.3 of [RFC3501]; e.g., for the
     *                        ;; flag \Seen, the corresponding <entry-name>
     *                        ;; is "/flags/\\seen", and for the flag
     *                        ;; $MDNSent, the corresponding <entry-name>
     *                        ;; is "/flags/$mdnsent".
     * </pre>
     *
     * @param flags the flags with one flag
     * @return the entry flag name string
     */
     String buildEntryFlagName(@Nonnull final Flags flags)  {
        final Flags.Flag[] sf = flags.getSystemFlags(); // get the system flags
        final String[] uf = flags.getUserFlags(); // get the user flag strings
        final StringBuilder s = new StringBuilder("\"/flags/"); // start of entry flag name

        if (sf.length == 1) {
            s.append(ImapClientConstants.BACKSLASH);
            if (sf[0] == Flags.Flag.ANSWERED) {
                s.append(ANSWERED);
            } else if (sf[0] == Flags.Flag.DELETED) {
                s.append(DELETED);
            } else if (sf[0] == Flags.Flag.DRAFT) {
                s.append(DRAFT);
            } else if (sf[0] == Flags.Flag.FLAGGED) {
                s.append(FLAGGED);
            } else if (sf[0] == Flags.Flag.RECENT) {
                s.append(RECENT);
            } else if (sf[0] == Flags.Flag.SEEN) {
                s.append(SEEN);
            }
        } else { // user flag == 1
            s.append(uf[0]);
        }

         s.append(ImapClientConstants.DQUOTA);
        return s.toString();
    }
}
