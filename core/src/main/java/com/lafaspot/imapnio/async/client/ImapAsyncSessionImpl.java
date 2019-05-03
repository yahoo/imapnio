package com.lafaspot.imapnio.async.client;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.mail.search.SearchException;

import org.slf4j.Logger;

import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;
import com.lafaspot.imapnio.async.exception.ImapAsyncClientException.FailureType;
import com.lafaspot.imapnio.async.request.IdleCommand;
import com.lafaspot.imapnio.async.request.ImapClientConstants;
import com.lafaspot.imapnio.async.request.ImapRequest;
import com.lafaspot.imapnio.async.response.ImapAsyncResponse;
import com.sun.mail.imap.protocol.IMAPResponse;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * This class establishes a session between imap server and sends command to server with async future.
 */
public class ImapAsyncSessionImpl implements ImapAsyncSession, ImapCommandChannelEventProcessor, ChannelFutureListener {

    /** Debug log record for server, first {} is sessionId, 2nd {} is for server message. */
    private static final String SERVER_LOG_REC = "[{}] S:{}";

    /** Debug log record for client, first {} is sessionId, 2nd {} is for client message. */
    private static final String CLIENT_LOG_REC = "[{}] C:{}";

    /** Debug log record for this class, due to channel or client input error, first {} is sessionId, 2nd {} is for message. */
    private static final String INTERNAL_LOG_REC = "[{}] I:{}";

    /** Debug message for donoting client error. */
    private static final String CLIENT_ERROR = " Client Error.";

    /** Tag prefix. */
    private static final char A = 'a';

    /** The Netty channel object. */
    private AtomicReference<Channel> channelRef = new AtomicReference<Channel>();

    /** Session Id. */
    private int sessionId;

    /** Producer queue. */
    private ConcurrentLinkedQueue<ImapCommandEntry> requestsQueue;

    /** Logger. */
    private Logger logger;

    /** Sequence number for tag. */
    private AtomicLong tagSequence;

    /**
     * This class handles and manages response from server and determines whether the job for this request is done. When the request is done, it sets
     * the future to done and returns the appropriate status to caller via handleResponse method.
     *
     * @param <T> the data type for next command after continuation.
     */
    private class ImapCommandEntry<T> {

        /** An Imap command. */
        @Nonnull
        private final ImapRequest<T> cmd;

        /** List of response lines. */
        @Nonnull
        private final ConcurrentLinkedQueue<IMAPResponse> responses;

        /** ImapCommandFuture. */
        @Nonnull
        private final ImapFuture<ImapAsyncResponse> future;

        /**
         * Initializes a newly created {@code ImapCommandJob} object so that it can handle the command responses and determine whether the request is
         * done.
         *
         * @param cmd ImapRequest instance
         * @param future ImapFuture instance
         */
        ImapCommandEntry(@Nonnull final ImapRequest<T> cmd, @Nonnull final ImapFuture<ImapAsyncResponse> future) {
            this.cmd = cmd;
            this.responses = (cmd.getStreamingResponsesQueue() != null) ? cmd.getStreamingResponsesQueue()
                    : new ConcurrentLinkedQueue<IMAPResponse>();
            this.future = future;
        }

        /**
         * @return the responses list
         */
        public Collection<IMAPResponse> getResponses() {
            return responses;
        }

        /**
         * @return the future for the imap command
         */
        public ImapFuture<ImapAsyncResponse> getFuture() {
            return future;
        }

        /**
         * @return the imap command
         */
        public ImapRequest<T> getRequest() {
            return cmd;
        }
    }

    /**
     * Initializes an imap session that supports async operations.
     *
     * @param channel Channel object established for this session
     * @param logger Logger object
     * @param sessionId the session id
     * @param pipeline the ChannelPipeline object
     */
    public ImapAsyncSessionImpl(@Nonnull final Channel channel, @Nonnull final Logger logger, final int sessionId, final ChannelPipeline pipeline) {
        this.channelRef.set(channel);
        this.logger = logger;
        this.sessionId = sessionId;
        this.requestsQueue = new ConcurrentLinkedQueue<ImapCommandEntry>();
        this.tagSequence = new AtomicLong(0);
        pipeline.addLast(ImapClientCommandRespHandler.HANDLER_NAME, new ImapClientCommandRespHandler(this));
    }

