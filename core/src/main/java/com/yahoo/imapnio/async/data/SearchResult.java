package com.yahoo.imapnio.async.data;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class provides the modification sequence and the list of message sequence numbers from search command response.
 */
public class SearchResult {
    /** Search command response sequence number, could be message sequence or UID. */
    private final List<Long> msgNumbers;

    /** Modification sequence, is only shown when CondStore is enabled. */
    private final Long modSeq;

    /**
     * Initializes a {@link SearchResult} object with message number collection.
     *
     * @param msgNumbers collection of message number from search command result
     */
    public SearchResult(@Nonnull final List<Long> msgNumbers) {
        this(msgNumbers, null);
    }

    /**
     * Initializes a {@code SearchResult} object with message number collection and modification sequence.
     *
     * @param msgNumbers collection of message number from search command result
     * @param modSeq modification sequence from search command result
     */
    public SearchResult(@Nonnull final List<Long> msgNumbers, @Nullable final Long modSeq) {
        this.msgNumbers = msgNumbers;
        this.modSeq = modSeq;
    }

    /**
     * @return message number collection from search command or UID search command result
     */
    @Nullable
    public List<Long> getMessageNumbers() {
        return this.msgNumbers;
    }

    /**
     * @return modification sequence from search command or UID search command result
     */
    @Nullable
    public Long getModSeq() {
        return this.modSeq;
    }
}
