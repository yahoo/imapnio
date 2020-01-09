package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

import com.yahoo.imapnio.async.data.QResyncParameter;

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

    /**
     * Initializes a @{code SelectCommand}.
     *
     * @param folderName folder name to select
     * @param qResyncParameter qresync parameter
     */
    public SelectFolderCommand(@Nonnull final String folderName, @Nonnull final QResyncParameter qResyncParameter) {
        super(SELECT, folderName, qResyncParameter);
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.SELECT_FOLDER;
    }
}
