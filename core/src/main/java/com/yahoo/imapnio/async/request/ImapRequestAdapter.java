package com.yahoo.imapnio.async.request;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

/**
 * This class is an adapter for commands with no continuation request or terminal request.
 */
public abstract class ImapRequestAdapter implements ImapRequest<String> {

    @Override
    public boolean isCommandLineDataSensitive() {
        return false;
    }

    @Override
    public String getDebugData() {
        return null;
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
