package com.lafaspot.imapnio.async.request;

/**
 * This class defines imap noop command request from client.
 */
public class NoopCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String NOOP = "NOOP";

    /**
     * Initializes the @{code NoopCommand}.
     */
    public NoopCommand() {
        super(NOOP);
    }
}
