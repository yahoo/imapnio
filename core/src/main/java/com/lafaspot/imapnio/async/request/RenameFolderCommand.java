package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;

/**
 * This class defines imap rename command request from client.
 */
public class RenameFolderCommand extends ImapRequestAdapter {

    /** Command name. */
    private static final String RENAME = "RENAME";

    /** Old folder name. */
    private String oldFolder;

    /** folder name. */
    private String newFolder;

    /**
     * Initializes a @{code RenameCommand}.
     *
     * @param oldFolder old folder name
     * @param newFolder new folder name
     */
    public RenameFolderCommand(@Nonnull final String oldFolder, @Nonnull final String newFolder) {
        this.oldFolder = oldFolder;
        this.newFolder = newFolder;
    }

    @Override
    public void cleanup() {
        this.oldFolder = null;
        this.newFolder = null;
    }

    @Override
    public String getCommandLine() {
        final int len = oldFolder.length() * 2 + newFolder.length() * 2 + ImapClientConstants.PAD_LEN;
        final StringBuilder sb = new StringBuilder(len).append(RENAME);
        sb.append(ImapClientConstants.SPACE);

        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
        final String o = BASE64MailboxEncoder.encode(oldFolder);
        formatter.formatArgument(o, sb, false); // already base64 encoded so can be formatted and write to sb
        sb.append(ImapClientConstants.SPACE);
        final String n = BASE64MailboxEncoder.encode(newFolder);
        formatter.formatArgument(n, sb, false); // already base64 encoded so can be formatted and write to sb
        sb.append(ImapClientConstants.CRLF);

        return sb.toString();
    }
}
