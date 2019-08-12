package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;

import org.apache.commons.codec.binary.Base64;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.data.Capability;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap authenticate xoauth2 command request from client.
 */
public class AuthXoauth2Command extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Command operator. */
    private static final String AUTH_XOAUTH2 = "AUTHENTICATE XOAUTH2";

    /** Byte array for AUTH XOAUTH2. */
    private static final byte[] AUTH_XOAUTH2_B = AUTH_XOAUTH2.getBytes(StandardCharsets.US_ASCII);

    /** AUTH_XOAUTH2 length. */
    private static final int AUTH_XOAUTH2_LEN = AUTH_XOAUTH2.length();

    /** Literal for logging data. */
    private static final String LOG_PREFIX = "AUTHENTICATE XOAUTH2 FOR USER:";

    /** Literal for user=. */
    private static final String USER = "user=";

    /** Literal for auth==Bearer. */
    private static final String AUTH_BEARER = "auth=Bearer ";

    /** Extra length for string. */
    private static final int EXTRA_LEN = 10;

    /** User name. */
    private String username;

    /** User token. */
    private String token;

    /** flag whether server allows one liner (Refer to RFC4959) instead of server challenge. */
    private boolean isSaslIREnabled;

    /**
     * Initializes an authenticate xoauth2 command.
     *
     * @param username the user name
     * @param token xoauth2 token
     * @param capa the capability obtained from server
     */
    public AuthXoauth2Command(@Nonnull final String username, @Nonnull final String token, @Nonnull final Capability capa) {
        this.username = username;
        this.token = token;
        this.isSaslIREnabled = capa.hasCapability(ImapClientConstants.SASL_IR);
    }

    @Override
    public void cleanup() {
        this.username = null;
        this.token = null;
    }

    /**
     * Builds the IR, aka client Initial Response (RFC4959). In this command, it is XOauth2 token format and encoded as base64.
     *
     * @return an encoded base64 XOauth2 format
     */
    private String buildClientResponse() {
        // Xoath2 format: "user=%s\001auth=Bearer %s\001\001";
        final int len = USER.length() + username.length() + token.length() + EXTRA_LEN;
        final StringBuilder sbOauth2 = new StringBuilder(len).append(USER).append(username).append(ImapClientConstants.SOH).append(AUTH_BEARER)
                .append(token).append(ImapClientConstants.SOH).append(ImapClientConstants.SOH);
        return Base64.encodeBase64String(sbOauth2.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public ByteBuf getCommandLineBytes() {
        if (isSaslIREnabled) { // server allows client response in one line
            final String clientResp = buildClientResponse();
            final ByteBuf sb = Unpooled.buffer(clientResp.length() + ImapClientConstants.PAD_LEN);
            sb.writeBytes(AUTH_XOAUTH2_B);
            sb.writeByte(ImapClientConstants.SPACE);
            sb.writeBytes(clientResp.getBytes(StandardCharsets.US_ASCII));
            sb.writeBytes(CRLF_B);
            return sb;
        }
        final int len = AUTH_XOAUTH2_LEN + ImapClientConstants.CRLFLEN;
        final ByteBuf buf = Unpooled.buffer(len);
        buf.writeBytes(AUTH_XOAUTH2_B);
        buf.writeBytes(CRLF_B);
        return buf;
    }

    @Override
    public boolean isCommandLineDataSensitive() {
        return true;
    }

    @Override
    public String getDebugData() {
        return new StringBuilder(LOG_PREFIX).append(username).toString();
    }

    @Override
    public ConcurrentLinkedQueue<IMAPResponse> getStreamingResponsesQueue() {
        return null;
    }

    @Override
    public ByteBuf getNextCommandLineAfterContinuation(final IMAPResponse serverResponse) throws ImapAsyncClientException {
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
        return ImapCommandType.AUTH_XOAUTH2;
    }
}
