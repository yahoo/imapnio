package com.lafaspot.imapnio.async.request;

/**
 * This class defines imap capability command request from client.
 */
public class CapaCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String CAPABILITY = "CAPABILITY";

    /**
     * Initializes a {@code CapaCommand}.
     */
    public CapaCommand() {
        super(CAPABILITY);
    }
}
