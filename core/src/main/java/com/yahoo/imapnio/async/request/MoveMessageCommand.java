package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.MessageSet;

/**
 * This class defines imap move command request from client.
 */
public class MoveMessageCommand extends AbstractMessageActionCommand {

    /** Command name. */
    private static final String MOVE = "MOVE";

    /**
     * Initializes a @{code MoveCommand} with the message sequence syntax.
     *
     * @param msgsets the set of message set
     * @param targetFolder the targetFolder to be stored
     */
    public MoveMessageCommand(@Nonnull final MessageSet[] msgsets, @Nonnull final String targetFolder) {
        super(MOVE, false, msgsets, targetFolder);
    }

    /**
     * Initializes a @{code MoveCommand} with the start and end message sequence.
     *
     * @param start the starting message sequence
     * @param end the ending message sequence
     * @param targetFolder the targetFolder to be stored
     */
    public MoveMessageCommand(final int start, final int end, @Nonnull final String targetFolder) {
        super(MOVE, false, start, end, targetFolder);
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.MOVE_MESSAGE;
    }
}
