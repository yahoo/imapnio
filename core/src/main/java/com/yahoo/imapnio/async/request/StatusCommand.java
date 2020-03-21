package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap status command request from client. RFC 3501 ABNF for status command.
 *
 * <pre>
 * status          = "STATUS" SP mailbox SP
 *                   "(" status-att *(SP status-att) ")"
 * status-att      = "MESSAGES" / "RECENT" / "UIDNEXT" / "UIDVALIDITY" /
 *                   "UNSEEN"
 * </pre>
 */
public class StatusCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Status and space. */
    private static final String STATUS_SP = "STATUS ";

    /** Byte array for STATUS. */
    private static final byte[] STATUS_SP_B = STATUS_SP.getBytes(StandardCharsets.US_ASCII);

    /** Folder name. */
    private String folderName;

    /** Status data item names. */
    private String[] items;

    /**
     * Initializes a {@link StatusCommand}.
     *
     * @param folderName folder name
     * @param items list of items. Available ones are : "MESSAGES", "RECENT", "UNSEEN", "UIDNEXT", "UIDVALIDITY"
     */
    public StatusCommand(@Nonnull final String folderName, @Nonnull final String[] items) {
        this.folderName = folderName;
        this.items = items;
    }

    @Override
    public void cleanup() {
        this.folderName = null;
        this.items = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() throws ImapAsyncClientException {

        final ByteBuf sb = Unpooled.buffer(ImapClientConstants.PAD_LEN);
        // ex: STATUS "test1" (UIDNEXT MESSAGES UIDVALIDITY RECENT)
        sb.writeBytes(STATUS_SP_B);

        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();

        final String encoded64Folder = BASE64MailboxEncoder.encode(folderName);
        formatter.formatArgument(encoded64Folder, sb, false); // already base64 encoded so can be formatted and write to sb

        sb.writeByte(ImapClientConstants.SPACE);
        sb.writeByte(ImapClientConstants.L_PAREN);
        for (int i = 0, len = items.length; i < len; i++) {
            formatter.formatArgument(items[i], sb, false);
            if (i < len - 1) { // do not add space for last item
                sb.writeByte(ImapClientConstants.SPACE);
            }
        }
        sb.writeByte(ImapClientConstants.R_PAREN);

        sb.writeBytes(CRLF_B);
        return sb;
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.STATUS;
    }
}
