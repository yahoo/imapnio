package com.yahoo.imapnio.async.data;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sun.mail.imap.protocol.FetchResponse;

/**
 * This class provides the highest modification sequence, the list of Fetch responses, and the list of modified messages number
 * from store command response based on RFC3501 and RFC7162 as following.
 *
 * <pre>
 * resp-text-code      =/ "HIGHESTMODSEQ" SP mod-sequence-value /
 *                        "MODIFIED" SP sequence-set
 * </pre>
 */
public class StoreResult {

    /** The highest modification sequence. */
    private Long highestModSeq;

    /** The collection of Fetch responses. */
    private final List<FetchResponse> fetchResponses;

    /** The collection of message number as sequence-set, only shown when CondStore is enabled. */
    private final MessageNumberSet[] modifiedMsgSets;

    /**
     * Initializes a {@link StoreResult} object with the highest modification sequence, Fetch responses collection,
     * and modified message number collection.
     *
     * @param highestModSeq the highest modification from store command result
     * @param fetchResponses collection of Fetch responses from store command result
     * @param modifiedMsgSets collection of modified message number from store command result
     */
    public StoreResult(@Nullable final Long highestModSeq, @Nonnull final List<FetchResponse> fetchResponses,
                       @Nullable final MessageNumberSet[] modifiedMsgSets) {
        this.highestModSeq = highestModSeq;
        this.fetchResponses = fetchResponses;
        this.modifiedMsgSets = modifiedMsgSets;
    }

    /**
     * @return the highest modification sequence from store or UID store command result
     */
    @Nullable
    public Long getHighestModSeq() {
        return highestModSeq;
    }

    /**
     * @return Fetch responses collection from store or UID store command result
     */
    @Nonnull
    public List<FetchResponse> getFetchResponses() {
        return fetchResponses;
    }

    /**
     * @return modified message number collection from store or UID store command result
     */
    @Nullable
    public MessageNumberSet[] getModifiedMsgSets() {
        return modifiedMsgSets;
    }
}
