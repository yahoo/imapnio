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
    private String uids;

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
     * @param uids the UID string following the RFC3501 syntax. For ex:3857529045,3857529047:3857529065
     * @param what the data items or macro
     */
    public UidFetchCommand(@Nonnull final String uids, @Nonnull final String what) {
        this.uids = uids;
        this.dataItems = what;
    }

    @Override
    public void cleanup() {
        this.uids = null;
        this.dataItems = null;
    }

    @Override
    public String getCommandLine() {
        final StringBuilder sb = new StringBuilder(dataItems.length() + ImapClientConstants.PAD_LEN).append(UID_FETCH_SPACE).append(uids)
                .append(ImapClientConstants.SPACE).append(ImapClientConstants.L_PAREN).append(dataItems).append(ImapClientConstants.R_PAREN)
                .append(ImapClientConstants.CRLF);

        return sb.toString();
    }
}
