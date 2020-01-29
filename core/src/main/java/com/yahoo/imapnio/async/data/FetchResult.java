package com.yahoo.imapnio.async.data;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sun.mail.imap.protocol.FetchResponse;

/**
 * This class provides the highest modification sequence and the list of fetch response from fetch command response.
 */
public class FetchResult {

    /** The highest modification sequence, only shown when CondStore is enabled. */
    private Long highestModSeq;

    /** The collection of fetch response. */
    private final List<FetchResponse> fetchResponses;

    /**
     * Initializes a {@code FetchResult} object with fetch responses collection.
     *
     * @param fetchResponses collection of fetch responses from fetch command result
     */
    public FetchResult(@Nonnull final List<FetchResponse> fetchResponses) {
        this.highestModSeq = null;
        this.fetchResponses = fetchResponses;
    }

    /**
     * Initializes a {@code FetchResult} object with the highest modification sequence and fetch responses collection.
     *
     * @param highestModSeq the highest modification from fetch command result
     * @param fetchResponses collection of fetch responses from fetch command result
     */
    public FetchResult(@Nullable final Long highestModSeq, @Nonnull final List<FetchResponse> fetchResponses) {
        this.highestModSeq = highestModSeq;
        this.fetchResponses = fetchResponses;
    }

    /**
     * @return the highest modification sequence from fetch or UID fetch command result
     */
    @Nullable
    public Long getHighestModSeq() {
        return highestModSeq;
    }

    /**
     * @return fetch responses collection from fetch or UID fetch command result
     */
    @Nonnull
    public List<FetchResponse> getFetchResponses() {
        return fetchResponses;
    }
}
