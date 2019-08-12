package com.yahoo.imapnio.async.data;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * This class provides the list of message sequence number from search command result.
 */
public class SearchResult {
    /** Message sequences from search command response. */
    @Nullable
    private final List<Integer> msgSeq;

    /**
     * Initializes a {@code SearchResult} object with message sequences.
     *
     * @param msgSeq collection of message sequence from search result
     */
    public SearchResult(@Nullable final List<Integer> msgSeq) {
        // make it immutable here so we avoid keeping creating UnmodifiableList whenever getter is called
        this.msgSeq = (msgSeq != null) ? Collections.unmodifiableList(msgSeq) : null;
    }

    /**
     * @return message sequence array
     */
    @Nullable
    public List<Integer> getMessageSequence() {
        return this.msgSeq;
    }
}
