package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

/**
 * This class defines imap examine command request from client. According to RFC3501: The EXAMINE command is identical to SELECT and returns the same
 * output; however, the selected mailbox is identified as read-only. No changes to the permanent state of the mailbox, including per-user state, are
 * permitted; in particular, EXAMINE MUST NOT cause messages to lose the \Recent flag.
 */
public class ExamineFolderCommand extends AbstractFolderActionCommand {

    /** Command name. */
    private static final String EXAMINE = "EXAMINE";

    /**
     * Initializes a @{code ExamineCommand}.
     *
     * @param folderName folder name to examine
     */
    public ExamineFolderCommand(@Nonnull final String folderName) {
        super(EXAMINE, folderName);
    }
}