    /**
     * Generates a new tag.
     *
     * @return the new tag that was not used
     */
    private String getNextTag() {
        return new StringBuilder().append(A).append(tagSequence.incrementAndGet()).toString();
    }

    @Override
    public ImapFuture<ImapAsyncResponse> execute(@Nonnull final ImapRequest command) throws ImapAsyncClientException, IOException, SearchException {
        if (!requestsQueue.isEmpty()) {
            throw new ImapAsyncClientException(FailureType.COMMAND_NOT_ALLOWED);
        }

        final ImapFuture<ImapAsyncResponse> cmdFuture = new ImapFuture<ImapAsyncResponse>();
        requestsQueue.add(new ImapCommandEntry(command, cmdFuture));

        final String tag = getNextTag();
        final StringBuilder sb = new StringBuilder(tag).append(ImapClientConstants.SPACE).append(command.getCommandLine());
        if (logger.isDebugEnabled()) {
            logger.debug(CLIENT_LOG_REC, sessionId, new StringBuilder(tag).append(ImapClientConstants.SPACE).append(command.getLogLine()).toString());
        }
        sendRequest(sb.toString());

        return cmdFuture;
    }

    /**
     * Sends the given request to server when being called.
     *
     * @param request the message of the request
     * @throws ImapAsyncClientException when channel is closed
     */
    private void sendRequest(@Nonnull final Object request) throws ImapAsyncClientException {
        final Channel channel = channelRef.get();
        if (channel == null || !channel.isActive()) {
            throw new ImapAsyncClientException(FailureType.OPERATION_PROHIBITED_ON_CLOSED_CHANNEL);
        }

        // ChannelPromise is the suggested ChannelFuture that allows caller to setup listener before the action is made
        // this is useful for light-speed operation.
        final ChannelPromise writeFuture = channel.newPromise();
        writeFuture.addListener(this); // "this" listens to write future done in operationComplete() to handle exception in writing.
        channel.writeAndFlush(request, writeFuture);
    }

    @Override
    public ImapFuture<ImapAsyncResponse> terminateCommand(@Nonnull final ImapRequest command) throws ImapAsyncClientException {
        if (requestsQueue.isEmpty()) {
            throw new ImapAsyncClientException(FailureType.COMMAND_NOT_ALLOWED);
        }

        final ImapCommandEntry entry = requestsQueue.peek();
        if (logger.isDebugEnabled()) {
            logger.debug(CLIENT_LOG_REC, sessionId, command.getLogLine());
        }
        sendRequest(entry.getRequest().getTerminateCommandLine());
        return entry.getFuture();
    }

    /**
     * Listens to write to server complete future.
     * @param future ChannelFuture instance to check whether the future has completed successfully
     */
    @Override
    public void operationComplete(final ChannelFuture future) {
        if (!future.isSuccess()) { // failed to write to server
            handleChannelException(new ImapAsyncClientException(FailureType.WRITE_TO_SERVER_FAILED, future.cause()));
        }
    }

    @Override
    public void handleChannelClosed() {
        if (channelRef == null) {
            return; // cleanup() has been called, leave
        }

        if (logger.isDebugEnabled()) {
            logger.debug(INTERNAL_LOG_REC, sessionId, "Channel is disconnected.");
        }
        // set the future done if there is any
        requestDoneWithException(new ImapAsyncClientException(FailureType.CHANNEL_DISCONNECTED));
        cleanup();
    }

    /**
     * Cleans up all member variables.
     */
    private void cleanup() {
        channelRef.set(null);
        channelRef = null;
        requestsQueue.clear();
        requestsQueue = null;
        logger = null;
        tagSequence = null;
    }

    /**
     * Removes the first entry in the queue and calls ImapRequest.cleanup.
     *
     * @return the removed entry, returns null if queue is empty
     */
    private ImapCommandEntry removeFirstEntry() {
        if (requestsQueue.isEmpty()) {
            return null;
        }

        final ImapCommandEntry entry = requestsQueue.poll();
        // clean up the command since it is done regardless success or fail
        entry.getRequest().cleanup();
        return entry;
    }

    /**
     * @return the current in-progress request without removing it
     */
    private ImapCommandEntry getFirstEntry() {
        return requestsQueue.isEmpty() ? null : requestsQueue.peek();
    }

