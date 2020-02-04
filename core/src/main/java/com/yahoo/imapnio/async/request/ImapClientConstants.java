package com.yahoo.imapnio.async.request;

/**
 * Class for constant string in the IMAP asynchronous client.
 */
final class ImapClientConstants {

    /** Space character. */
    static final char SPACE = ' ';

    /** Literal to cancel the command when server responds error. */
    static final char CANCEL_B = '*';

    /** String for CR and LF. */
    static final String CRLF = "\r\n";

    /** Literal for CR and LF. */
    static final int CRLFLEN = "\r\n".length();

    /** NULL character. */
    static final char NULL = '\0';

    /** Start of header character. */
    static final char SOH = 0x01;

    /** Left parenthesis and space. */
    static final char L_PAREN = '(';

    /** Left parenthesis and space. */
    static final char R_PAREN = ')';

    /** Literal for colon. */
    static final String COLON = ":";

    /** Literal for plus. */
    static final char PLUS = '+';

    /** Literal for minus. */
    static final char MINUS = '-';

    /** SASL-IR capability. */
    static final String SASL_IR = "SASL-IR";

    /** LITERAL+ capability. */
    static final String LITERAL_PLUS = "LITERAL+";

    /** Extra buffer length for command line builder to add. */
    static final int PAD_LEN = 100;

    /** Literal for double quota. */
    static final char DQUOTA = '\"';

    /** Literal for back slash. */
    static final char BACKSLASH = '\\';

    /**
     * Private constructor to avoid constructing instance of this class.
     */
    private ImapClientConstants() {
    }
}
