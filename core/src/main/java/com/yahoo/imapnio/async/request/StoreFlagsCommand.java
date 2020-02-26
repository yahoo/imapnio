package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;
import javax.mail.Flags;

import com.yahoo.imapnio.async.data.MessageNumberSet;

/**
 * This class defines imap store command request from client.
 */
public class StoreFlagsCommand extends AbstractStoreFlagsCommand {

    /**
     * Initializes a {@link StoreFlagsCommand} with the MessageNumberSet array, Flags and action.Requests server to return the new value.
     *
     * @param msgsets the set of message set
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     */
    public StoreFlagsCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final Flags flags, @Nonnull final FlagsAction action) {
        super(false, msgsets, flags, action, false);
    }

    /**
     * Initializes a {@link StoreFlagsCommand} with the MessageNumberSet array and flags.
     *
     * @param msgsets the set of message set
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     * @param silent true if asking server to respond silently
     */
    public StoreFlagsCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final Flags flags, @Nonnull final FlagsAction action,
            final boolean silent) {
        super(false, msgsets, flags, action, silent);
    }

    /**
     * Initializes a {@link StoreFlagsCommand} with string form message numbers, Flags, action, flag whether to request server to return the new
     * value.
     *
     * @param msgNumbers the message numbers in string format
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     * @param silent true if asking server to respond silently; false if requesting server to return the new values
     */
    public StoreFlagsCommand(@Nonnull final String msgNumbers, @Nonnull final Flags flags, @Nonnull final FlagsAction action, final boolean silent) {
        super(false, msgNumbers, flags, action, silent);
    }

    /**
     * Initializes a {@link StoreFlagsCommand} with the MessageNumberSet array, Flags, action, and unchanged since modification sequence.
     * Requests server to return the new value.
     *
     * @param msgsets the set of message set
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     * @param unchangedSince unchanged since the given modification sequence
     */
    public StoreFlagsCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final Flags flags, @Nonnull final FlagsAction action,
                             @Nonnull final Long unchangedSince) {
        super(false, msgsets, flags, action, false, unchangedSince);
    }

    /**
     * Initializes a {@link StoreFlagsCommand} with the MessageNumberSet array, flags, action, flag whether to request server to return the new
     * value, and unchanged since modification sequence.
     *
     * @param msgsets the set of message set
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     * @param silent true if asking server to respond silently
     * @param unchangedSince unchanged since the given modification sequence
     */
    public StoreFlagsCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final Flags flags, @Nonnull final FlagsAction action,
                             final boolean silent, @Nonnull final Long unchangedSince) {
        super(false, msgsets, flags, action, silent, unchangedSince);
    }

    /**
     * Initializes a {@link StoreFlagsCommand} with string form message numbers, Flags, action, flag whether to request server to return the new
     * value.
     *
     * @param msgNumbers the message numbers in string format
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     * @param silent true if asking server to respond silently; false if requesting server to return the new values
     * @param unchangedSince unchanged since the given modification sequence
     */
    public StoreFlagsCommand(@Nonnull final String msgNumbers, @Nonnull final Flags flags, @Nonnull final FlagsAction action,
                             final boolean silent, @Nonnull final Long unchangedSince) {
        super(false, msgNumbers, flags, action, silent, unchangedSince);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.STORE_FLAGS;
    }
}
