package com.yahoo.imapnio.async.request;

/**
 * Literal support variation.
 */
public enum LiteralSupport {
    /** Use Literal+ support to send literals. */
    ENABLE_LITERAL_PLUS,
    /** Use Literal- support to send literals. */
    ENABLE_LITERAL_MINUS,
    /** Disabling using literal support, just use standard RFC3501 specifications. */
    DISABLE
}
