package com.yahoo.imapnio.async.client;

import javax.annotation.Nonnull;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.request.ImapRequest;
import com.yahoo.imapnio.async.response.ImapAsyncResponse;

/**
 * A class that defines the behavior of Asynchronous IMAP session.
 */
public interface ImapAsyncSession {
    /**
     * Flag to turn on or off debugging for this session.
     */
    public enum DebugMode {
        /** Debugging is off for this session. */
        DEBUG_OFF,
        /** Debugging is on for this session. */
        DEBUG_ON
    }

    /**
     * Starts the compression, assuming caller verified the support of compression capability in server.
     *
     * @param <T> the data type for returning in getNextCommandLineAfterContinuation call
     *
     * @return the future object for this command
     * @throws ImapAsyncClientException on failure
     */
    <T> ImapFuture<ImapAsyncResponse> startCompression() throws ImapAsyncClientException;

    /**
     * Turns on or off the debugging.
     *
     * @param debugMode the debugging mode
     */
    void setDebugMode(DebugMode debugMode);

    /**
     * Sends a IMAP command to the server.
     *
     * @param <T> the data type for returning in getNextCommandLineAfterContinuation call
     *
     * @param command the command request.
     * @return the future object for this command
     * @throws ImapAsyncClientException on failure
     */
    <T> ImapFuture<ImapAsyncResponse> execute(ImapRequest command) throws ImapAsyncClientException;

    /**
     * Terminates the current running command.
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
