package com.yahoo.imapnio.async.request;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;

/**
 * This class defines an Imap command sent from client.
 */
public interface ImapRequest {
    /**
     * @return true if the command line data is sensitive; false otherwise
     */
    boolean isCommandLineDataSensitive();

    /**
     * Builds the command line in bytes - the line to be sent over wire.
     *
     * @return command line in binary form
     * @throws ImapAsyncClientException when encountering an error in building terminate command line
     */
    @Nonnull
    ByteBuf getCommandLineBytes() throws ImapAsyncClientException;

    /**
     * Builds the command line for this command - the line to be sent over wire.
     *
     * @return command line
     * @throws ImapAsyncClientException when encountering an error in building terminate command line
     */
    @Nonnull
    String getCommandLine() throws ImapAsyncClientException;

    /**
     * @return IMAP command type
     */
    @Nullable
    ImapCommandType getCommandType();

    /**
     * @return log data appropriate for the command
     */
    @Nullable
    String getDebugData();

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
    ByteBuf getNextCommandLineAfterContinuation(@Nonnull IMAPResponse serverResponse) throws ImapAsyncClientException;

    /**
     * Builds the next command line after server challenge.
     *
     * @throws ImapAsyncClientException when encountering an error in building terminate command line
     * @return command line
     */
    @Nullable
    ByteBuf getTerminateCommandLine() throws ImapAsyncClientException;

    /**
     * Avoids loitering.
     */
    @Nullable
    void cleanup();
}
