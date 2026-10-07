package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

import io.netty.buffer.ByteBuf;

/**
 * This class defines IMAP login command request from client.
 *
 * <p>
 * A user name or secret carrying CR or LF is sent as a literal, since a quoted string cannot represent one. With a synchronizing literal the
 * command line is sent in more than one piece, each piece released by a command continuation request from the server, so this command keeps
 * the pieces and hands them over one at a time.
 * </p>
 */
public class LoginCommand extends ImapRequestAdapter {

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

    /** Which literal form the server accepts. */
    private LiteralSupport literalOpt;

    /** The command line pieces, populated when the command line is built. */
    private List<ByteBuf> segments;

    /** Index of the piece to send on the next command continuation request. */
    private int nextSegment;

    /**
     * Initializes an {@link LoginCommand}. User name and pass given have to be ASCII.
     *
     * @param username the user name
     * @param dwp the secret
     */
    public LoginCommand(@Nonnull final String username, @Nonnull final String dwp) {
        this(username, dwp, LiteralSupport.DISABLE);
    }

    /**
     * Initializes an {@link LoginCommand} with a literal form. User name and pass given have to be ASCII.
     *
     * @param username the user name
     * @param dwp the secret
     * @param literalOpt the literal form the server accepts, which decides whether a literal costs a round trip
     */
    public LoginCommand(@Nonnull final String username, @Nonnull final String dwp, @Nonnull final LiteralSupport literalOpt) {
        this.username = username;
        this.dwp = dwp;
        this.literalOpt = literalOpt;
    }

    @Override
    public void cleanup() {
        this.username = null;
        this.dwp = null;
        this.literalOpt = null;
        this.segments = null; // a piece may hold the secret, so it is not kept past the command
    }

    @Override
    public ByteBuf getCommandLineBytes() throws ImapAsyncClientException {
        segments = new ImapCommandLineBuilder(literalOpt)
                .raw(LOGIN_SP_B)
                .astring(username, false, "username")
                .raw((byte) ImapClientConstants.SPACE)
                .astring(dwp, false, "password")
                .finish();
        nextSegment = 1;
        return segments.get(0);
    }

    @Override
    public ByteBuf getNextCommandLineAfterContinuation(@Nonnull final IMAPResponse serverResponse) throws ImapAsyncClientException {
        if (segments == null || nextSegment >= segments.size()) {
            // the server asked for more than this command line has left to give
            throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
        }
        return segments.get(nextSegment++);
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
