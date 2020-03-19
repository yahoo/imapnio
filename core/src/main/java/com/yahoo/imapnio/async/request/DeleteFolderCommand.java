package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

/**
 * This class defines imap delete command request from client.
 */
public class DeleteFolderCommand extends AbstractFolderActionCommand {

    /** Literal DELETE. */
    private static final String DELETE = "DELETE";

    /**
     * Initializes a {@link DeleteFolderCommand}.
     *
     * @param folderName folder name to delete
     */
    public DeleteFolderCommand(@Nonnull final String folderName) {
        super(DELETE, folderName);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.DELETE_FOLDER;
    }
}
