package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

/**
 * This class defines imap unsubscribe command request from client.
 */
public class UnsubscribeFolderCommand extends AbstractFolderActionCommand {

    /** Command name. */
    private static final String UNSUBSCRIBE = "UNSUBSCRIBE";

    /**
     * Initializes a {@link UnsubscribeFolderCommand}.
     *
     * @param folderName folder name to UNSUBSCRIBE
     */
    public UnsubscribeFolderCommand(@Nonnull final String folderName) {
        super(UNSUBSCRIBE, folderName);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.UNSUBSCRIBE;
    }
}
