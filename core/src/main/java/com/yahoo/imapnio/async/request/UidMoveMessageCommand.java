package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.UIDSet;
import com.yahoo.imapnio.async.data.MessageNumberSet;

/**
 * This class defines imap move command request from client.
 */
public class UidMoveMessageCommand extends AbstractMessageActionCommand {

    /** Command name. */
    private static final String MOVE = "MOVE";

    /**
     * Initializes a {@link UidMoveMessageCommand} with the message sequence syntax.
     *
     * @param uidsets the list of UIDSet
     * @param targetFolder the targetFolder to be stored
     */
    public UidMoveMessageCommand(@Nonnull final UIDSet[] uidsets, @Nonnull final String targetFolder) {
        super(MOVE, true, UIDSet.toString(uidsets), targetFolder);
    }

    /**
     * Initializes a {@link UidMoveMessageCommand} with the message sequence syntax.
     *
     * @param uids the string representing UID based on RFC3501
     * @param targetFolder the targetFolder to be stored
     */
    public UidMoveMessageCommand(@Nonnull final String uids, @Nonnull final String targetFolder) {
        super(MOVE, true, uids, targetFolder);
    }

    /**
     * Initializes a {@link UidMoveMessageCommand} with the {@link MessageNumberSet} array.
     *
     * @param msgsets the set of {@link MessageNumberSet}
     * @param targetFolder the targetFolder to be stored
     */
    public UidMoveMessageCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final String targetFolder) {
        super(MOVE, true, msgsets, targetFolder);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.UID_MOVE_MESSAGE;
    }
}
