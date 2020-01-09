package com.yahoo.imapnio.async.data;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * This class models the QRESYNC Sequence match data.
 */
public class QResyncSeqMatchData {
    /** Message sequence number. */
    private List<MessageNumberSet> messageSeqNumbers;

    /** Corresponding UIDs. */
    private List<MessageNumberSet> uids;

    /**
     * Constructor
     * @param messageNumberSet message sequence numbers
     * @param uid UIDs
     */
    public QResyncSeqMatchData(@Nonnull final List<MessageNumberSet> messageNumberSet, @Nonnull final List<MessageNumberSet> uid) {
        this.messageSeqNumbers = messageNumberSet;
        this.uids = uid;
    }

    /**
     * Get the message sequence number.
     * @return message sequence numbers
     */
    public List<MessageNumberSet> getMessageSeqNumbers() {
        return messageSeqNumbers;
    }

    /**
     * Get the uid numbers.
     * @return UIDs
     */
    public List<MessageNumberSet> getUids() {
        return uids;
    }

    /**
     * Convert to command line string.
     * @return String
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (messageSeqNumbers != null && messageSeqNumbers.size() > 0) {
            sb.append(MessageNumberSet.buildString(messageSeqNumbers.toArray(new MessageNumberSet[0])));
        }
        if (uids != null && uids.size() > 0) {
            if (sb.length() != 0) {
                sb.append(" ");
            }
            sb.append(MessageNumberSet.buildString(uids.toArray(new MessageNumberSet[0])));
        }
        return sb.toString();
    }
}
