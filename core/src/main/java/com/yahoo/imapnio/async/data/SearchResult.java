package com.yahoo.imapnio.async.data;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class provides the modification sequence and the list of message sequence numbers from search command response.
 */
public class SearchResult {
    /** Search command response sequence number, could be message sequence or UID. */
    @Nonnull
    private final List<Long> msgNumbers;

    /** Modification sequence, is only shown when CondStore is enabled. */
    private final Long highestModSeq;

    /**
     * Initializes a {@link SearchResult} object with message number collection and modification sequence.
     *
     * @param msgNumbers collection of message number from search command result
     * @param highestModSeq the highest modification sequence from search command result
     */
    public SearchResult(@Nonnull final List<Long> msgNumbers, @Nullable final Long highestModSeq) {
        this.msgNumbers = msgNumbers;
        this.highestModSeq = highestModSeq;
    }

    /**
     * @return message number collection from search command or UID search command result
     */
    @Nonnull
    public List<Long> getMessageNumbers() {
        return this.msgNumbers;
    }

    /**
     * @return the highest modification sequence from search command or UID search command result
     */
    @Nullable
    public Long getHighestModSeq() {
        return this.highestModSeq;
    }
}
