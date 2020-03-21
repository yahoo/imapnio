package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines IMAP rename command request from client.
 */
public class RenameFolderCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Command name. */
    private static final String RENAME_SP = "RENAME ";

    /** Byte array for RENAME. */
    private static final byte[] RENAME_SP_B = RENAME_SP.getBytes(StandardCharsets.US_ASCII);

    /** Old folder name. */
    private String oldFolder;

    /** folder name. */
    private String newFolder;

    /**
     * Initializes a {@link RenameFolderCommand}.
     *
     * @param oldFolder old folder name
     * @param newFolder new folder name
     */
    public RenameFolderCommand(@Nonnull final String oldFolder, @Nonnull final String newFolder) {
        this.oldFolder = oldFolder;
        this.newFolder = newFolder;
    }

    @Override
    public void cleanup() {
        this.oldFolder = null;
        this.newFolder = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() throws ImapAsyncClientException {
        final int len = oldFolder.length() * 2 + newFolder.length() * 2 + ImapClientConstants.PAD_LEN;
        final ByteBuf sb = Unpooled.buffer(len);
        sb.writeBytes(RENAME_SP_B);

        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
        final String o = BASE64MailboxEncoder.encode(oldFolder);
        formatter.formatArgument(o, sb, false); // already base64 encoded so can be formatted and write to sb
        sb.writeByte(ImapClientConstants.SPACE);
        final String n = BASE64MailboxEncoder.encode(newFolder);
        formatter.formatArgument(n, sb, false); // already base64 encoded so can be formatted and write to sb
        sb.writeBytes(CRLF_B);

        return sb;
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.RENAME_FOLDER;
    }
}
