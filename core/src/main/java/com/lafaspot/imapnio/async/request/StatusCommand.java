package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;

/**
 * This class defines imap status command request from client.
 */
public class StatusCommand extends ImapRequestAdapter {

    /** Status and space. */
    private static final String STATUS_SPACE = "STATUS ";

    /** Folder name. */
    private String folderName;

    /** Status data item names. */
    private String[] items;

    /**
     * Initializes a @{code StatusCommand}.
     *
     * @param folderName folder name
     * @param items list of items. Available ones are : "MESSAGES", "RECENT", "UNSEEN", "UIDNEXT", "UIDVALIDITY"
     */
    public StatusCommand(@Nonnull final String folderName, @Nonnull final String[] items) {
        this.folderName = folderName;
        this.items = items;
    }

    @Override
    public void cleanup() {
        this.folderName = null;
        this.items = null;
    }

    @Override
    public String getCommandLine() {
        // ex: STATUS "test1" (UIDNEXT MESSAGES UIDVALIDITY RECENT)
        final StringBuilder sb = new StringBuilder(STATUS_SPACE);

        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();

        final String encoded64Folder = BASE64MailboxEncoder.encode(folderName);
        formatter.formatArgument(encoded64Folder, sb, false); // already base64 encoded so can be formatted and write to sb

        sb.append(ImapClientConstants.SPACE).append(ImapClientConstants.L_PAREN);
        for (int i = 0, len = items.length; i < len; i++) {
            formatter.formatArgument(items[i], sb, false);
            if (i < len - 1) { // do not add space for last item
                sb.append(ImapClientConstants.SPACE);
            }
        }
        sb.append(ImapClientConstants.R_PAREN);

        sb.append(ImapClientConstants.CRLF);
        return sb.toString();
    }
}
