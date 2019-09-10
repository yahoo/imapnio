package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines an Imap command that has no arguments sent from client.
 */
public abstract class AbstractNoArgsCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** The Command. */
    private String op;

    /**
     * Initializes an IMAP command that has no arguments.
     *
     * @param op imap command string. For example, "NOOP"
     */
    protected AbstractNoArgsCommand(@Nonnull final String op) {
        super();
        this.op = op;
    }

    @Override
    public void cleanup() {
        this.op = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() {
        final int len = op.length() + ImapClientConstants.CRLFLEN;
        final ByteBuf sb = Unpooled.buffer(len);
        sb.writeBytes(op.getBytes(StandardCharsets.US_ASCII));
        sb.writeBytes(CRLF_B);
        return sb;
    }

}
