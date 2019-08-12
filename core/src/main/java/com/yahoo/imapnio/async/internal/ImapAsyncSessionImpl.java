package com.yahoo.imapnio.async.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.mail.search.SearchException;

import org.slf4j.Logger;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.client.ImapAsyncClient;
import com.yahoo.imapnio.async.client.ImapAsyncSession;
import com.yahoo.imapnio.async.client.ImapFuture;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;
import com.yahoo.imapnio.async.netty.ImapClientCommandRespHandler;
import com.yahoo.imapnio.async.netty.ImapCommandChannelEventProcessor;
import com.yahoo.imapnio.async.request.IdleCommand;
import com.yahoo.imapnio.async.request.ImapRequest;
import com.yahoo.imapnio.async.response.ImapAsyncResponse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.compression.JdkZlibDecoder;
import io.netty.handler.codec.compression.JdkZlibEncoder;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * This class establishes a session between imap server and sends command to server with async future.
 */
public class ImapAsyncSessionImpl implements ImapAsyncSession, ImapCommandChannelEventProcessor, ChannelFutureListener {

    /** Error record for the session, first {} is sessionId, exception will be printed too as stack. */
    private static final String SESSION_ERR_REC = "[{}]";

    /** Debug log record for server, first {} is sessionId, 2nd {} is for server message. */
    private static final String SERVER_LOG_REC = "[{}] S:{}";

    /** Debug log record for client, first {} is sessionId, 2nd {} is for client message. */
    private static final String CLIENT_LOG_REC = "[{}] C:{}";

    /** Space character. */
    static final char SPACE = ' ';

    /** Tag prefix. */
    private static final char A = 'a';

    /** Deflater handler name for enabling server compress. */
    private static final String ZLIB_DECODER = "DEFLATER";

    /** Inflater handler name for enabling server compress. */
    private static final String ZLIB_ENCODER = "INFLATER";

    /** The Netty channel object. */
    private AtomicReference<Channel> channelRef = new AtomicReference<Channel>();

    /** Session Id. */
    private int sessionId;

    /** Producer queue. */
    private ConcurrentLinkedQueue<ImapCommandEntry> requestsQueue;

    /** Logger. */
    private Logger logger;

    /** Debug mode. */
    private AtomicReference<DebugMode> debugModeRef = new AtomicReference<DebugMode>();

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
        private final ImapRequest cmd;

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
        ImapCommandEntry(@Nonnull final ImapRequest cmd, @Nonnull final ImapFuture<ImapAsyncResponse> future) {
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
        public ImapRequest getRequest() {
            return cmd;
        }
    }

