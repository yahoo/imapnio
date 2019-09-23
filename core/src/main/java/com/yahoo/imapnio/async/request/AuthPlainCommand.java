package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.codec.binary.Base64;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.data.Capability;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap authenticate plain command request from client.
 */
public class AuthPlainCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Literal for Auth plain and space. */
    private static final String AUTH_PLAIN = "AUTHENTICATE PLAIN";

    /** Byte array for AUTH_PLAIN. */
    private static final byte[] AUTH_PLAIN_B = AUTH_PLAIN.getBytes(StandardCharsets.US_ASCII);

    /** Literal for logging data. */
    private static final String LOG_PREFIX = "AUTHENTICATE PLAIN FOR USER:";

    /** Literal for 10. */
    private static final int TEN = 10;

    /** Authorize id. */
    private String authId;

    /** User name. */
    private String username;

    /** User pass word. */
    private String dwp;

    /** flag whether server allows one liner (Refer to RFC4959) instead of server challenge. */
    private boolean isSaslIREnabled;

    /**
     * Initializes an authenticate plain command.
     *
     * @param username user name
     * @param dwp pass word
     * @param capa the capability obtained from server
     */
    public AuthPlainCommand(@Nonnull final String username, @Nonnull final String dwp, @Nonnull final Capability capa) {
        this(null, username, dwp, capa);
    }

    /**
     * Initializes an authenticate plain command.
     *
     * @param authId authorize-id in rfc2595
     * @param username user name
     * @param dwp pass word
     * @param capa the capability obtained from server
     */
    public AuthPlainCommand(@Nullable final String authId, @Nonnull final String username, @Nonnull final String dwp,
            @Nonnull final Capability capa) {
        this.authId = authId;
        this.username = username;
        this.dwp = dwp;
        this.isSaslIREnabled = capa.hasCapability(ImapClientConstants.SASL_IR);
    }

    @Override
    public void cleanup() {
        this.authId = null;
        this.username = null;
        this.dwp = null;
    }

    @Override
    public ConcurrentLinkedQueue<IMAPResponse> getStreamingResponsesQueue() {
        return null;
    }

    @Override
    public ByteBuf getCommandLineBytes() {
        // refer rfc2595, BNF is message = [authorize-id] NUL authenticate-id NUL password
        // [authorize-id] is optional if same as authenticate-id
        if (isSaslIREnabled) { // server allows client response in one line
            final String clientResp = buildClientResponse();
            final ByteBuf buf = Unpooled.buffer(clientResp.length() + ImapClientConstants.PAD_LEN);
            buf.writeBytes(AUTH_PLAIN_B);
            buf.writeByte(ImapClientConstants.SPACE);
            buf.writeBytes(clientResp.getBytes(StandardCharsets.US_ASCII));
            buf.writeBytes(CRLF_B);
            return buf;
        }
        final ByteBuf sb = Unpooled.buffer(ImapClientConstants.PAD_LEN);
        sb.writeBytes(AUTH_PLAIN_B);
        sb.writeBytes(CRLF_B);
        return sb;
    }

    @Override
    public boolean isCommandLineDataSensitive() {
        return true;
    }

    @Override
    public String getDebugData() {
        return new StringBuilder(LOG_PREFIX).append(username).toString();
    }

    /**
     * Builds the IR, aka client Initial Response (RFC4959). In this command, it is AUTH=PLAIN required format and encoded as base64.
     *
     * @return an encoded base64 AUTH=PLAIN format
     */
    private String buildClientResponse() {
        /// NOTE: char cannot be passed to StringBuilder constructor, since it becomes int as capacity
        // ex:bob\0bob\0munchkin
        final int authLen = (authId != null) ? authId.length() : 0;
        final int len = authLen + username.length() + dwp.length() + TEN;
        final StringBuilder sb = new StringBuilder(len);
        if (authId != null) {
            sb.append(authId);
        }
        final byte[] b = sb.append(ImapClientConstants.NULL).append(username).append(ImapClientConstants.NULL).append(dwp).toString()
                .getBytes(StandardCharsets.UTF_8);
        return Base64.encodeBase64String(b);
    }

    @Override
    public ByteBuf getNextCommandLineAfterContinuation(@Nonnull final IMAPResponse serverResponse) throws ImapAsyncClientException {
        if (isSaslIREnabled) { // should not reach here, since if SASL-IR enabled, server should not ask for next line
            throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
        }
        final String clientResp = buildClientResponse();
        final ByteBuf buf = Unpooled.buffer(clientResp.length() + ImapClientConstants.CRLFLEN);
        buf.writeBytes(clientResp.getBytes(StandardCharsets.US_ASCII));
        buf.writeBytes(CRLF_B);
        return buf;
    }

    @Override
    public ByteBuf getTerminateCommandLine() throws ImapAsyncClientException {
        throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.AUTHENTICATE;
    }
}
