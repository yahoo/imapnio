package com.lafaspot.imapnio.async.request;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * This class defines imap idle command request from client.
 */
public class IdleCommand implements ImapRequest<String> {

    /** Command name. */
    private static final String IDLE = "IDLE";

    /** Literal for DONE. */
    private static final String DONE = "DONE";

    /** Literal for DONE. */
    private static final int LINE_LEN = 20;

    /** ConcurrentLinkedQueue shared from caller and @{code ImapAsyncSession}. */
    private ConcurrentLinkedQueue<IMAPResponse> serverStreamingResponses;

    /**
     * Initializes a @{code IdleCommand}.
     *
     * @param serverStreamingResponses server streaming responses will be placed in this parameter
     */
    public IdleCommand(@Nonnull final ConcurrentLinkedQueue<IMAPResponse> serverStreamingResponses) {
        this.serverStreamingResponses = serverStreamingResponses;
    }

    @Override
    public void cleanup() {
        this.serverStreamingResponses = null;
    }

    @Override
    public ConcurrentLinkedQueue<IMAPResponse> getStreamingResponsesQueue() {
        return serverStreamingResponses;
    }

    @Override
    public String getLogLine() {
        return getCommandLine();
    }

    @Override
    public String getCommandLine() {
        return new StringBuilder(LINE_LEN).append(IDLE).append(ImapClientConstants.CRLF).toString();
    }

    @Override
    public String getNextCommandLineAfterContinuation(@Nonnull final IMAPResponse serverResponse) {
        return null;
    }

    @Override
    public String getTerminateCommandLine() {
        return new StringBuilder(LINE_LEN).append(DONE).append(ImapClientConstants.CRLF).toString();
    }

}
