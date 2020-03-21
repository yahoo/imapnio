package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.IMAPResponse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap idle command request from client.
 */
public class IdleCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Command name. */
    private static final String IDLE = "IDLE";

    /** Byte array for IDLE. */
    private static final byte[] IDLE_B = IDLE.getBytes(StandardCharsets.US_ASCII);

    /** Literal for DONE. */
    private static final String DONE = "DONE";

    /** Byte array for DONE. */
    private static final byte[] DONE_B = DONE.getBytes(StandardCharsets.US_ASCII);

    /** Initial buffer length, enough for the word IDLE or DONE with some room to grow. */
    private static final int LINE_LEN = 20;

    /** ConcurrentLinkedQueue shared from caller and {@code ImapAsyncSession}. */
    private ConcurrentLinkedQueue<IMAPResponse> serverStreamingResponses;

    /**
     * Initializes a {@link IdleCommand}.
     *
     * @param serverStreamingResponses server streaming responses will be placed in this parameter
     */
    public IdleCommand(@Nonnull final ConcurrentLinkedQueue<IMAPResponse> serverStreamingResponses) {
        this.serverStreamingResponses = serverStreamingResponses;
    }

    @Override
    public void cleanup() {
        this.serverStreamingResponses = null;
    }

    @Override
    public ConcurrentLinkedQueue<IMAPResponse> getStreamingResponsesQueue() {
        return serverStreamingResponses;
    }

    @Override
    public boolean isCommandLineDataSensitive() {
        return false;
    }

    @Override
    public String getDebugData() {
        return null;
    }

    @Override
    public ByteBuf getCommandLineBytes() {
        final ByteBuf buf = Unpooled.buffer(LINE_LEN);
        buf.writeBytes(IDLE_B);
        buf.writeBytes(CRLF_B);
        return buf;
    }

    @Override
    public ByteBuf getNextCommandLineAfterContinuation(@Nonnull final IMAPResponse serverResponse) {
        return null;
    }

    @Override
    public ByteBuf getTerminateCommandLine() {
        final ByteBuf buf = Unpooled.buffer(LINE_LEN);
        buf.writeBytes(DONE_B);
        buf.writeBytes(CRLF_B);
        return buf;
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.IDLE;
    }
}
