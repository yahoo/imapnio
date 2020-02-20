package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.sun.mail.imap.protocol.MessageSet;
import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap message change operation command from client. For example, copy message, move message.
 */
abstract class AbstractMessageActionCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** UID and space. */
    private static final String UID_SPACE = "UID ";

    /** Byte array for UID. */
    private static final byte[] UID_B = UID_SPACE.getBytes(StandardCharsets.US_ASCII);

    /** The command. */
    private String op;

    /** Type of the given message id values, for ex, message sequence or uid. */
    private boolean isUid;

    /** A collection of messages specified based on RFC3501 syntax. */
    private String msgNumbers;

    /** The destination folder for the email to copy to. */
    private String targetFolder;

    /**
     * Initializes a {@link AbstractMessageActionCommand} with the message sequence syntax.
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
     * Initializes a {@link AbstractMessageActionCommand} with the message sequence syntax.
     *
     * @param op the command
     * @param isUid true if it is a uid sequence
     * @param msgsets the set of {@link MessageNumberSet}
     * @param targetFolder the targetFolder to be stored
     */
    protected AbstractMessageActionCommand(@Nonnull final String op, final boolean isUid, @Nonnull final MessageNumberSet[] msgsets,
            @Nonnull final String targetFolder) {
        this(op, isUid, MessageNumberSet.buildString(msgsets), targetFolder);
    }

    /**
     * Initializes a {@link AbstractMessageActionCommand} with the start and end message sequence.
     *
     * @param op the command*
     * @param isUid true if it is a uid sequence
     * @param start the starting message sequence
     * @param end the ending message sequence
     * @param targetFolder the targetFolder to be stored
     */
    protected AbstractMessageActionCommand(@Nonnull final String op, final boolean isUid, final int start, final int end,
            @Nonnull final String targetFolder) {
        this(op, isUid, new StringBuilder(String.valueOf(start)).append(ImapClientConstants.COLON).append(end).toString(), targetFolder);
    }

    /**
     * Initializes a {@link AbstractMessageActionCommand} with the msg string directly.
     *
     * @param op the command
     * @param isUid true if it is a uid sequence
     * @param msgNumbers the messages set string
     * @param targetFolder the targetFolder to be stored
     */
    protected AbstractMessageActionCommand(@Nonnull final String op, final boolean isUid, @Nonnull final String msgNumbers,
            @Nonnull final String targetFolder) {
        this.op = op;
        this.isUid = isUid;
        this.msgNumbers = msgNumbers;
        this.targetFolder = targetFolder;
    }

    @Override
    public void cleanup() {
        this.op = null;
        this.msgNumbers = null;
        this.targetFolder = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() throws ImapAsyncClientException {

        // encode the mbox as per RFC2060
        final String base64Folder = BASE64MailboxEncoder.encode(targetFolder);
        // 2 * base64Folder.length(): assuming every char needs to be escaped, goal is eliminating resizing, and avoid complex length calculation
        final int len = 2 * base64Folder.length() + ImapClientConstants.PAD_LEN;
        final ByteBuf sb = Unpooled.buffer(len);

        if (isUid) {
            sb.writeBytes(UID_B);
        }

        sb.writeBytes(op.getBytes(StandardCharsets.US_ASCII));
        sb.writeByte(ImapClientConstants.SPACE);
        sb.writeBytes(msgNumbers.getBytes(StandardCharsets.US_ASCII));
        sb.writeByte(ImapClientConstants.SPACE);

        final ImapArgumentFormatter argWriter = new ImapArgumentFormatter();
        argWriter.formatArgument(base64Folder, sb, false);

        sb.writeBytes(CRLF_B);

        return sb;
    }
}
