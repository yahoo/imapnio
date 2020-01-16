package com.yahoo.imapnio.async.data;

import java.util.List;

import javax.annotation.Nullable;

/**
 * This class models the QRESYNC Sequence match data defined in https://tools.ietf.org/html/rfc7162#page-26.
 */
public class QResyncSeqMatchData {
    /** Message sequence number. */
    private MessageNumberSet[] knownSequenceSet;

    /** Corresponding UIDs. */
    private MessageNumberSet[] knownUidSet;

    /**
     * Constructor.
     * @param knownSequenceSet message sequence numbers
     * @param knownUidSet UIDs
     */
    public QResyncSeqMatchData(@Nullable final List<MessageNumberSet> knownSequenceSet, @Nullable final List<MessageNumberSet> knownUidSet) {
        if (knownSequenceSet != null) {
            this.knownSequenceSet = knownSequenceSet.toArray(new MessageNumberSet[0]);
        }
        if (knownUidSet != null) {
            this.knownUidSet = knownUidSet.toArray(new MessageNumberSet[0]);
        }
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

    /**
     * Construct the command line string for message sequence match data.
     * @return the command line string
     */
    public String buildCommandLine() {
        StringBuilder sb = new StringBuilder();
        if (knownSequenceSet != null && knownSequenceSet.length > 0) {
            sb.append(MessageNumberSet.buildString(knownSequenceSet));
        }
        if (knownUidSet != null && knownUidSet.length > 0) {
            if (sb.length() != 0) {
                sb.append(" ");
            }
            sb.append(MessageNumberSet.buildString(knownUidSet));
        }
        return sb.toString();
    }
}
