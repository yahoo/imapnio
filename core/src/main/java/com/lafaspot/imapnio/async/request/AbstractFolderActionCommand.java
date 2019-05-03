package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;

/**
 * This class defines imap abstract commands related to change operation on folder, like create folder, rename folder, delete folder.
 */
abstract class AbstractFolderActionCommand extends ImapRequestAdapter {

    /** Command operator, for example, "CREATE". */
    private String op;

    /** Folder name. */
    private String folderName;

    /**
     * Initializes a @{code FolderActionCommand}.
     *
     * @param op command operator
     * @param folderName folder name
     */
    protected AbstractFolderActionCommand(@Nonnull final String op, @Nonnull final String folderName) {
        this.op = op;
        this.folderName = folderName;
    }

    @Override
    public void cleanup() {
        this.op = null;
        this.folderName = null;
    }

    @Override
    public String getCommandLine() {

        final String base64Folder = BASE64MailboxEncoder.encode(folderName);
        // 2 * base64Folder.length(): assuming every char needs to be escaped, goal is eliminating resizing, and avoid complex length calculation
        final int len = 2 * base64Folder.length() + ImapClientConstants.PAD_LEN;
        final StringBuilder sb = new StringBuilder(len).append(op);
        sb.append(ImapClientConstants.SPACE);

        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
        formatter.formatArgument(base64Folder, sb, false); // already base64 encoded so can be formatted and write to sb
        sb.append(ImapClientConstants.CRLF);

        return sb.toString();
    }
}
