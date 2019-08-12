package com.yahoo.imapnio.async.request;

/**
 * Flags action used for store command.
 */
public enum FlagsAction {
    /** Replace the flags given in the flags list for the message. */
    REPLACE,
    /** Add the flags given in the flags list for the message. */
    ADD,
    /** Remove the flags given in the flags list for the message. */
    REMOVE
}
