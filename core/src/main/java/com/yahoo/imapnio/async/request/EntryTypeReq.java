package com.yahoo.imapnio.async.request;

/**
 * Metadata item type for search command with the modification sequence extension.
 */
public enum EntryTypeReq {
    /** Entry type for a private metadata item. */
    PRIV,
    /** Entry type for a shared metadata item. */
    SHARED,
    /** Entry type for both private and shared. */
    ALL
}
