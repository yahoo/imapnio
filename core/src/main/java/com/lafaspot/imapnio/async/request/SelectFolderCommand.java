package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

/**
 * This class defines imap select command request from client.
 */
public class SelectFolderCommand extends AbstractFolderActionCommand {

    /** Command name. */
    private static final String SELECT = "SELECT";

    /**
     * Initializes a @{code SelectCommand}.
     *
     * @param folderName folder name to select
     */
    public SelectFolderCommand(@Nonnull final String folderName) {
        super(SELECT, folderName);
    }
}
