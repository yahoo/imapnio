package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;

/**
 * This class defines IMAP rename command request from client.
 */
public class RenameFolderCommand extends ImapRequestAdapter {

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
        // both names are already base64 encoded, so neither can need a literal
        return new ImapCommandLineBuilder(LiteralSupport.DISABLE)
                .raw(RENAME_SP_B)
                .astring(BASE64MailboxEncoder.encode(oldFolder), false, "old folder name")
                .raw((byte) ImapClientConstants.SPACE)
                .astring(BASE64MailboxEncoder.encode(newFolder), false, "new folder name")
                .finishSingle();
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.RENAME_FOLDER;
    }
}
