package com.yahoo.imapnio.async.request;

/**
 * This class defines imap noop command request from client.
 */
public class NoopCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String NOOP = "NOOP";

    /**
     * Initializes the {@link NoopCommand}.
     */
    public NoopCommand() {
        super(NOOP);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.NOOP;
    }
}
