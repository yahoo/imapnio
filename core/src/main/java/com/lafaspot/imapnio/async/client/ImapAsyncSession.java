package com.lafaspot.imapnio.async.client;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.mail.search.SearchException;

import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;
import com.lafaspot.imapnio.async.request.ImapRequest;
import com.lafaspot.imapnio.async.response.ImapAsyncResponse;

/**
 * A class that defines the behavior of Asynchronous imap session.
 */
public interface ImapAsyncSession {

    /**
     * Sends a imap command to the server.
     *
     * @param <T> the data type for retruning in getNextCommandLineAfterContinuation call
     *
     * @param command the command request.
     * @return the future object for this command
     * @throws ImapAsyncClientException on failure
     * @throws IOException when encountering IO exception
     * @throws SearchException when a search expression cannot be handled, conformed to RFC3501 standard
     */
    <T> ImapFuture<ImapAsyncResponse> execute(ImapRequest<T> command) throws ImapAsyncClientException, IOException, SearchException;

    /**
     * Terminates the currenct running command.
     *
     * @param command the command request.
     * @return the future object for this command
     * @throws ImapAsyncClientException on failure
     */
    ImapFuture<ImapAsyncResponse> terminateCommand(@Nonnull ImapRequest command) throws ImapAsyncClientException;

    /**
     * Closes/disconnects this session.
     *
     * @return a future when it is completed. True means successful, otherwise failure.
     */
    ImapFuture<Boolean> close();

}
