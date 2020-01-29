package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.yahoo.imapnio.async.data.MessageNumberSet;

/**
 * This class defines IMAP UID fetch command request from client.
 */
public class UidFetchCommand extends AbstractFetchCommand {

    /**
     * Initializes a {@link UidFetchCommand} with the {@link MessageNumberSet} array.
     *
     * @param msgsets the set of message set
     * @param items the data items
     */
    public UidFetchCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final String items) {
        super(true, msgsets, items);
    }

    /**
     * Initializes a {@link UidFetchCommand} with the {@link MessageNumberSet} array.
     *
     * @param msgsets the set of message set
     * @param macro the macro, for example, ALL
     */
    public UidFetchCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final FetchMacro macro) {
        super(true, msgsets, macro);
    }

    /**
     * Initializes a {@link UidFetchCommand} with string form uids and data items.
     *
     * @param uids the UID string following the RFC3501 syntax. For ex:3857529045,3857529047:3857529065
     * @param items the data items
     */
    public UidFetchCommand(@Nonnull final String uids, @Nonnull final String items) {
        super(true, uids, items);
    }

    /**
     * Initializes a {@link UidFetchCommand} with string form uids and macro.
     *
     * @param uids the UID string following the RFC3501 syntax. For ex:3857529045,3857529047:3857529065
     * @param macro the macro, for example, ALL
     */
    public UidFetchCommand(@Nonnull final String uids, @Nonnull final FetchMacro macro) {
        super(true, uids, macro, null);
    }

    /**
     * Initializes a @{code UidFetchCommand} with the @{code MessageNumberSet} array, data items, and changed since the modification sequence.
     *
     * @param msgsets the set of message set
     * @param items the data items
     * @param changedSince changed since the given modification sequence
     */
    public UidFetchCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final String items, @Nullable final Long changedSince) {
        super(true, msgsets, items, changedSince);
    }

    /**
     * Initializes a @{code UidFetchCommand} with the @{code MessageNumberSet} array, macro, and changed since the modification sequence.
     *
     * @param msgsets the set of message set
     * @param macro the macro, for example, ALL
     * @param changedSince changed since the given modification sequence
     */
    public UidFetchCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final FetchMacro macro, @Nullable final Long changedSince) {
        super(true, msgsets, macro, changedSince);
    }

    /**
     * Initializes a @{code UidFetchCommand} with string form uids, data items, and changed since the modification sequence.
     *
     * @param uids the UID string following the RFC3501 syntax. For ex:3857529045,3857529047:3857529065
     * @param items the data items
     * @param changedSince changed since the given modification sequence
     */
    public UidFetchCommand(@Nonnull final String uids, @Nonnull final String items, @Nullable final Long changedSince) {
        super(true, uids, items, changedSince);
    }

    /**
     * Initializes a @{code UidFetchCommand} with string form uids, macro, and changed since the modification sequence.
     *
     * @param uids the UID string following the RFC3501 syntax. For ex:3857529045,3857529047:3857529065
     * @param macro the macro, for example, ALL
     * @param changedSince changed since the given modification sequence
     */
    public UidFetchCommand(@Nonnull final String uids, @Nonnull final FetchMacro macro, @Nullable final Long changedSince) {
        super(true, uids, macro, changedSince);
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.UID_FETCH;
    }
}
