package com.lafaspot.imapnio.async.request;


/**
 * Class for constant string in the IMAP Async client.
 */
public final class ImapClientConstants {

    /** Space character. */
    public static final char SPACE = ' ';

    /** String for CR and LF. */
    public static final String CRLF = "\r\n";

    /** Literal for CR and LF. */
    public static final int CRLFLEN = "\r\n".length();

    /** NULL character. */
    public static final char NULL = '\0';

    /** Start of header character. */
    public static final char SOH = 0x01;

    /** Left paraenthsisand space. */
    public static final char L_PAREN = '(';

    /** Left paraenthsisand space. */
    public static final char R_PAREN = ')';

    /** Literal for colon. */
    public static final String COLON = ":";

    /** Literal for plus. */
    public static final char PLUS = '+';

    /** Literal for minus. */
    public static final char MINUS = '-';

    /** NIL literal. */
    public static final String NIL = "NIL";

    /** SASL-IR capability. */
    public static final String SASL_IR = "SASL-IR";

    /** Extra buffer length for command line builder to add. */
    public static final int PAD_LEN = 100;;
    /**
     * Private constructor to avoid constructing instance of this class.
     */
    private ImapClientConstants() {
    }
}
