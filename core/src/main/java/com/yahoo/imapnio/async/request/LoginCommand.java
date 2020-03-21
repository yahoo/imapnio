package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines IMAP login command request from client.
 */
public class LoginCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Literal for Login and space. */
    private static final String LOGIN_SP = "LOGIN ";

    /** Byte array for LOGIN. */
    private static final byte[] LOGIN_SP_B = LOGIN_SP.getBytes(StandardCharsets.US_ASCII);

    /** Literal for logging data. */
    private static final String LOG_PREFIX = "LOGIN FOR USER:";

    /** User name. */
    private String username;

    /** User pass word. */
    private String dwp;

    /**
     * Initializes an {@link LoginCommand}. User name and pass given have to be ASCII.
     *
     * @param username the user name
     * @param dwp the secret
     */
    public LoginCommand(@Nonnull final String username, @Nonnull final String dwp) {
        this.username = username;
        this.dwp = dwp;
    }

    @Override
    public void cleanup() {
        this.username = null;
        this.dwp = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() throws ImapAsyncClientException {

        final ByteBuf sb = Unpooled.buffer(username.length() + dwp.length() + ImapClientConstants.PAD_LEN);
        sb.writeBytes(LOGIN_SP_B);

        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
        formatter.formatArgument(username, sb, false);
        sb.writeByte(ImapClientConstants.SPACE);

        formatter.formatArgument(dwp, sb, false);
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

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.LOGIN;
    }
}
