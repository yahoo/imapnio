package com.yahoo.imapnio.async.request;

/**
 * This class defines imap close command request from client.
 */
public class CloseCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String CLOSE = "CLOSE";

    /**
     * Initializes the {@link CloseCommand}.
     */
    public CloseCommand() {
        super(CLOSE);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.CLOSE;
    }
}
