package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;
import javax.mail.Flags;

/**
 * This class encodes/formats imap command arguments properly based on the data value.
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
     * @param src the source string, assuming it is UTF-7 already
     * @param out the StringBuilder to append to
     * @param doQuote whether to quote or not
     */
    void formatArgument(@Nonnull final String src, @Nonnull final StringBuilder out, final boolean doQuote) {
        int len = src.length();

        // if 0 length, send as quoted-string
        boolean quote = len == 0 ? true : doQuote;
        boolean escape = false;

        char b;
        for (int i = 0; i < len; i++) {
            b = src.charAt(i);
            if (b == '\0' || b == '\r' || b == '\n' || ((b & MASK) > ASCII_CODE_127)) {
                // NUL, CR or LF means the bytes need to be sent as literals
                out.append(src);
                return;
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
}
