package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.MessageSet;

/**
 * This class defines imap copy command from client.
 */
public class CopyMessageCommand extends AbstractMessageActionCommand {

    /** Command name. */
    private static final String COPY = "COPY";

    /**
     * Initializes a @{code CopyMessageCommand} with the message sequence syntax.
     *
     * @param msgsets the set of message set
     * @param targetFolder the targetFolder to be stored
     */
    public CopyMessageCommand(@Nonnull final MessageSet[] msgsets, @Nonnull final String targetFolder) {
        super(COPY, false, msgsets, targetFolder);
    }

    /**
     * Initializes a @{code CopyMessageCommand} with the start and end message sequence.
     *
     * @param start the starting message sequence
     * @param end the ending message sequence
     * @param targetFolder the targetFolder to be stored
     */
    public CopyMessageCommand(final int start, final int end, @Nonnull final String targetFolder) {
        super(COPY, false, start, end, targetFolder);
    }
}
