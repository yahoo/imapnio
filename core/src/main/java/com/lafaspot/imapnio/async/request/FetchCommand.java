package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.MessageSet;

/**
 * This class defines imap fetch command request from client.
 */
public class FetchCommand extends ImapRequestAdapter {

    /** FETCH and space. */
    private static final String FETCH_SPACE = "FETCH ";

    /** Message Id, either message sequence or UID. */
    private String msgIds;

    /** Fetch items. */
    private String dataItems;

    /**
     * Initializes a @{code FetchCommand} with the message sequence syntax.
     *
     * @param msgsets the set of message set
     * @param what the data items or macro
     */
    public FetchCommand(@Nonnull final MessageSet[] msgsets, @Nonnull final String what) {
        this(MessageSet.toString(msgsets), what);
    }

    /**
     * Initializes a @{code FetchCommand} with the start and end message sequence.
     *
     * @param msgStart the starting message sequence
     * @param msgEnd the ending message sequence
     * @param what the data items or macro
     */
    public FetchCommand(final int msgStart, final int msgEnd, @Nonnull final String what) {
        this(new StringBuilder(String.valueOf(msgStart)).append(ImapClientConstants.COLON).append(String.valueOf(msgEnd)).toString(), what);
    }

    /**
     * Initializes a @{code FetchCommand} with the message sequence syntax.
     *
     * @param msgIds the message sequence or UID string. For ex:2:4
     * @param what the data items or macro
     */
    private FetchCommand(@Nonnull final String msgIds, @Nonnull final String what) {
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
        final StringBuilder sb = new StringBuilder(dataItems.length() + ImapClientConstants.PAD_LEN).append(FETCH_SPACE).append(msgIds)
                .append(ImapClientConstants.SPACE).append(ImapClientConstants.L_PAREN).append(dataItems).append(ImapClientConstants.R_PAREN)
                .append(ImapClientConstants.CRLF);

        return sb.toString();
    }
}
