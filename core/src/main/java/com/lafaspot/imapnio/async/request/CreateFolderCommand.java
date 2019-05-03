package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

/**
 * This class defines imap create command request from client.
 */
public class CreateFolderCommand extends AbstractFolderActionCommand {

    /** Literal Create. */
    private static final String CREATE = "CREATE";

    /**
     * Initializes a @{code CreateCommand}.
     *
     * @param folderName folder name to create
     */
    public CreateFolderCommand(@Nonnull final String folderName) {
        super(CREATE, folderName);
    }
}
