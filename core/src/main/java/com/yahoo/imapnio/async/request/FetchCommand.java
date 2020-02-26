package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

import com.yahoo.imapnio.async.data.MessageNumberSet;

/**
 * This class defines imap fetch command request from client.
 */
public class FetchCommand extends AbstractFetchCommand {

    /**
     * Initializes a {@link FetchCommand} with the {@link MessageNumberSet} array and fetch items.
     *
     * @param msgsets the set of message set
     * @param items the data items
     */
    public FetchCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final String items) {
        super(false, msgsets, items);
    }

    /**
     * Initializes a {@link FetchCommand} with the {@link MessageNumberSet} array and macro.
     *
     * @param msgsets the set of message set
     * @param macro the macro
     */
    public FetchCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final FetchMacro macro) {
        super(false, msgsets, macro);
    }

    /**
     * Initializes a {@link FetchCommand} with the {@code MessageNumberSet} array, fetch items, and changed since the modification sequence.
     *
     * @param msgsets the set of message set
     * @param items the data items
     * @param changedSince changed since the given modification sequence
     */
    public FetchCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final String items, @Nonnull final Long changedSince) {
        super(false, msgsets, items, changedSince, false);
    }

    /**
     * Initializes a {@link FetchCommand} with the {@code MessageNumberSet} array, macro, and changed since the modification sequence.
     *
     * @param msgsets the set of message set
     * @param macro the macro
     * @param changedSince changed since the given modification sequence
     */
    public FetchCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final FetchMacro macro, @Nonnull final Long changedSince) {
        super(false, msgsets, macro, changedSince, false);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.FETCH;
    }
}
