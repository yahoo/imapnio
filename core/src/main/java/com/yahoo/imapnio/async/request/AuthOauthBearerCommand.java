package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import org.apache.commons.codec.binary.Base64;

import com.yahoo.imapnio.async.data.Capability;

import io.netty.buffer.ByteBuf;

/**
 * This class defines imap authenticate OauthBearer command request from client.
 */
public final class AuthOauthBearerCommand extends AbstractAuthCommand {

    /** Command operator. */
    private static final String AUTH_OAUTHBEARER = "AUTHENTICATE OAUTHBEARER";

    /** Byte array for AUTH OAUTHBEARER. */
    private static final byte[] AUTH_OAUTHBEARER_B = AUTH_OAUTHBEARER.getBytes(StandardCharsets.US_ASCII);

    /** Literal for logging data. */
    private static final String LOG_PREFIX = "AUTHENTICATE OAUTHBEARER FOR USER:";

    /** Literal for user=. */
    private static final String N_A = "n,a=";

    /** Literal for auth==Bearer. */
    private static final String AUTH_BEARER = "auth=Bearer ";

    /** Extra length for port and a bunch of SOH. */
    private static final int EXTRA_LEN = 50;

    /** Comma literal. */
    private static final char COMMA = ',';

    /** Email Id. */
    private String emailId;

    /** Host name. */
    private String hostname;

    /** Port. */
    private int port;

    /** User token. */
    private String token;

    /**
     * Initializes an authenticate OauthBearer command.
     *
     * @param emailId the user name
     * @param hostname the host name
     * @param port the port
     * @param token xoauth2 token
     * @param capa the capability obtained from server
     */
    public AuthOauthBearerCommand(@Nonnull final String emailId, @Nonnull final String hostname, final int port, @Nonnull final String token,
            @Nonnull final Capability capa) {
        super(capa);
        this.emailId = emailId;
        this.hostname = hostname;
        this.port = port;
        this.token = token;
    }

    @Override
    public void cleanup() {
        this.emailId = null;
        this.hostname = null;
        this.token = null;
    }

    @Override
    void buildCommand(@Nonnull final ByteBuf buf) {
        buf.writeBytes(AUTH_OAUTHBEARER_B);
    }

    /**
     * Builds the IR, aka client Initial Response (RFC4959). In this command, it is Oauthbearer token format and encoded as base64.
     *
     * @return an encoded base64 Oauthbearer format
     */
    @Override
    String buildClientResponse() {
        // String format: n,a=user@example.com,^Ahost=server.example.com^Aport=993^Aauth=Bearer <oauthtoken>^A^A
        final int len = N_A.length() + emailId.length() + hostname.length() + token.length() + EXTRA_LEN;
        final StringBuilder sbOauth2 = new StringBuilder(len).append(N_A).append(emailId).append(COMMA).append(ImapClientConstants.SOH);
        sbOauth2.append("host=").append(hostname).append(ImapClientConstants.SOH).append("port=").append(port).append(ImapClientConstants.SOH);
        sbOauth2.append(AUTH_BEARER).append(token).append(ImapClientConstants.SOH).append(ImapClientConstants.SOH);
        return Base64.encodeBase64String(sbOauth2.toString().getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public String getDebugData() {
        return new StringBuilder(LOG_PREFIX).append(emailId).toString();
    }
}
