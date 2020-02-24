package com.yahoo.imapnio.async.data;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class provides the list of message sequence numbers from search command response.
 */
public class SearchResult {
    /** Search command response sequence number, could be message sequence or UID. */
    @Nonnull
    private final List<Long> msgNumbers;

    /**
     * Initializes a {@link SearchResult} object with message number collection.
     *
     * @param msgNumbers collection of message number from search command result
     */
    public SearchResult(@Nonnull final List<Long> msgNumbers) {
        this.msgNumbers = msgNumbers;
    }

    /**
     * @return message number collection from search command or UID search command result
     */
    @Nullable
    public List<Long> getMessageNumbers() {
        return this.msgNumbers;
    }
}
