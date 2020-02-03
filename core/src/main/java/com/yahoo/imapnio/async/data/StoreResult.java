package com.yahoo.imapnio.async.data;

import java.util.List;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.FetchResponse;

/**
 * This class provides the highest modification sequence, the list of fetch response, and the list of modified message
 * number from store command response.
 */
public class StoreResult extends FetchResult {

    /** The collection of message number, only shown when CondStore is enabled. */
    private final List<Long> modifiedMsgsets;

    /**
     * Initializes a {@code StoreResult} object with fetch responses collection,
     * and modified message number collection.
     *
     * @param fetchResponses collection of fetch responses from store command result
     * @param modifiedMsgsets collection of modified message number from store command result
     */
    public StoreResult(@Nonnull final List<FetchResponse> fetchResponses, @Nonnull final List<Long> modifiedMsgsets) {
        super(fetchResponses);
        this.modifiedMsgsets = modifiedMsgsets;
    }

    /**
     * Initializes a {@code StoreResult} object with the highest modification sequence, fetch responses collection,
     * and modified message number collection.
     *
     * @param highestModSeq the highest modification from store command result
     * @param fetchResponses collection of fetch responses from store command result
     * @param modifiedMsgsets collection of modified message number from store command result
     */
    public StoreResult(@Nonnull final Long highestModSeq, @Nonnull final List<FetchResponse> fetchResponses,
                       @Nonnull final List<Long> modifiedMsgsets) {
        super(highestModSeq, fetchResponses);
        this.modifiedMsgsets = modifiedMsgsets;
    }

    /**
     * @return modified message number collection from store or UID store command result
     */
    @Nonnull
    public List<Long> getModifiedMsgsets() {
        return modifiedMsgsets;
    }
}
