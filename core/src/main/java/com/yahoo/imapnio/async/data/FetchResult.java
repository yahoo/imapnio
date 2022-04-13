package com.yahoo.imapnio.async.data;

import java.util.List;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.FetchResponse;

/**
 * This class provides the list of Fetch responses from fetch command response with parsing from FetchResponseMapper.
 */
public class FetchResult {

    /** The collection of Fetch responses. */
    private final List<FetchResponse> fetchResponses;

    /**
     * Initializes a {@link FetchResult} object with Fetch responses collection.
     *
     * @param fetchResponses collection of Fetch responses from fetch command result
     */
    public FetchResult(@Nonnull final List<FetchResponse> fetchResponses) {
        this.fetchResponses = fetchResponses;
    }

    /**
     * @return Fetch responses collection from fetch or UID fetch command result
     */
    @Nonnull
    public List<FetchResponse> getFetchResponses() {
        return fetchResponses;
    }
}
