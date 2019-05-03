package com.lafaspot.imapnio.async.request;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;
import javax.mail.search.SearchException;

import org.apache.avro.reflect.Nullable;

import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * This class defines an Imap command sent from client.
 *
 * @param <T> the data type for next command after continuation.
 */
public interface ImapRequest<T> {

    /**
     * Builds the command line for this command - the line to be sent over wire.
     *
     * @return command line
     * @throws SearchException when Search command expression does not conform to standard
     * @throws IOException for I/O errors
     * @throws ImapAsyncClientException when encountering an error in building terminate command line
     */
    @Nonnull
    String getCommandLine() throws SearchException, IOException, ImapAsyncClientException;

    /**
     * Builds the log line for this command - the line to be logged.
     *
     * @return log line
     */
    @Nonnull
    String getLogLine();

    /**
     * @return the queue for holding the server streaming responses
     */
    @Nullable
    ConcurrentLinkedQueue<IMAPResponse> getStreamingResponsesQueue();

    /**
     * Builds the next command line after server challenge.
     *
     * @param serverResponse the server response
     * @throws ImapAsyncClientException when building command line encounters an error
     * @return command line
     */
    @Nullable
    T getNextCommandLineAfterContinuation(@Nonnull IMAPResponse serverResponse) throws ImapAsyncClientException;

    /**
     * Builds the next command line after server challenge.
     *
     * @throws ImapAsyncClientException when encountering an error in building terminate command line
     * @return command line
     */
    @Nullable
    String getTerminateCommandLine() throws ImapAsyncClientException;

    /**
     * Avoids loitering.
     */
    @Nullable
    void cleanup();
}
