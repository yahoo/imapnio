package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

/**
 * This class defines imap subscribe command request from client.
 */
public class SubscribeFolderCommand extends AbstractFolderActionCommand {

    /** Command name. */
    private static final String SUBSCRIBE = "SUBSCRIBE";

    /**
     * Initializes a {@link SubscribeFolderCommand}.
     *
     * @param folderName folder name to SUBSCRIBE
     */
    public SubscribeFolderCommand(@Nonnull final String folderName) {
        super(SUBSCRIBE, folderName);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.SUBSCRIBE;
    }
}
