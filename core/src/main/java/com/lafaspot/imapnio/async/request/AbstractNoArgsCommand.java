package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

/**
 * This class defines an Imap command that has no arguments sent from client.
 */
public abstract class AbstractNoArgsCommand extends ImapRequestAdapter {

    /** The Command. */
    private String op;

    /**
     * Initializes an IMAP command that has no arguments.
     *
     * @param op imap command string. For example, "NOOP"
     */
    protected AbstractNoArgsCommand(@Nonnull final String op) {
        super();
        this.op = op;
    }

    @Override
    public void cleanup() {
        this.op = null;
    }

    @Override
    public String getCommandLine() {
        final int len = op.length() + ImapClientConstants.CRLFLEN;
        return new StringBuilder(len).append(op).append(ImapClientConstants.CRLF).toString();
    }

}
