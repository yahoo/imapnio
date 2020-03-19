package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.UIDSet;
import com.yahoo.imapnio.async.data.MessageNumberSet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines IMAP UID EXPUNGE command from client.
 */
public class UidExpungeCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Command name. */
    private static final String UID_EXPUNGE = "UID EXPUNGE";

    /** Byte array for UID EXPUNGE. */
    private static final byte[] UID_EXPUNGE_B = UID_EXPUNGE.getBytes(StandardCharsets.US_ASCII);

    /** Message Id, aka UID. */
    private String uids;

    /**
     * Initializes a {@link UidExpungeCommand} with the message sequence syntax.
     *
     * @param uidsets the set of UIDSet representing UID based on RFC3501
     */
    public UidExpungeCommand(@Nonnull final UIDSet[] uidsets) {
        this(UIDSet.toString(uidsets));
    }

    /**
     * Initializes a {@link UidExpungeCommand} with the message sequence syntax. MessageNumberSet allows last message.
     *
     * @param uidsets the set of MessageNumberSet representing UID based on RFC3501
     */
    public UidExpungeCommand(@Nonnull final MessageNumberSet[] uidsets) {
        this(MessageNumberSet.buildString(uidsets));
    }

    /**
     * Initializes a {@link UidExpungeCommand} with the message sequence syntax.
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
    public ByteBuf getCommandLineBytes() {
        final ByteBuf buf = Unpooled.buffer(UID_EXPUNGE.length() + uids.length() + ImapClientConstants.PAD_LEN);
        buf.writeBytes(UID_EXPUNGE_B);
        buf.writeByte(ImapClientConstants.SPACE);
        buf.writeBytes(uids.getBytes(StandardCharsets.US_ASCII));
        buf.writeBytes(CRLF_B);
        return buf;
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.UID_EXPUNGE;
    }
}
