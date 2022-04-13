package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

import com.yahoo.imapnio.async.data.QResyncParameter;

/**
 * This class defines imap examine command request from client. According to RFC3501: The EXAMINE command is identical to SELECT and returns the same
 * output; however, the selected mailbox is identified as read-only. No changes to the permanent state of the mailbox, including per-user state, are
 * permitted; in particular, EXAMINE MUST NOT cause messages to lose the \Recent flag.
 */
public class ExamineFolderCommand extends OpenFolderActionCommand {

    /** Command name. */
    private static final String EXAMINE = "EXAMINE";

    /**
     * Initializes a {@link ExamineFolderCommand}.
     *
     * @param folderName folder name to examine
     */
    public ExamineFolderCommand(@Nonnull final String folderName) {
        super(EXAMINE, folderName);
    }

    /**
     * Initializes a {@link ExamineFolderCommand}.
     *
     * @param folderName folder name to examine
     * @param qResyncParameter qresync parameter
     */
    public ExamineFolderCommand(@Nonnull final String folderName, @Nonnull final QResyncParameter qResyncParameter) {
        super(EXAMINE, folderName, qResyncParameter);
    }

    /**
     * Initializes a {@link ExamineFolderCommand}.
     *
     * @param folderName folder name to examine
     * @param isCondstoreEnabled whether to enable CondStore
     */
    public ExamineFolderCommand(@Nonnull final String folderName, final boolean isCondstoreEnabled) {
        super(EXAMINE, folderName, isCondstoreEnabled);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.EXAMINE_FOLDER;
    }
}
