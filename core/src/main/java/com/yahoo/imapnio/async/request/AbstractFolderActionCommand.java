package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;

/**
 * This class defines imap abstract commands related to change operation on folder, like create folder, rename folder, delete folder.
 */
abstract class AbstractFolderActionCommand extends ImapRequestAdapter {

    /** Command operator, for example, "CREATE". */
    private String op;

    /** Folder name. */
    private String folderName;

    /**
     * Initializes a {@link AbstractFolderActionCommand}.
     *
     * @param op command operator
     * @param folderName folder name
     */
    protected AbstractFolderActionCommand(@Nonnull final String op, @Nonnull final String folderName) {
        this.op = op;
        this.folderName = folderName;
    }

    @Override
    public void cleanup() {
        this.op = null;
        this.folderName = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() throws ImapAsyncClientException {

        final String base64Folder = BASE64MailboxEncoder.encode(folderName);
        return new ImapCommandLineBuilder(LiteralSupport.DISABLE)
                .raw(op.getBytes(StandardCharsets.US_ASCII))
                .raw((byte) ImapClientConstants.SPACE)
                .astring(base64Folder, false, "folder name") // already base64 encoded, so it cannot need a literal
                .finishSingle();
    }
}
