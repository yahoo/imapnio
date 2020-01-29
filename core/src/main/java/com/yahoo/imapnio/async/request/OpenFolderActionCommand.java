package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.yahoo.imapnio.async.data.MessageNumberSet;
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

    /** Literal for CONDSTORE. */
    private static final String CONDSTORE = " (CONDSTORE)";

    /** Byte array for CONDSTORE. */
    private static final byte[] CONDSTORE_B = CONDSTORE.getBytes(StandardCharsets.US_ASCII);

    /** Command operator, for example, "SELECT". */
    private String op;

    /** Folder name. */
    private String folderName;

    /** Optional QResync parameter. */
    private QResyncParameter qResyncParameter;

    /** Optional CondStore parameter. */
    private boolean condStore;

    /**
     * Initializes a {@link OpenFolderActionCommand}.
     *
     * @param op command operator
     * @param folderName folder name
     */
    protected OpenFolderActionCommand(@Nonnull final String op, @Nonnull final String folderName) {
        this.op = op;
        this.folderName = folderName;
        this.qResyncParameter = null;
        this.condStore = false;
    }

    /**
     * Initializes a {@link OpenFolderActionCommand}.
     *
     * @param op command operator
     * @param folderName folder name to select
     * @param qResyncParameter qresync parameter
     */
    public OpenFolderActionCommand(@Nonnull final String op, @Nonnull final String folderName, @Nonnull final QResyncParameter qResyncParameter) {
        this.op = op;
        this.folderName = folderName;
        this.qResyncParameter = qResyncParameter;
        this.condStore = false;
    }

    /**
     * Initializes a @{code FolderActionCommand}.
     *
     * @param op command operator
     * @param folderName folder name
     * @param condStore whether to enable CondStore
     */
    protected OpenFolderActionCommand(@Nonnull final String op, @Nonnull final String folderName, final boolean condStore) {
        this.op = op;
        this.folderName = folderName;
        this.qResyncParameter = null;
        this.condStore = condStore;
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
        int qResyncParameterSize = 0;
        int condStoreSize = 0;
        StringBuilder sb = null;
        if (qResyncParameter != null) {
            sb = new StringBuilder();
            sb.append("(QRESYNC (").append(qResyncParameter.getUidValidity()).append(ImapClientConstants.SPACE).append(qResyncParameter.getModSeq());
            if (qResyncParameter.getKnownUids() != null) {
                sb.append(ImapClientConstants.SPACE);
                sb.append(MessageNumberSet.buildString(qResyncParameter.getKnownUids()));
            }
            if (qResyncParameter.getSeqMatchData() != null) {
                sb.append(ImapClientConstants.SPACE).append("(");
                sb.append(MessageNumberSet.buildString(qResyncParameter.getSeqMatchData().getKnownSequenceSet()));
                sb.append(ImapClientConstants.SPACE);
                sb.append(MessageNumberSet.buildString(qResyncParameter.getSeqMatchData().getKnownUidSet()));
                sb.append(")");
            }
            sb.append("))");
            qResyncParameterSize = sb.length();
        }

        if (condStore) {
            condStoreSize = CONDSTORE.length();
        }
        // 2 * base64Folder.length(): assuming every char needs to be escaped, goal is eliminating resizing, and avoid complex length calculation
        final int len = 2 * base64Folder.length() + ImapClientConstants.PAD_LEN + qResyncParameterSize + condStoreSize;
        final ByteBuf byteBuf = Unpooled.buffer(len);
        byteBuf.writeBytes(op.getBytes(StandardCharsets.US_ASCII));
        byteBuf.writeByte(ImapClientConstants.SPACE);

        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
        formatter.formatArgument(base64Folder, byteBuf, false); // already base64 encoded so can be formatted and write to sb

        if (qResyncParameterSize > 0) {
            byteBuf.writeByte(ImapClientConstants.SPACE);
            byteBuf.writeBytes(sb.toString().getBytes(StandardCharsets.US_ASCII));
        }

        if (condStore) {
            byteBuf.writeBytes(CONDSTORE_B);
        }

        byteBuf.writeBytes(CRLF_B);

        return byteBuf;
    }
}
