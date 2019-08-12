package com.yahoo.imapnio.async.data;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * This class provides the list of UID from UID search command result.
 */
public class UidSearchResult {
    /** UID collection from UID search command response. */
    @Nullable
    private final List<Long> uids;

    /**
     * Initializes a {@code UidSearchResult} object with UID collection.
     *
     * @param uids collection of UID from UID search command result
     */
    public UidSearchResult(@Nullable final List<Long> uids) {
        // make it immutable here so we avoid keeping creating UnmodifiableList whenever getter is called
        this.uids = (uids != null) ? Collections.unmodifiableList(uids) : null;
    }

    /**
     * @return UID collection from UID search command result
     */
    @Nullable
    public List<Long> getUids() {
        return this.uids;
    }
}
