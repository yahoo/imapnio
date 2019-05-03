package com.lafaspot.imapnio.async.request;

/**
 * This class defines imap logout command request from client.
 */
public class LogoutCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String LOGOUT = "LOGOUT";

    /**
     * Initializes the @{code LogoutCommand} command.
     */
    public LogoutCommand() {
        super(LOGOUT);
    }
}
