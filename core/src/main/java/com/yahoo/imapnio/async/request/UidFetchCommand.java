package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

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
        super(true, uids, macro);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.UID_FETCH;
    }
}
