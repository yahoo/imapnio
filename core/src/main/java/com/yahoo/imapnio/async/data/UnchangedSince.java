package com.yahoo.imapnio.async.data;

/**
 * This class uses the given modification sequence for Store and UID Store with CondStore Extension.
 */
public class UnchangedSince {
    /** Modification sequence. */
    private long modSeq;

    /**
     * Constructor.
     *
     * @param modSeq modification sequence
     */
    public UnchangedSince(final long modSeq) {
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
