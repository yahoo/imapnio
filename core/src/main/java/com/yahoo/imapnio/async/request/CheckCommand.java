package com.yahoo.imapnio.async.request;

/**
 * This class defines imap check command request from client.
 */
public class CheckCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String CHECK = "CHECK";

    /**
     * Initializes the {@link CheckCommand}.
     */
    public CheckCommand() {
        super(CHECK);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.CHECK;
    }
}
