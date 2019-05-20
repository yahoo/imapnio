package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.sun.mail.imap.protocol.MessageSet;

/**
 * This class defines imap message change operation command from client. For example, copy message, move message.
 */
abstract class AbstractMessageActionCommand extends ImapRequestAdapter {

    /** UID and space. */
    private static final String UID_SPACE = "UID ";

    /** The command. */
    private String op;

    /** Type of the given message id values, for ex, message sequence or uid. */
    private boolean isUid;

    /** A collection of messages specified based on RFC3501 syntax. */
    private String msgset;

    /** The destionation folder for the email to copy to. */
    private String targetFolder;

    /**
     * Initializes a @{code MessageActionCommand} with the message sequence syntax.
     *
     * @param op the command
     * @param isUid true if it is a uid sequence
     * @param msgsets the set of message set
     * @param targetFolder the targetFolder to be stored
     */
    protected AbstractMessageActionCommand(@Nonnull final String op, final boolean isUid, @Nonnull final MessageSet[] msgsets,
            @Nonnull final String targetFolder) {
        this(op, isUid, MessageSet.toString(msgsets), targetFolder);
    }

    /**
     * Initializes a @{code MessageActionCommand} with the start and end message sequence.
     *
     * @param op the command*
     * @param isUid true if it is a uid sequence
     * @param start the starting message sequence
     * @param end the ending message sequence
     * @param targetFolder the targetFolder to be stored
     */
    protected AbstractMessageActionCommand(@Nonnull final String op, final boolean isUid, final int start, final int end,
            @Nonnull final String targetFolder) {
        this(op, isUid, new StringBuilder(String.valueOf(start)).append(ImapClientConstants.COLON).append(String.valueOf(end)).toString(),
                targetFolder);
    }

    /**
     * Initializes a @{code MessageActionCommand} with the msg string directly.
     *
     * @param op the command
     * @param isUid true if it is a uid sequence
     * @param msgset the messages set
     * @param targetFolder the targetFolder to be stored
     */
    private AbstractMessageActionCommand(@Nonnull final String op, final boolean isUid, @Nonnull final String msgset,
            @Nonnull final String targetFolder) {
        this.op = op;
        this.isUid = isUid;
        this.msgset = msgset;
        this.targetFolder = targetFolder;
    }

    @Override
    public void cleanup() {
        this.op = null;
        this.msgset = null;
        this.targetFolder = null;
    }

    @Override
    public String getCommandLine() {

        // encode the mbox as per RFC2060
        final String base64Folder = BASE64MailboxEncoder.encode(targetFolder);
        // 2 * base64Folder.length(): assuming every char needs to be escaped, goal is eliminating resizing, and avoid complex length calculation
        final int len = 2 * base64Folder.length() + ImapClientConstants.PAD_LEN;
        final StringBuilder sb = new StringBuilder(len);

        if (isUid) {
            sb.append(UID_SPACE);
        }

        sb.append(op).append(ImapClientConstants.SPACE).append(msgset).append(ImapClientConstants.SPACE);

        final ImapArgumentFormatter argWriter = new ImapArgumentFormatter();
        argWriter.formatArgument(base64Folder, sb, false);

        sb.append(ImapClientConstants.CRLF);

        return sb.toString();
    }
}
