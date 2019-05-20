package com.lafaspot.imapnio.async.request;

/**
 * This class defines imap expunge command request from client.
 */
public class ExpungeCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String EXPUNGE = "EXPUNGE";

    /**
     * Initializes the @{code ExpungeCommand}.
     */
    public ExpungeCommand() {
        super(EXPUNGE);
    }
}
