package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.sun.mail.imap.protocol.MessageSet;
import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;

/**
 * This class defines imap message change operation command from client. For example, copy message, move message.
 */
abstract class AbstractMessageActionCommand extends ImapRequestAdapter {

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
        final ImapCommandLineBuilder builder = new ImapCommandLineBuilder(LiteralSupport.DISABLE);

        if (isUid) {
            builder.raw(UID_B);
        }

        // sequence-set holds only numbers and the separators that join them
        return builder.raw(op.getBytes(StandardCharsets.US_ASCII))
                .raw((byte) ImapClientConstants.SPACE)
                .raw(new ImapArgumentFormatter().validateSequenceSet(msgNumbers, "message number").getBytes(StandardCharsets.US_ASCII))
                .raw((byte) ImapClientConstants.SPACE)
                .astring(base64Folder, false, "folder name") // already base64 encoded, so it cannot need a literal
                .finishSingle();
    }
}
