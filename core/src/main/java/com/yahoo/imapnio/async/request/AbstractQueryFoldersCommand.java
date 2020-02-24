package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap select command request from client.
 */
abstract class AbstractQueryFoldersCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** The Command. */
    private String op;

    /** reference name. */
    private String ref;

    /** search pattern. */
    private String pattern;

    /**
     * Initializes with command name, reference name, and pattern.
     *
     * @param op command/operator name, for ex, "LIST"
     * @param ref the reference string
     * @param pattern folder name with possible wildcards, see RFC3501 list command for detail.
     */
    AbstractQueryFoldersCommand(@Nonnull final String op, @Nonnull final String ref, @Nonnull final String pattern) {
        this.op = op;
        this.ref = ref;
        this.pattern = pattern;
    }

    @Override
    public void cleanup() {
        this.op = null;
        this.ref = null;
        this.pattern = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() throws ImapAsyncClientException {
        // Ex:LIST /usr/staff/jones ""

        // encode the arguments as per RFC2060
        final String ref64 = BASE64MailboxEncoder.encode(ref);
        final String pat64 = BASE64MailboxEncoder.encode(pattern);

        final int len = 2 * ref64.length() + 2 * pat64.length() + ImapClientConstants.PAD_LEN;
        final ByteBuf sb = Unpooled.buffer(len);
        sb.writeBytes(op.getBytes(StandardCharsets.US_ASCII));
        sb.writeByte(ImapClientConstants.SPACE);

        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
        formatter.formatArgument(ref64, sb, false); // already base64 encoded so can be formatted and write to sb
        sb.writeByte(ImapClientConstants.SPACE);

        formatter.formatArgument(pat64, sb, false);
        sb.writeBytes(CRLF_B); // already base64 encoded so can be formatted and write to sb

        return sb;
    }
}
