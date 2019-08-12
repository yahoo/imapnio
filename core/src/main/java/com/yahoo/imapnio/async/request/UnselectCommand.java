package com.yahoo.imapnio.async.request;

/**
 * This class defines imap unselect command request from client.
 */
public class UnselectCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String UNSELECT = "UNSELECT";

    /**
     * Initializes the @{code UnselectCommand}.
     */
    public UnselectCommand() {
        super(UNSELECT);
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.UNSELECT;
    }
}
