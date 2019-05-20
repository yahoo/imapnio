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
 * This class defines imap authenticate plain command request from client.
 */
public class AuthPlainCommand implements ImapRequest<String> {

    /** Literal for Auth plan and space. */
    private static final String AUTH_PLAIN = "AUTHENTICATE PLAIN";

    /** Literal for 10. */
    private static final int TEN = 10;

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
        this.username = username;
        this.dwp = dwp;
        this.isSaslIREnabled = capa.hasCapability(ImapClientConstants.SASL_IR);
    }

    @Override
    public void cleanup() {
        this.username = null;
        this.dwp = null;
    }

    @Override
    public ConcurrentLinkedQueue<IMAPResponse> getStreamingResponsesQueue() {
        return null;
    }

    @Override
    public String getCommandLine() {
        if (isSaslIREnabled) { // server allows client response in one line
            return new StringBuilder(AUTH_PLAIN).append(ImapClientConstants.SPACE).append(buildClientResponse()).append(ImapClientConstants.CRLF)
                    .toString();
        }
        return new StringBuilder(AUTH_PLAIN).append(ImapClientConstants.CRLF).toString();
    }

    @Override
    public String getLogLine() {
        return new StringBuilder(AUTH_PLAIN).append(ImapClientConstants.SPACE).append(username).toString();
    }

    /**
     * Builds the IR, aka client Initial Response (RFC4959). In this command, it is AUTH=PLAIN required format and encoded as base64.
     *
     * @return an encoded base64 AUTH=PLAIN format
     */
    private String buildClientResponse() {
        /// NOTE: char cannot be passed to StringBuilder constructor, since it becomes int as capacity
        // ex:\0bob\0munchkin
        final int len = username.length() + dwp.length() + TEN;
        final byte[] b = new StringBuilder(len).append(ImapClientConstants.NULL).append(username).append(ImapClientConstants.NULL).append(dwp)
                .toString().getBytes(StandardCharsets.UTF_8);
        return Base64.encodeBase64String(b);
    }

    @Override
    public String getNextCommandLineAfterContinuation(@Nonnull final IMAPResponse serverResponse) throws ImapAsyncClientException {
        if (isSaslIREnabled) { // should not reach here, since if SASL-IR enabled, server should not ask for next line
            throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
        }
        final String clientResp = buildClientResponse();
        return new StringBuilder(clientResp.length() + ImapClientConstants.CRLFLEN).append(clientResp).append(ImapClientConstants.CRLF).toString();
    }

    @Override
    public String getTerminateCommandLine() throws ImapAsyncClientException {
        throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
    }
}
