package com.yahoo.imapnio.async.request;

/**
 * This class defines imap unselect command request from client.
 */
public class UnselectCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String UNSELECT = "UNSELECT";

    /**
     * Initializes the {@link UnselectCommand}.
     */
    public UnselectCommand() {
        super(UNSELECT);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.UNSELECT;
    }
}
