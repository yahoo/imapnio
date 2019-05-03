package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

/**
 * This class defines an Imap command that has no arguments sent from client.
 */
abstract class AbstractNoArgsCommand extends ImapRequestAdapter {

    /** The Command. */
    private String op;

    /**
     * Initalizes an IMAP command that has no arguments.
     *
     * @param op imap command string. For example, "NOOP"
     */
    AbstractNoArgsCommand(@Nonnull final String op) {
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
