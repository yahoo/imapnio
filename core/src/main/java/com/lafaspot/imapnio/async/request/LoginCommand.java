package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

/**
 * This class defines imap login command request from client.
 */
public class LoginCommand extends ImapRequestAdapter {

    /** Literal for Login and space. */
    private static final String LOGIN_SPACE = "LOGIN ";

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
    public String getLogLine() {
        return new StringBuilder(LOGIN_SPACE).append(username).toString();
    }
}
