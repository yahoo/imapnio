package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.UIDSet;

/**
 * This class defines IMAP UID fetch command request from client.
 */
public class UidFetchCommand extends ImapRequestAdapter {

    /** UID FETCH and space. */
    private static final String UID_FETCH_SPACE = "UID FETCH ";

    /** Message UID. */
    private String msgIds;

    /** Fetch items. */
    private String dataItems;

    /**
     * Initializes a @{code UidFetchCommand} with the message sequence syntax.
     *
     * @param uidsets the set of UID set
     * @param what the data items or macro
     */
    public UidFetchCommand(@Nonnull final UIDSet[] uidsets, @Nonnull final String what) {
        this(UIDSet.toString(uidsets), what);
    }

    /**
     * Initializes a @{code UidFetchCommand} with the message sequence syntax.
     *
     * @param msgIds the message sequence or UID string. For ex:2:4
     * @param what the data items or macro
     */
    public UidFetchCommand(@Nonnull final String msgIds, @Nonnull final String what) {
        this.msgIds = msgIds;
        this.dataItems = what;
    }

    @Override
    public void cleanup() {
        this.msgIds = null;
        this.dataItems = null;
    }

    @Override
    public String getCommandLine() {
        final StringBuilder sb = new StringBuilder(dataItems.length() + ImapClientConstants.PAD_LEN).append(UID_FETCH_SPACE).append(msgIds)
                .append(ImapClientConstants.SPACE).append(ImapClientConstants.L_PAREN).append(dataItems).append(ImapClientConstants.R_PAREN)
                .append(ImapClientConstants.CRLF);

        return sb.toString();
    }
}
