package com.yahoo.imapnio.async.internal;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

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

    /** Label for command sent, used in exception message. */
    private static final String CMD_SENT = ",cmdSent:";

    /** Label for command type, used in exception message. */
    private static final String CMD_TYPE = ",cmdType:";

    /** Label for command type, used in exception message. */
    private static final String CMD_TAG = ",cmdTag:";

    /** Error record for the session, first {} is sessionId, 2nd user information. */
    private static final String SESSION_LOG_REC = "[{},{}] {}";

    /** Error record for the session, first {} is sessionId, 2nd user information. */
    private static final String SESSION_LOG_WITH_EXCEPTION = "[{},{}]";

    /** Debug log record for server, first {} is sessionId, 2nd user information, 3rd for server message. */
    private static final String SERVER_LOG_REC = "[{},{}] S:{}";

    /** Debug log record for client, first {} is sessionId, 2nd user information, 3rd for client message. */
    private static final String CLIENT_LOG_REC = "[{},{}] C:{}";

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
    private long sessionId;

    /** Instance that stores the client context, we will call toString() of it. */
    @Nonnull
    private Object sessionCtx;

    /** Clock instance. */
    private Clock clock;

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
    private static class ImapCommandEntry<T> {
        /**
         * State of the command in its life cycle.
         */
        public enum CommandState {
            /** Request (command line) is in preparation to be generated and sent, but not yet sent to server. */
            REQUEST_IN_PREPARATION,
            /** Request (command line) is confirmed sent to the the server. */
            REQUEST_SENT,
            /** Server done with responses for the given client request. Server is not obligated to send more responses per given request. */
            RESPONSES_DONE
        }

        /** An Imap command. */
        @Nonnull
        private final ImapRequest cmd;

        /** The state of command. */
        @Nonnull
        private CommandState state;

        /** List of response lines. */
        @Nonnull
        private final ConcurrentLinkedQueue<IMAPResponse> responses;

        /** ImapCommandFuture. */
        @Nonnull
        private final ImapFuture<ImapAsyncResponse> future;

        /** The tag for this command. */
        @Nonnull
        private final String tag;

        /** Time when request is sent to server. */
        private long requestSentTime;

        /**
         * Initializes a newly created {@link ImapCommandEntry} object so that it can handle the command responses and determine whether the request
         * is done.
         *
         * @param cmd ImapRequest instance
         * @param future ImapFuture instance
         * @param tag the tag associated with this command
         */
        ImapCommandEntry(@Nonnull final ImapRequest cmd, @Nonnull final ImapFuture<ImapAsyncResponse> future, @Nonnull final String tag) {
            this.cmd = cmd;
            this.state = CommandState.REQUEST_IN_PREPARATION;
            this.responses = (cmd.getStreamingResponsesQueue() != null) ? cmd.getStreamingResponsesQueue()
                    : new ConcurrentLinkedQueue<IMAPResponse>();
            this.future = future;
            this.tag = tag;
            this.requestSentTime = 0;
        }

        /**
         * Sets the state of the command.
         *
         * @param state the target state to set
         * @param clock the clock instance to record time
         */
        public void setState(@Nonnull final CommandState state, @Nonnull final Clock clock) {
            this.state = state;
            if (state == CommandState.REQUEST_SENT) {
                this.requestSentTime = clock.millis();
            }
        }

        /**
         * @return the state of the command
         */
        public CommandState getState() {
            return state;
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

        /**
         * @return the tag for this imap command
         */
        public String getTag() {
            return tag;
        }

        /**
         * @return the request sent time, 0 if it is not set
         */
        public long getRequestSentTime() {
            return requestSentTime;
        }

        /**
         * Populates the entry information to the given StringBuilder.
         *
         * @param sb StringBuilder instance to output the entry information
         */
        public void debugInfo(@Nonnull final StringBuilder sb) {
            sb.append(CMD_TAG).append(tag).append(CMD_TYPE).append(getRequest().getCommandType()).append(CMD_SENT).append(getRequestSentTime());
        }
    }

    /**
     * Initializes an imap session that supports async operations.
     *
     * @param clock Clock instance
     * @param channel Channel object established for this session
     * @param logger Logger object
     * @param debugMode Flag for debugging
     * @param sessionId the session id
     * @param pipeline the ChannelPipeline object
     * @param sessionCtx context for client to store information
     */
    public ImapAsyncSessionImpl(@Nonnull final Clock clock, @Nonnull final Channel channel, @Nonnull final Logger logger,
            @Nonnull final DebugMode debugMode, final long sessionId, final ChannelPipeline pipeline, @Nonnull final Object sessionCtx) {
        this.channelRef.set(channel);
        this.clock = clock;
        this.logger = logger;
        this.debugModeRef.set(debugMode);
        this.sessionId = sessionId;
        this.requestsQueue = new ConcurrentLinkedQueue<ImapCommandEntry>();
        this.tagSequence = new AtomicLong(0);
        this.sessionCtx = sessionCtx;
        pipeline.addLast(ImapClientCommandRespHandler.HANDLER_NAME, new ImapClientCommandRespHandler(this));
    }

    /**
     * @return returns the user information
     */
    private String getUserInfo() {
        return sessionCtx.toString();
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
        return logger.isTraceEnabled() || (logger.isDebugEnabled() && debugModeRef.get() == DebugMode.DEBUG_ON);
    }

    @Override
    public void setDebugMode(@Nonnull final DebugMode newOption) {
        this.debugModeRef.set(newOption);
    }

    @Override
    public ImapFuture<ImapAsyncResponse> execute(@Nonnull final ImapRequest command) throws ImapAsyncClientException {
        if (isChannelClosed()) { // fail fast instead of entering to sendRequest() to fail
            throw new ImapAsyncClientException(FailureType.OPERATION_PROHIBITED_ON_CLOSED_CHANNEL, sessionId, sessionCtx);
        }
        if (!requestsQueue.isEmpty()) { // when prior command is in process, do not allow the new one
            throw new ImapAsyncClientException(FailureType.COMMAND_NOT_ALLOWED, sessionId, sessionCtx);
        }

        final ImapFuture<ImapAsyncResponse> cmdFuture = new ImapFuture<ImapAsyncResponse>();
        final String tag = getNextTag();
        requestsQueue.add(new ImapCommandEntry(command, cmdFuture, tag));

        final ByteBuf buf = Unpooled.buffer();

        buf.writeBytes(tag.getBytes(StandardCharsets.US_ASCII));
        buf.writeByte(SPACE);
        buf.writeBytes(command.getCommandLineBytes());

        sendRequest(buf, command);

        return cmdFuture;
    }

    @Override
    public <T> ImapFuture<ImapAsyncResponse> startCompression() throws ImapAsyncClientException {
        final ImapFuture<ImapAsyncResponse> future = execute(new CompressCommand());
        return future;
    }

    /**
     * @return true if channel is closed; false otherwise
     */
    boolean isChannelClosed() {
        return !channelRef.get().isActive();
    }

    /**
     * Sends the given request to server when being called.
     *
     * @param request the message of the request
     * @param command the imap command
     * @throws ImapAsyncClientException when channel is closed
     */
    private void sendRequest(@Nonnull final ByteBuf request, @Nonnull final ImapRequest command) throws ImapAsyncClientException {
        if (isDebugEnabled()) {
            // log given request if it not sensitive, otherwise log the debug data decided by command
            logger.debug(CLIENT_LOG_REC, sessionId, getUserInfo(),
                    (!command.isCommandLineDataSensitive()) ? request.toString(StandardCharsets.UTF_8) : command.getDebugData());
        }
        if (isChannelClosed()) {
            throw new ImapAsyncClientException(FailureType.OPERATION_PROHIBITED_ON_CLOSED_CHANNEL, sessionId, sessionCtx);
        }

        // ChannelPromise is the suggested ChannelFuture that allows caller to setup listener before the action is made
        // this is useful for light-speed operation.
        final Channel channel = channelRef.get();
        final ChannelPromise writeFuture = channel.newPromise();
        writeFuture.addListener(this); // "this" listens to write future done in operationComplete() to handle exception in writing.
        channel.writeAndFlush(request, writeFuture);
    }

    @Override
    public ImapFuture<ImapAsyncResponse> terminateCommand(@Nonnull final ImapRequest command) throws ImapAsyncClientException {
        if (requestsQueue.isEmpty()) {
            throw new ImapAsyncClientException(FailureType.COMMAND_NOT_ALLOWED, sessionId, sessionCtx);
        }

        final ImapCommandEntry entry = requestsQueue.peek();
        sendRequest(entry.getRequest().getTerminateCommandLine(), command);
        return entry.getFuture();
    }

    /**
     * Listens to write to server complete future.
     *
     * @param future ChannelFuture instance to check whether the future has completed successfully
     */
    @Override
    public void operationComplete(final ChannelFuture future) {
        final ImapCommandEntry entry = requestsQueue.peek();
        if (entry != null) {
            // set the state to REQUEST_SENT regardless success or not
            entry.setState(ImapCommandEntry.CommandState.REQUEST_SENT, clock);
        }

        if (!future.isSuccess()) { // failed to write to server
            handleChannelException(new ImapAsyncClientException(FailureType.WRITE_TO_SERVER_FAILED, future.cause(), sessionId, sessionCtx));
        }
    }

    @Override
    public void handleChannelClosed() {
        if (isDebugEnabled()) {
            logger.debug(SESSION_LOG_REC, sessionId, getUserInfo(), "Session is confirmed closed.");
        }

        final StringBuilder sb = new StringBuilder(getUserInfo());
        final ImapCommandEntry curEntry = getFirstEntry();
        if (curEntry != null) {
            curEntry.debugInfo(sb);
        }

        // set the future done if there is any
        requestDoneWithException(new ImapAsyncClientException(FailureType.CHANNEL_DISCONNECTED, sessionId, sb.toString()));
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
        return (requestsQueue.isEmpty()) ? null : requestsQueue.peek();
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
        if (isDebugEnabled()) {
            logger.debug(SESSION_LOG_WITH_EXCEPTION, sessionId, getUserInfo(), cause);
        }
        entry.getFuture().done(cause);

        // close session when encountering channel exception since the health of session is frail/unknown.
        close();
    }

    @Override
    public void handleChannelException(@Nonnull final Throwable cause) {
        final StringBuilder sb = new StringBuilder(getUserInfo());
        final ImapCommandEntry curEntry = getFirstEntry();
        if (curEntry != null) {
            curEntry.debugInfo(sb);
        }
        requestDoneWithException(new ImapAsyncClientException(FailureType.CHANNEL_EXCEPTION, cause, sessionId, sb.toString()));
    }

    @Override
    public void handleIdleEvent(@Nonnull final IdleStateEvent idleEvent) {
        final ImapCommandEntry curEntry = getFirstEntry();
        // only throws channel timeout when a request is sent and we are waiting for the responses to come
        if (curEntry == null || curEntry.getState() != ImapCommandEntry.CommandState.REQUEST_SENT || curEntry.getRequest() instanceof IdleCommand) {
            return;
        }

        // error out for any other commands sent but server is not responding
        final StringBuilder sb = new StringBuilder(getUserInfo());
        curEntry.debugInfo(sb);

        requestDoneWithException(new ImapAsyncClientException(FailureType.CHANNEL_TIMEOUT, sessionId, sb.toString()));
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
            logger.debug(SERVER_LOG_REC, sessionId, getUserInfo(), serverResponse.toString());
        }

        // server sends continuation message (+) for next request
        if (serverResponse.isContinuation()) {
            try {
                curEntry.setState(ImapCommandEntry.CommandState.RESPONSES_DONE, clock);
                final ByteBuf cmdAfterContinue = currentCmd.getNextCommandLineAfterContinuation(serverResponse);
                if (cmdAfterContinue == null) {
                    return; // no data from client after continuation, we leave, this is for Idle
                }
                curEntry.setState(ImapCommandEntry.CommandState.REQUEST_IN_PREPARATION, clock); // preparing to send request
                sendRequest(cmdAfterContinue, currentCmd);

            } catch (final ImapAsyncClientException | RuntimeException e) { // when encountering an error on building request from client
                requestDoneWithException(
                        new ImapAsyncClientException(ImapAsyncClientException.FailureType.CHANNEL_EXCEPTION, e, sessionId, sessionCtx));
            }
            return;

        } else if (serverResponse.isTagged() && curEntry.getTag().equals(serverResponse.getTag())) {
            // If this is a matching command completion response, we are done
            try {
                curEntry.setState(ImapCommandEntry.CommandState.RESPONSES_DONE, clock);
                if (currentCmd instanceof CompressCommand && serverResponse.isOK()) {
                    // check whether channel is closed before dereferencing.
                    if (isChannelClosed()) {
                        requestDoneWithException(
                                new ImapAsyncClientException(FailureType.OPERATION_PROHIBITED_ON_CLOSED_CHANNEL, sessionId, sessionCtx));
                        return;
                    }

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
                requestDoneWithException(
                        new ImapAsyncClientException(ImapAsyncClientException.FailureType.CHANNEL_EXCEPTION, e, sessionId, sessionCtx));
            }
        }

        // none-tagged server responses if reaching here
    }

    @Override
    public ImapFuture<Boolean> close() {
        final ImapFuture<Boolean> closeFuture = new ImapFuture<Boolean>();
        if (isChannelClosed()) {
            closeFuture.done(Boolean.TRUE);
        } else {
            if (isDebugEnabled()) {
                logger.debug(SESSION_LOG_REC, sessionId, getUserInfo(), "Closing the session via close().");
            }
            final Channel channel = channelRef.get();
            final ChannelPromise channelPromise = channel.newPromise();
            final ImapChannelClosedListener channelClosedListener = new ImapChannelClosedListener(closeFuture);
            channelPromise.addListener(channelClosedListener);
            // this triggers handleChannelDisconnected() hence no need to handle queue here. We use close() instead of disconnect() to ensure it is
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
                imapSessionCloseFuture
                        .done(new ImapAsyncClientException(FailureType.CLOSING_CONNECTION_FAILED, future.cause(), sessionId, sessionCtx));
            }
        }

    }
}
