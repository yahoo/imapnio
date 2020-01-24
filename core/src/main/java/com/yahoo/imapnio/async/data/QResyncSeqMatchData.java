package com.yahoo.imapnio.async.data;

import javax.annotation.Nonnull;

/**
 * This class models the QRESYNC Sequence match data defined in https://tools.ietf.org/html/rfc7162#page-26.
 */
public class QResyncSeqMatchData {
    /** Message sequence number. */
    @Nonnull
    private MessageNumberSet[] knownSequenceSet;

    /** Corresponding UIDs. */
    @Nonnull
    private MessageNumberSet[] knownUidSet;

    /**
     * Constructor.
     * @param knownSequenceSet message sequence numbers
     * @param knownUidSet UIDs
     */
    public QResyncSeqMatchData(@Nonnull final MessageNumberSet[] knownSequenceSet, @Nonnull final MessageNumberSet[] knownUidSet) {
        this.knownSequenceSet = knownSequenceSet;
        this.knownUidSet = knownUidSet;
    }

    /**
     * Get the known sequence set.
     * @return the known sequence set
     */
    public MessageNumberSet[] getKnownSequenceSet() {
        return knownSequenceSet;
    }

    /**
     * Get the known uid set.
     * @return known UID set
     */
    public MessageNumberSet[] getKnownUidSet() {
        return knownUidSet;
    }
}
