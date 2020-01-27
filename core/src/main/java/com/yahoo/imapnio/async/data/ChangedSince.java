package com.yahoo.imapnio.async.data;

/**
 * This class uses the given modification sequence for Fetch and UID Fetch with CondStore Extension.
 */
public class ChangedSince {
    /** Modification sequence. */
    private long modSeq;

    /**
     * Constructor.
     *
     * @param modSeq modification sequence
     */
    public ChangedSince(final long modSeq) {
        this.modSeq = modSeq;
    }

    /**
     * Get modification sequence.
     * @return modification sequence
     */
    public long getModSeq() {
        return modSeq;
    }
}

