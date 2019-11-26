package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import org.apache.commons.codec.binary.Base64;

import com.yahoo.imapnio.async.data.Capability;

import io.netty.buffer.ByteBuf;

/**
 * This class defines imap authenticate xoauth2 command request from client.
 */
public final class AuthXoauth2Command extends AbstractAuthCommand {

    /** Command operator. */
    private static final String AUTH_XOAUTH2 = "AUTHENTICATE XOAUTH2";

    /** Byte array for AUTH XOAUTH2. */
    private static final byte[] AUTH_XOAUTH2_B = AUTH_XOAUTH2.getBytes(StandardCharsets.US_ASCII);

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

    /**
     * Initializes an authenticate xoauth2 command.
     *
     * @param username the user name
     * @param token xoauth2 token
     * @param capa the capability obtained from server
     */
    public AuthXoauth2Command(@Nonnull final String username, @Nonnull final String token, @Nonnull final Capability capa) {
        super(capa);
        this.username = username;
        this.token = token;
    }

    @Override
    public void cleanup() {
        this.username = null;
        this.token = null;
    }

    @Override
    void buildCommand(@Nonnull final ByteBuf buf) {
        buf.writeBytes(AUTH_XOAUTH2_B);
    }

    /**
     * Builds the IR, aka client Initial Response (RFC4959). In this command, it is XOauth2 token format and encoded as base64.
     *
     * @return an encoded base64 XOauth2 format
     */
    @Override
    String buildClientResponse() {
        // Xoath2 format: "user=%s\001auth=Bearer %s\001\001";
        final int len = USER.length() + username.length() + token.length() + EXTRA_LEN;
        final StringBuilder sbOauth2 = new StringBuilder(len).append(USER).append(username).append(ImapClientConstants.SOH).append(AUTH_BEARER)
                .append(token).append(ImapClientConstants.SOH).append(ImapClientConstants.SOH);
        return Base64.encodeBase64String(sbOauth2.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getDebugData() {
        return new StringBuilder(LOG_PREFIX).append(username).toString();
    }
}
