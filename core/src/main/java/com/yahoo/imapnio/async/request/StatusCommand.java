package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;

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

        // ex: STATUS "test1" (UIDNEXT MESSAGES UIDVALIDITY RECENT)
        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
        final ImapCommandLineBuilder builder = new ImapCommandLineBuilder(LiteralSupport.DISABLE);
        builder.raw(STATUS_SP_B)
                .astring(BASE64MailboxEncoder.encode(folderName), false, "folder name") // already base64 encoded, so no literal is needed
                .raw((byte) ImapClientConstants.SPACE)
                .raw((byte) ImapClientConstants.L_PAREN);

        for (int i = 0, len = items.length; i < len; i++) {
            // status-att is an atom, so atom-specials keeps a space or parenthesis out of it
            builder.astring(formatter.validateAtom(items[i], "status item"), false, "status item");
            if (i < len - 1) { // do not add space for last item
                builder.raw((byte) ImapClientConstants.SPACE);
            }
        }

        return builder.raw((byte) ImapClientConstants.R_PAREN).finishSingle();
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.STATUS;
    }
}
