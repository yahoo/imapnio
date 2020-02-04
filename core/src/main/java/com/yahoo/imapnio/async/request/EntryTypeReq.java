package com.yahoo.imapnio.async.request;

/**
 * Metadata item type (entry-type-req) for search command with the modification sequence extension.
 */
public enum EntryTypeReq {
    /** Entry type for a private metadata item. */
    PRIV,
    /** Entry type for a shared metadata item. */
    SHARED,
    /** Entry type to indicate to use the biggest value among "PRIV" and "SHARED" mod-sequences for the metadata item. */
    ALL
}
