package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.yahoo.imapnio.async.data.QResyncParameter;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap abstract commands related to open operation on folder, like select and examine folder.
 */
abstract class OpenFolderActionCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Command operator, for example, "SELECT". */
    private String op;

    /** Folder name. */
    private String folderName;

    /** Optional QResync parameter. */
    private QResyncParameter qResyncParameter;

    /**
     * Initializes a @{code OpenFolderActionCommand}.
     *
     * @param op command operator
     * @param folderName folder name
     */
    protected OpenFolderActionCommand(@Nonnull final String op, @Nonnull final String folderName) {
        this.op = op;
        this.folderName = folderName;
        this.qResyncParameter = null;
    }

    /**
     * Initializes a @{code OpenFolderActionCommand}.
     *
     * @param op command operator
     * @param folderName folder name to select
     * @param qResyncParameter qresync parameter
     */
    public OpenFolderActionCommand(@Nonnull final String op, @Nonnull final String folderName, @Nonnull final QResyncParameter qResyncParameter) {
        this.op = op;
        this.folderName = folderName;
        this.qResyncParameter = qResyncParameter;
    }

    @Override
    public void cleanup() {
        this.op = null;
        this.folderName = null;
        this.qResyncParameter = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() throws ImapAsyncClientException {
        final String base64Folder = BASE64MailboxEncoder.encode(folderName);
        int qResyncParamSize = 0;
        String qResyncParamStr = null;
        if (qResyncParameter != null) {
            qResyncParamStr = qResyncParameter.buildCommandLine();
            qResyncParamSize = qResyncParamStr.length();
        }
        // 2 * base64Folder.length(): assuming every char needs to be escaped, goal is eliminating resizing, and avoid complex length calculation
        final int len = 2 * base64Folder.length() + ImapClientConstants.PAD_LEN + qResyncParamSize;
        final ByteBuf sb = Unpooled.buffer(len);
        sb.writeBytes(op.getBytes(StandardCharsets.US_ASCII));
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
}
