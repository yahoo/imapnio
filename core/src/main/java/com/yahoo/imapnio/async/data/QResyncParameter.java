package com.yahoo.imapnio.async.data;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class models the QRESYNC parameter defined in https://tools.ietf.org/html/rfc7162#page-26.
 */
public class QResyncParameter {
    /** Last known UIDVALIDITY. */
    private long knownUidValidity;

    /** Last known modification sequence. */
    private long knownModSeq;

    /** Optional set of known UIDs. */
    private List<MessageNumberSet> knownUids;

    /** Optional parenthesized list of known sequence ranges and their corresponding UIDs. */
    private QResyncSeqMatchData qResyncSeqMatchData;

    /**
     * Constructor.
     *
     * @param knownUidValidity last known uidvalidity
     * @param knownModSeq last known modification sequence
     * @param knownUids known UIDs
     * @param qResyncSeqMatchData known message sequence set and their corresponding UID
     */
    public QResyncParameter(final long knownUidValidity, final long knownModSeq, @Nullable final List<MessageNumberSet> knownUids,
                            @Nullable final QResyncSeqMatchData qResyncSeqMatchData) {
        this.knownUidValidity = knownUidValidity;
        this.knownModSeq = knownModSeq;
        this.knownUids = knownUids;
        this.qResyncSeqMatchData = qResyncSeqMatchData;
    }

    /**
     * Get the last known uidvalidity.
     * @return last known uidvalidity
     */
    public long getKnownUidValidity() {
        return knownUidValidity;
    }

    /**
     * Get the last known modification sequence.
     * @return last known modification sequence
     */
    public long getKnownModSeq() {
        return knownModSeq;
    }

    /**
     * Get the last known UIDs.
     * @return known UIDs
     */
    public List<MessageNumberSet> getKnownUids() {
        return knownUids;
    }

    /**
     * Get the message sequence set and corresponding UID.
     * @return QResyncSeqMatchData
     */
    public QResyncSeqMatchData getqResyncSeqMatchData() {
        return qResyncSeqMatchData;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(QRESYNC (").append(knownUidValidity).append(" ").append(knownModSeq);
        if (knownUids != null) {
            sb.append(" ");
            final MessageNumberSet[] messageNumberSets = new MessageNumberSet[knownUids.size()];
            sb.append(MessageNumberSet.buildString(knownUids.toArray(messageNumberSets)));
        }
        if (qResyncSeqMatchData != null) {
            sb.append(" (");
            sb.append(qResyncSeqMatchData.toString());
            sb.append(")");
        }
        sb.append("))");
        return sb.toString();
    }
}
