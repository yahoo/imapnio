package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.UIDSet;

/**
 * This class defines IMAP UID EXPUNGE command from client.
 */
public class UidExpungeCommand extends ImapRequestAdapter {

    /** Command name. */
    private static final String UID_EXPUNGE = "UID EXPUNGE";

    /** Message Id, aka UID. */
    private String uids;

    /**
     * Initializes a @{code UidExpungeCommand} with the message sequence syntax.
     *
     * @param uidsets the set of UIDSet representing UID based on RFC3501
     */
    public UidExpungeCommand(@Nonnull final UIDSet[] uidsets) {
        this(UIDSet.toString(uidsets));
    }

    /**
     * Initializes a @{code UidExpungeCommand} with the message sequence syntax.
     *
     * @param uids the string representing UID string based on RFC3501
     */
    public UidExpungeCommand(@Nonnull final String uids) {
        this.uids = uids;
    }

    @Override
    public void cleanup() {
        this.uids = null;
    }

    @Override
    public String getCommandLine() {
        final StringBuilder sb = new StringBuilder(UID_EXPUNGE).append(ImapClientConstants.SPACE).append(uids).append(ImapClientConstants.CRLF);

        return sb.toString();
    }
}
