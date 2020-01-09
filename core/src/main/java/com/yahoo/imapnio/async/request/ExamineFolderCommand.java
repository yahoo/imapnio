package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.yahoo.imapnio.async.data.QResyncParameter;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap examine command request from client. According to RFC3501: The EXAMINE command is identical to SELECT and returns the same
 * output; however, the selected mailbox is identified as read-only. No changes to the permanent state of the mailbox, including per-user state, are
 * permitted; in particular, EXAMINE MUST NOT cause messages to lose the \Recent flag.
 */
public class ExamineFolderCommand extends AbstractFolderActionCommand {

    /** Command name. */
    private static final String EXAMINE = "EXAMINE";

    /** Optional QResync parameter. */
    private QResyncParameter qResyncParameter;

    /**
     * Initializes a @{code ExamineCommand}.
     *
     * @param folderName folder name to examine
     */
    public ExamineFolderCommand(@Nonnull final String folderName) {
        super(EXAMINE, folderName);
        this.qResyncParameter = null;
    }

    /**
     * Initializes a @{code ExamineCommand}.
     *
     * @param folderName folder name to examine
     * @param qResyncParameter qresync parameter
     */
    public ExamineFolderCommand(@Nonnull final String folderName, @Nonnull final QResyncParameter qResyncParameter) {
        super(EXAMINE, folderName);
        this.qResyncParameter = qResyncParameter;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.qResyncParameter = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() throws ImapAsyncClientException {
        final String base64Folder = BASE64MailboxEncoder.encode(getFolderName());
        int qResyncParamSize = 0;
        String qResyncParamStr = null;
        if (qResyncParameter != null) {
            qResyncParamStr = qResyncParameter.toString();
            qResyncParamSize = qResyncParamStr.length();
        }
        // 2 * base64Folder.length(): assuming every char needs to be escaped, goal is eliminating resizing, and avoid complex length calculation
        final int len = 2 * base64Folder.length() + ImapClientConstants.PAD_LEN + qResyncParamSize;
        final ByteBuf sb = Unpooled.buffer(len);
        sb.writeBytes(getOp().getBytes(StandardCharsets.US_ASCII));
        sb.writeByte(ImapClientConstants.SPACE);

        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
        formatter.formatArgument(base64Folder, sb, false); // already base64 encoded so can be formatted and write to sb

        if (qResyncParamStr != null) {
            sb.writeByte(ImapClientConstants.SPACE);
            sb.writeBytes(qResyncParamStr.getBytes(StandardCharsets.US_ASCII));
        }
        sb.writeBytes(CRLF_B);

        return sb;
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.EXAMINE_FOLDER;
    }
}