    /**
     * Initializes an imap session that supports async operations.
     *
     * @param channel Channel object established for this session
     * @param logger Logger object
     * @param debugMode Flag for debugging
     * @param sessionId the session id
     * @param pipeline the ChannelPipeline object
     */
    public ImapAsyncSessionImpl(@Nonnull final Channel channel, @Nonnull final Logger logger, @Nonnull final DebugMode debugMode, final int sessionId,
            final ChannelPipeline pipeline) {
        this.channelRef.set(channel);
        this.logger = logger;
        this.debugModeRef.set(debugMode);
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

    /**
     * @return true if debugging is enabled either for the session or for all sessions
     */
    private boolean isDebugEnabled() {
        // when trace is enabled, log for all sessions
        // when debug is enabled && session debug is on, we print specific session
        return logger.isTraceEnabled() || debugModeRef.get() == DebugMode.DEBUG_ON;
    }

    @Override
    public void setDebugMode(@Nonnull final DebugMode newOption) {
        this.debugModeRef.set(newOption);
    }

    @Override
    public ImapFuture<ImapAsyncResponse> execute(@Nonnull final ImapRequest command) throws ImapAsyncClientException {
        if (!requestsQueue.isEmpty()) {
            throw new ImapAsyncClientException(FailureType.COMMAND_NOT_ALLOWED);
        }

        final ImapFuture<ImapAsyncResponse> cmdFuture = new ImapFuture<ImapAsyncResponse>();
        requestsQueue.add(new ImapCommandEntry(command, cmdFuture));

        final ByteBuf buf = Unpooled.buffer();
        final String tag = getNextTag();
        buf.writeBytes(tag.getBytes(StandardCharsets.US_ASCII));
        buf.writeByte(SPACE);
        buf.writeBytes(command.getCommandLineBytes());

        if (isDebugEnabled() && command.isCommandLineDataSensitive()) { // if we cannot log data sent over wire, ask command to provide log info
            logger.debug(CLIENT_LOG_REC, sessionId, command.getDebugData());
        }
        sendRequest(buf, command.isCommandLineDataSensitive());

        return cmdFuture;
    }

    @Override
    public <T> ImapFuture<ImapAsyncResponse> startCompression() throws ImapAsyncClientException, SearchException, IOException {
        final ImapFuture<ImapAsyncResponse> future = execute(new CompressCommand());
        return future;
    }

    /**
     * Sends the given request to server when being called.
     *
     * @param request the message of the request
     * @param isDataSensitve flag whether data is sensitive
     * @throws ImapAsyncClientException when channel is closed
     */
    private void sendRequest(@Nonnull final ByteBuf request, final boolean isDataSensitve) throws ImapAsyncClientException {
        if (isDebugEnabled() && !isDataSensitve) {
            logger.debug(CLIENT_LOG_REC, sessionId, request.toString(StandardCharsets.UTF_8));
        }
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
        sendRequest(entry.getRequest().getTerminateCommandLine(), command.isCommandLineDataSensitive());
        return entry.getFuture();
    }

    /**
     * Listens to write to server complete future.
     *
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
        debugModeRef.set(null);
        debugModeRef = null;
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

        // log at error level
        logger.error(SESSION_ERR_REC, sessionId, cause);
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

        if (isDebugEnabled()) { // logging all server responses when enabled
            logger.debug(SERVER_LOG_REC, sessionId, serverResponse.toString());
        }

        // server sends continuation message (+) for next request
        if (serverResponse.isContinuation()) {
            try {
                final ByteBuf cmdAfterContinue = currentCmd.getNextCommandLineAfterContinuation(serverResponse);
                if (cmdAfterContinue == null) {
                    return; // no data from client after continuation, we leave, this is for Idle
                }
                sendRequest(cmdAfterContinue, currentCmd.isCommandLineDataSensitive());
            } catch (final ImapAsyncClientException | RuntimeException e) { // when encountering an error on building request from client
                requestDoneWithException(new ImapAsyncClientException(ImapAsyncClientException.FailureType.CHANNEL_EXCEPTION, e));
            }
            return;

        } else if (serverResponse.isTagged()) {
            try {
                if (currentCmd instanceof CompressCommand && serverResponse.isOK()) {
                    final Channel ch = channelRef.get();
                    final ChannelPipeline pipeline = ch.pipeline();
                    final JdkZlibDecoder decoder = new JdkZlibDecoder(ZlibWrapper.NONE);
                    final JdkZlibEncoder encoder = new JdkZlibEncoder(ZlibWrapper.NONE, 5);
                    if (pipeline.get(ImapAsyncClient.SSL_HANDLER) == null) {
                        // no SSL handler, deflater/enflater has to be first
                        pipeline.addFirst(ZLIB_DECODER, decoder);
                        pipeline.addFirst(ZLIB_ENCODER, encoder);
                    } else {
                        pipeline.addAfter(ImapAsyncClient.SSL_HANDLER, ZLIB_DECODER, decoder);
                        pipeline.addAfter(ImapAsyncClient.SSL_HANDLER, ZLIB_ENCODER, encoder);
                    }
                }
                // see rfc3501, page 63 for details, since we always give a tagged command, response completion should be the first tagged response
                final ImapAsyncResponse doneResponse = new ImapAsyncResponse(responses);
                removeFirstEntry();
                curEntry.getFuture().done(doneResponse);
                return;
            } catch (final RuntimeException e) {
                requestDoneWithException(new ImapAsyncClientException(ImapAsyncClientException.FailureType.CHANNEL_EXCEPTION, e));
            }
        }

        // none-tagged server responses if reaching here
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
         * Initializes a channel close listener with ImapFuture instance.
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
