package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

/**
 * Metadata item type (entry-type-req) for search command with the modification sequence extension.
 */
public enum EntryTypeRequest {
    /** Entry type for a private metadata item. */
    PRIVATE("PRIV"),
    /** Entry type for a shared metadata item. */
    SHARED("SHARED"),
    /** Entry type to indicate to use the biggest value among "PRIV" and "SHARED" mod-sequences for the metadata item. */
    ALL("ALL");

    /** Name of entry type. */
    private final String typeName;

    /**
     * Initializes a {@link EntryTypeRequest} with type name.
     *
     * @param typeName name of entry type
     */
    EntryTypeRequest(@Nonnull final String typeName) {
        this.typeName = typeName;
    }

    /**
     * @return the entry type name
     */
    @Nonnull
    public String getTypeName() {
        return this.typeName;
    }
}
