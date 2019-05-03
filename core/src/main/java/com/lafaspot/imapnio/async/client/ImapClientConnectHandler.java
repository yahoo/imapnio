package com.lafaspot.imapnio.async.client;

import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;
import com.lafaspot.imapnio.async.exception.ImapAsyncClientException.FailureType;
import com.sun.mail.imap.protocol.IMAPResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * This class handles the business logic of how to process messages and handle events.
 */
public class ImapClientConnectHandler extends MessageToMessageDecoder<IMAPResponse> {

    /** Literal for the name registered in pipeline. */
    public static final String HANDLER_NAME = "ImapClientConnectHandler";

    /** Future for the created session. */
    private ImapFuture<ImapAsyncSession> sessionCreatedFuture;

    /** Logger instance. */
    private Logger logger;

    /** Session Id. */
    private int sessionId;

    /**
     * Initializes @{code ImapClientConnectHandler} to process ok greeting after connection.
     *
     * @param sessionFuture imap session future, should be set to done once ok is recieved
     * @param logger the @{code Logger} instance for @{ImapAsyncSessionImpl}
     * @param sessionId the session id
     */
    public ImapClientConnectHandler(@Nonnull final ImapFuture<ImapAsyncSession> sessionFuture, @Nonnull final Logger logger, final int sessionId) {
        this.sessionCreatedFuture = sessionFuture;
        this.logger = logger;
        this.sessionId = sessionId;
    }

    @Override
    public void decode(final ChannelHandlerContext ctx, final IMAPResponse serverResponse, final List<Object> out) {
        final ChannelPipeline pipeline = ctx.pipeline();
        // this handler is solely used to detect connect greeting from server, job done, removing it
        pipeline.remove(HANDLER_NAME);

        if (serverResponse.isOK()) { // we can call it successful only when response is ok
            //add the command response handler
            final ImapAsyncSessionImpl session = new ImapAsyncSessionImpl(ctx.channel(), logger, sessionId, pipeline);
            sessionCreatedFuture.done(session);

        } else {
            logger.error("[{}] Server response without OK:{}", sessionId, serverResponse.toString());
            sessionCreatedFuture.done(new ImapAsyncClientException(FailureType.CONNECTION_FAILED_WITHOUT_OK_RESPONSE));
        }
        cleanup();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        sessionCreatedFuture.done(new ImapAsyncClientException(FailureType.CONNECTION_FAILED_EXCEPTION, cause));
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object msg) {
        if (msg instanceof IdleStateEvent) { // Handle the IdleState if needed
            final IdleStateEvent event = (IdleStateEvent) msg;
            if (event.state() == IdleState.READER_IDLE) {
                sessionCreatedFuture.done(new ImapAsyncClientException(FailureType.CONNECTION_FAILED_EXCEED_IDLE_MAX));
                // closing the channel if server is not responding with OK response for max read timeout limit
                ctx.close();
            }
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        if (sessionCreatedFuture == null) {
            return; // cleanup() has been called, leave
        }
        sessionCreatedFuture.done(new ImapAsyncClientException(FailureType.CONNECTION_INACTIVE));
        cleanup();
    }

    /**
     * Avoids loitering.
     */
    private void cleanup() {
        sessionCreatedFuture = null;
        logger = null;
    }
}
