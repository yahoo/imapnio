package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

import com.yahoo.imapnio.async.data.QResyncParameter;

/**
 * This class defines imap select command request from client.
 */
public class SelectFolderCommand extends OpenFolderActionCommand {

    /** Command name. */
    private static final String SELECT = "SELECT";

    /**
     * Initializes a {@link SelectFolderCommand}.
     *
     * @param folderName folder name to select
     */
    public SelectFolderCommand(@Nonnull final String folderName) {
        super(SELECT, folderName);
    }

    /**
     * Initializes a {@link SelectFolderCommand}.
     *
     * @param folderName folder name to select
     * @param qResyncParameter qresync parameter
     */
    public SelectFolderCommand(@Nonnull final String folderName, @Nonnull final QResyncParameter qResyncParameter) {
        super(SELECT, folderName, qResyncParameter);
    }

    /**
     * Initializes a {@link SelectFolderCommand}.
     *
     * @param folderName folder name to select
     * @param isCondstoreEnabled whether to enable CondStore
     */
    public SelectFolderCommand(@Nonnull final String folderName, final boolean isCondstoreEnabled) {
        super(SELECT, folderName, isCondstoreEnabled);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.SELECT_FOLDER;
    }
}
