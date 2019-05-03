package com.lafaspot.imapnio.async.request;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;

import org.apache.commons.codec.binary.Base64;

import com.lafaspot.imapnio.async.data.Capability;
import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;
import com.lafaspot.imapnio.async.exception.ImapAsyncClientException.FailureType;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * This class defines imap authenticate xoauth2 command request from client.
 */
public class AuthXoauth2Command implements ImapRequest<String> {

    /** Command operator. */
    private static final String AUTH_XOAUTH2 = "AUTHENTICATE XOAUTH2";

    /** AUTH_XOAUTH2 length. */
    private static final int AUTH_XOAUTH2_LEN = AUTH_XOAUTH2.length();

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
    public String getCommandLine() {
        if (isSaslIREnabled) { // server allows client response in one line
            final String clientResp = buildClientResponse();
            return new StringBuilder(clientResp.length() + ImapClientConstants.PAD_LEN).append(AUTH_XOAUTH2).append(ImapClientConstants.SPACE)
                    .append(clientResp).append(ImapClientConstants.CRLF).toString();
        }
        final int len = AUTH_XOAUTH2_LEN + ImapClientConstants.CRLFLEN;
        return new StringBuilder(len).append(AUTH_XOAUTH2).append(ImapClientConstants.CRLF).toString();
    }

    @Override
    public String getLogLine() {
        return new StringBuilder(AUTH_XOAUTH2).append(ImapClientConstants.SPACE).append(username).toString();
    }

    @Override
    public ConcurrentLinkedQueue<IMAPResponse> getStreamingResponsesQueue() {
        return null;
    }

    @Override
    public String getNextCommandLineAfterContinuation(final IMAPResponse serverResponse) throws ImapAsyncClientException {
        if (isSaslIREnabled) { // should not reach here, since if SASL-IR enabled, server should not ask for next line
            throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
        }
        final String clientResp = buildClientResponse();
        final int len = clientResp.length() + ImapClientConstants.CRLFLEN;
        return new StringBuilder(len).append(clientResp).append(ImapClientConstants.CRLF).toString();
    }

    @Override
    public String getTerminateCommandLine() throws ImapAsyncClientException {
        throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
    }
}
