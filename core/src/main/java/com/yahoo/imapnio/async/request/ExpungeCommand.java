package com.yahoo.imapnio.async.request;

/**
 * This class defines imap expunge command request from client.
 */
public class ExpungeCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String EXPUNGE = "EXPUNGE";

    /**
     * Initializes the {@link ExpungeCommand}.
     */
    public ExpungeCommand() {
        super(EXPUNGE);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.EXPUNGE;
    }
}
