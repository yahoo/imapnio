package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;

/**
 * This class defines imap select command request from client.
 */
abstract class AbstractQueryFoldersCommand extends ImapRequestAdapter {

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

        // both are already base64 encoded, so neither can need a literal
        return new ImapCommandLineBuilder(LiteralSupport.DISABLE)
                .raw(op.getBytes(StandardCharsets.US_ASCII))
                .raw((byte) ImapClientConstants.SPACE)
                .astring(ref64, false, "reference name")
                .raw((byte) ImapClientConstants.SPACE)
                .astring(pat64, false, "pattern")
                .finishSingle();
    }
}
