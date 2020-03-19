package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.data.Capability;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap authenticate command request from client.
 */
public abstract class AbstractAuthCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Byte buffer length for cancel statement. */
    private static final int CANCEL_LEN = 3;

    /** Byte buffer length for authenticate command, enough for the supported ones, oauth bear, plain, and xoauth2. */
    private static final int COMMAND_LEN = 50;

    /** Flag whether server allows one liner (Refer to RFC4959) instead of server challenge. */
    private boolean isSaslIREnabled;

    /** Flag whether the client response is sent already. */
    private boolean isClientResponseSent;

    /** Flag whether the data just sent is sensitive or not. */
    private boolean isDataSensitive;

    /**
     * Initializes an abstract authenticate command.
     *
     * @param capa the capability obtained from server
     */
    public AbstractAuthCommand(@Nonnull final Capability capa) {
        this.isSaslIREnabled = capa.hasCapability(ImapClientConstants.SASL_IR);
        this.isClientResponseSent = false;
        this.isDataSensitive = true;
    }

    /**
     * Builds the command (for example, "AUTHENTICATE XOAUTH2") and populate to the given {@link ByteBuf} instance.
     *
     * @param buf the {@link ByteBuf} instance to populate to
     */
    abstract void buildCommand(@Nonnull final ByteBuf buf);

    /**
     * Builds the IR, aka client Initial Response (RFC4959) and populates to the given {@link ByteBuf} instance.
     *
     * @return a string as a client initial response
     */
    abstract String buildClientResponse();

    @Override
    public ByteBuf getCommandLineBytes() {
        if (isSaslIREnabled) { // server allows client response in one line
            this.isDataSensitive = true; // containing sensitive data
            final String clientResp = buildClientResponse();

            // SASL-IR, rfc4959. "AUTHENTICATE" SP auth-type [SP (base64 / "=")] *(CRLF base64) ex: AUTHENTICATE XOAUTH2 [base64 response]
            final ByteBuf sb = Unpooled.buffer(clientResp.length() + ImapClientConstants.PAD_LEN);
            buildCommand(sb); // ex: AUTHENTICATE XOAUTH2
            sb.writeByte(ImapClientConstants.SPACE);
            sb.writeBytes(clientResp.getBytes(StandardCharsets.US_ASCII)); // client responses
            sb.writeBytes(CRLF_B);
            this.isClientResponseSent = true; // setting to true to indicate client response is sent
            return sb;
        }

        // SASL-IR is not supported, just send command without client response
        this.isDataSensitive = false;
        final ByteBuf buf = Unpooled.buffer(COMMAND_LEN);
        buildCommand(buf);
        buf.writeBytes(CRLF_B);
        return buf;
    }

    @Override
    public boolean isCommandLineDataSensitive() {
        return isDataSensitive;
    }

    @Override
    public ConcurrentLinkedQueue<IMAPResponse> getStreamingResponsesQueue() {
        return null;
    }

    @Override
    public ByteBuf getNextCommandLineAfterContinuation(final IMAPResponse serverResponse) {
        if (isClientResponseSent) { // when server sends "+ [base64 encoded error response]" after client response is sent, we send cancel
            this.isDataSensitive = false;
            final ByteBuf buf = Unpooled.buffer(CANCEL_LEN);
            buf.writeByte(ImapClientConstants.CANCEL_B);
            buf.writeBytes(CRLF_B);
            return buf;
        }

        // client response is not sent yet, sending it now
        this.isDataSensitive = true;
        final String clientResp = buildClientResponse();
        final ByteBuf buf = Unpooled.buffer(clientResp.length() + ImapClientConstants.CRLFLEN);
        buf.writeBytes(clientResp.getBytes(StandardCharsets.US_ASCII));
        buf.writeBytes(CRLF_B);
        isClientResponseSent = true; // setting to true to indicate client response is sent
        return buf;
    }

    @Override
    public ByteBuf getTerminateCommandLine() throws ImapAsyncClientException {
        throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.AUTHENTICATE;
    }
}
