package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.codec.binary.Base64;

import com.yahoo.imapnio.async.data.Capability;

import io.netty.buffer.ByteBuf;

/**
 * This class defines imap authenticate plain command request from client.
 */
public final class AuthPlainCommand extends AbstractAuthCommand {

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
        super(capa);
        this.authId = authId;
        this.username = username;
        this.dwp = dwp;
    }

    @Override
    public void cleanup() {
        this.authId = null;
        this.username = null;
        this.dwp = null;
    }

    @Override
    void buildCommand(@Nonnull final ByteBuf buf) {
        buf.writeBytes(AUTH_PLAIN_B);
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
    @Override
    String buildClientResponse() {
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
}
