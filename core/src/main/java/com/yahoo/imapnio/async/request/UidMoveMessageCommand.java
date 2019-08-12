package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.UIDSet;

/**
 * This class defines imap move command request from client.
 */
public class UidMoveMessageCommand extends AbstractMessageActionCommand {

    /** Command name. */
    private static final String MOVE = "MOVE";

    /**
     * Initializes a @{code UidMoveMessageCommand} with the message sequence syntax.
     *
     * @param uidsets the list of UIDSet
     * @param targetFolder the targetFolder to be stored
     */
    public UidMoveMessageCommand(@Nonnull final UIDSet[] uidsets, @Nonnull final String targetFolder) {
        super(MOVE, true, UIDSet.toString(uidsets), targetFolder);
    }

    /**
     * Initializes a @{code UidMoveMessageCommand} with the message sequence syntax.
     *
     * @param uids the string representing UID based on RFC3501
     * @param targetFolder the targetFolder to be stored
     */
    public UidMoveMessageCommand(@Nonnull final String uids, @Nonnull final String targetFolder) {
        super(MOVE, true, uids, targetFolder);
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.UID_MOVE_MESSAGE;
    }
}
