package com.yahoo.imapnio.async.request;

/**
 * This class defines imap capability command request from client.
 */
public class CapaCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String CAPABILITY = "CAPABILITY";

    /**
     * Initializes a {@link CapaCommand}.
     */
    public CapaCommand() {
        super(CAPABILITY);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.CAPABILITY;
    }
}
