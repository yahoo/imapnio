package com.lafaspot.imapnio.async.request;

/**
 * This class defines imap close command request from client.
 */
public class CloseCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String CLOSE = "CLOSE";

    /**
     * Initializes the @{code CloseCommand}.
     */
    public CloseCommand() {
        super(CLOSE);
    }
}
