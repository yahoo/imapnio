package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;
import javax.mail.Flags;

import com.yahoo.imapnio.async.data.MessageNumberSet;

/**
 * This class defines IMAP UID store command request from client.
 */
public class UidStoreFlagsCommand extends AbstractStoreFlagsCommand {

    /**
     * Initializes a {@link UidStoreFlagsCommand} with the MessageNumberSet array, Flags and action. Requests server to return the new value.
     *
     * @param msgsets the set of message set
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     */
    public UidStoreFlagsCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final Flags flags, @Nonnull final FlagsAction action) {
        super(true, msgsets, flags, action, false);
    }

    /**
     * Initializes a {@link UidStoreFlagsCommand} with the MessageNumberSet array, Flags, action, flag whether to request server to return the new
     * value.
     *
     * @param msgsets the set of message set
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     * @param silent true if asking server to respond silently; false if requesting server to return the new values
     */
    public UidStoreFlagsCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final Flags flags, @Nonnull final FlagsAction action,
            final boolean silent) {
        super(true, msgsets, flags, action, silent);
    }

    /**
     * Initializes a {@link UidStoreFlagsCommand} with string form message numbers, Flags, action, flag whether to request server to return the new
     * value.
     *
     * @param uids the string representing UID based on RFC3501
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     * @param silent true if asking server to respond silently; false if requesting server to return the new values
     */
    public UidStoreFlagsCommand(@Nonnull final String uids, @Nonnull final Flags flags, @Nonnull final FlagsAction action, final boolean silent) {
        super(true, uids, flags, action, silent);
    }

    /**
     * Initializes a {@link UidStoreFlagsCommand} with the MessageNumberSet array, Flags and action. Requests server to return the new value.
     *
     * @param msgsets the set of message set
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     * @param unchangedSince unchanged since the given modification sequence
     */
    public UidStoreFlagsCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final Flags flags, @Nonnull final FlagsAction action,
                                @Nonnull final Long unchangedSince) {
        super(true, msgsets, flags, action, false, unchangedSince);
    }

    /**
     * Initializes a {@link UidStoreFlagsCommand} with the MessageNumberSet array, Flags, action, flag whether to request server to return the new
     * value, and unchanged since the modification sequence.
     *
     * @param msgsets the set of message set
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     * @param silent true if asking server to respond silently; false if requesting server to return the new values
     * @param unchangedSince unchanged since the given modification sequence
     */
    public UidStoreFlagsCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final Flags flags, @Nonnull final FlagsAction action,
                                final boolean silent, @Nonnull final Long unchangedSince) {
        super(true, msgsets, flags, action, silent, unchangedSince);
    }

    /**
     * Initializes a {@link UidStoreFlagsCommand} with string form message numbers, Flags, action, flag whether to request server to return the new
     * value, and the unchanged since the given modification sequence.
     *
     * @param uids the string representing UID based on RFC3501
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     * @param silent true if asking server to respond silently; false if requesting server to return the new values
     * @param unchangedSince unchanged since the given modification sequence
     */
    public UidStoreFlagsCommand(@Nonnull final String uids, @Nonnull final Flags flags, @Nonnull final FlagsAction action,
                                final boolean silent, @Nonnull final Long unchangedSince) {
        super(true, uids, flags, action, silent, unchangedSince);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.UID_STORE_FLAGS;
    }
}
