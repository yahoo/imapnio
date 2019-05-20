package com.lafaspot.imapnio.async.request;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;
import javax.mail.search.SearchException;

import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;
import com.lafaspot.imapnio.async.exception.ImapAsyncClientException.FailureType;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * This class is an adapter for commands with no continuation request or terminal request.
 */
public abstract class ImapRequestAdapter implements ImapRequest<String> {

    @Override
    public String getLogLine() {
        try {
            return getCommandLine();
        } catch (final SearchException | IOException | ImapAsyncClientException e) {
            return new StringBuilder("unable to obtain log line due to exception:").append(e.getMessage()).toString();
        }
    }

    @Override
    public ConcurrentLinkedQueue<IMAPResponse> getStreamingResponsesQueue() {
        return null;
    }

    @Override
    public String getNextCommandLineAfterContinuation(@Nonnull final IMAPResponse serverResponse) throws ImapAsyncClientException {
        throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
    }

    @Override
    public String getTerminateCommandLine() throws ImapAsyncClientException {
        throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
    }
}
