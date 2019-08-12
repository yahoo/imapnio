package com.yahoo.imapnio.async.data;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * This class provides the list of message sequence numbers from search command response.
 */
public class SearchResult {
    /** Search command response sequence number, could be message sequence or UID. */
    @Nullable
    private final List<Long> msgNumbers;

    /**
     * Initializes a {@code SearchResult} object with message number collection.
     *
     * @param msgNumbers collection of message number from search command result
     */
    public SearchResult(@Nullable final List<Long> msgNumbers) {
        // make it immutable here so we avoid keeping creating UnmodifiableList whenever getter is called
        this.msgNumbers = (msgNumbers != null) ? Collections.unmodifiableList(msgNumbers) : null;
    }

    /**
     * @return message number collection from search command or UID search command result
     */
    @Nullable
    public List<Long> getMessageNumbers() {
        return this.msgNumbers;
    }
}
