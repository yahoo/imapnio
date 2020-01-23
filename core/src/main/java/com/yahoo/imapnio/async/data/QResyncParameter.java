package com.yahoo.imapnio.async.data;

import javax.annotation.Nullable;

/**
 * This class models the QRESYNC parameter defined in https://tools.ietf.org/html/rfc7162#page-26.
 */
public class QResyncParameter {
    /** Last known UIDVALIDITY. */
    private long uidValidity;

    /** Last known modification sequence. */
    private long modSeq;

    /** Optional set of known UIDs. */
    private MessageNumberSet[] knownUids;

    /** Optional parenthesized list of known sequence ranges and their corresponding UIDs. */
    private QResyncSeqMatchData seqMatchData;

    /**
     * Constructor.
     *
     * @param uidValidity last known uidvalidity
     * @param modSeq last known modification sequence
     * @param knownUids known UIDs
     * @param seqMatchData known message sequence set and their corresponding UID
     */
    public QResyncParameter(final long uidValidity, final long modSeq, @Nullable final MessageNumberSet[] knownUids,
                            @Nullable final QResyncSeqMatchData seqMatchData) {
        this.uidValidity = uidValidity;
        this.modSeq = modSeq;
        this.knownUids = knownUids;
        this.seqMatchData = seqMatchData;
    }

    /**
     * Get the last known uidvalidity.
     * @return last known uidvalidity
     */
    public long getUidValidity() {
        return uidValidity;
    }

    /**
     * Get the last known modification sequence.
     * @return last known modification sequence
     */
    public long getModSeq() {
        return modSeq;
    }

    /**
     * Get the last known UIDs.
     * @return known UIDs
     */
    public MessageNumberSet[] getKnownUids() {
        return knownUids;
    }

    /**
     * Get the message sequence set and corresponding UID.
     * @return QResyncSeqMatchData
     */
    public QResyncSeqMatchData getSeqMatchData() {
        return seqMatchData;
    }
}
