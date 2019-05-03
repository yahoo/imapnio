package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

/**
 * This class defines imap subscribe command request from client.
 */
public class SubscribeFolderCommand extends AbstractFolderActionCommand {

    /** Command name. */
    private static final String SUBSCRIBE = "SUBSCRIBE";

    /**
     * Initializes a @{code SubscribeCommand}.
     *
     * @param folderName folder name to SUBSCRIBE
     */
    public SubscribeFolderCommand(@Nonnull final String folderName) {
        super(SUBSCRIBE, folderName);
    }
}