    /**
     * Sets the future done when command is executed unsuccessfully.
     *
     * @param cause the cause of why the operation fails
     */
    private void requestDoneWithException(@Nonnull final ImapAsyncClientException cause) {
        final ImapCommandEntry entry = removeFirstEntry();
        if (entry == null) {
            return;
        }
        entry.getFuture().done(cause);
    }

    @Override
    public void handleChannelException(@Nonnull final Throwable cause) {
        requestDoneWithException(new ImapAsyncClientException(FailureType.CHANNEL_EXCEPTION, cause));
    }

    @Override
    public void handleIdleEvent(@Nonnull final IdleStateEvent idleEvent) {
        final ImapCommandEntry curEntry = getFirstEntry();
        // no command or idleCommand sent, server is allowed to be silent, note we only allow READ_IDLE
        if (curEntry == null || curEntry.getRequest() instanceof IdleCommand) {
            return;
        }

        // error out for any other commands sent but server is not responding
        requestDoneWithException(new ImapAsyncClientException(FailureType.CHANNEL_TIMEOUT));
    }

    @Override
    public <T> void handleChannelResponse(@Nonnull final IMAPResponse serverResponse) {
        final ImapCommandEntry curEntry = getFirstEntry();
        if (curEntry == null) {
            return;
        }

        final ImapRequest currentCmd = curEntry.getRequest();
        final Collection<IMAPResponse> responses = curEntry.getResponses();
        responses.add(serverResponse);

        // server sends continuation message (+) for next request
        if (serverResponse.isContinuation()) {
            try {
                final Object cmdAfterContinue = currentCmd.getNextCommandLineAfterContinuation(serverResponse);
                if (cmdAfterContinue == null) {
                    return; // no data from client after continuation, we leave, this is for Idle
                }
                sendRequest(cmdAfterContinue);
            } catch (final ImapAsyncClientException | IndexOutOfBoundsException e) { // when encountering an error on building request from client
                if (logger.isDebugEnabled()) {
                    logger.debug(INTERNAL_LOG_REC, sessionId, CLIENT_ERROR, e);
                }
                requestDoneWithException(new ImapAsyncClientException(ImapAsyncClientException.FailureType.CHANNEL_EXCEPTION, e));
            }
            return;

        } else if (serverResponse.isTagged()) {
            // see rfc3501, page 63 for details, since we always give a tagged command, response completion should be the first tagged response
            final ImapAsyncResponse doneResponse = new ImapAsyncResponse(responses);
            removeFirstEntry();
            curEntry.getFuture().done(doneResponse);
            return;
        }

        // none-tagged server responses if reaching here
        if (logger.isDebugEnabled()) {
            logger.debug(SERVER_LOG_REC, sessionId, serverResponse.toString());
        }
    }

    @Override
    public ImapFuture<Boolean> close() {
        final ImapFuture<Boolean> closeFuture = new ImapFuture<Boolean>();
        final Channel channel = channelRef.get();
        if (channel == null || !channel.isActive()) {
            closeFuture.done(Boolean.TRUE);
        } else {
            final ChannelPromise channelPromise = channel.newPromise();
            final ImapChannelClosedListener channelClosedListener = new ImapChannelClosedListener(closeFuture);
            channelPromise.addListener(channelClosedListener);
            // this triggers handleChannelDisconnected() hence no need to handle queue here. We use close() instead of disconenct() to ensure it is
            // clearly a close action regardless TCP or UDP
            channel.close(channelPromise);
        }
        return closeFuture;
    }

    /**
     * Listener for channel close event done.
     */
    class ImapChannelClosedListener implements ChannelFutureListener {

        /** Future for the ImapAsyncSession client. */
        private ImapFuture<Boolean> imapSessionCloseFuture;

        /**
         * Initializes a channel close listner with ImapFuture instance.
         *
         * @param imapFuture the future for caller of @{link ImapAsyncSession} close method
         */
        ImapChannelClosedListener(final ImapFuture<Boolean> imapFuture) {
            this.imapSessionCloseFuture = imapFuture;
        }

        @Override
        public void operationComplete(@Nonnull final ChannelFuture future) {
            if (future.isSuccess()) {
                imapSessionCloseFuture.done(Boolean.TRUE);
            } else {
                imapSessionCloseFuture.done(new ImapAsyncClientException(FailureType.CLOSING_CONNECTION_FAILED, future.cause()));
            }
        }

    }
}
