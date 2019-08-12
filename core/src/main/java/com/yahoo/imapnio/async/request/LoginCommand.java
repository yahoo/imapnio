package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

/**
 * This class defines IMAP login command request from client.
 */
public class LoginCommand extends ImapRequestAdapter {

    /** Literal for Login and space. */
    private static final String LOGIN_SPACE = "LOGIN ";

    /** Literal for logging data. */
    private static final String LOG_PREFIX = "LOGIN FOR USER:";

    /** User name. */
    private String username;

    /** User pass word. */
    private String dwp;

    /**
     * Initializes an @{code LoginCommand}. User name and pass given have to be ASCII.
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
    public String getCommandLine() {
        return new StringBuilder(LOGIN_SPACE).append(username).append(ImapClientConstants.SPACE).append(dwp).append(ImapClientConstants.CRLF)
                .toString();
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
    public ImapCommandType getCommandType() {
        return ImapCommandType.LOGIN;
    }
}
