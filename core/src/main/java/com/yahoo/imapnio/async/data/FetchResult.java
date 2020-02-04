package com.yahoo.imapnio.async.data;

import java.util.List;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * This class provides the list of IMAP response from fetch command response.
 */
public class FetchResult {

    /** The collection of IMAP responses. */
    private final List<IMAPResponse> imapResponses;

    /**
     * Initializes a {@code FetchResult} object with IMAP responses collection.
     *
     * @param imapResponses collection of IMAP responses from fetch command result
     */
    public FetchResult(@Nonnull final List<IMAPResponse> imapResponses) {
        this.imapResponses = imapResponses;
    }

    /**
     * @return IMAP responses collection from fetch or UID fetch command result
     */
    @Nonnull
    public List<IMAPResponse> getIMAPResponses() {
        return imapResponses;
    }
}
