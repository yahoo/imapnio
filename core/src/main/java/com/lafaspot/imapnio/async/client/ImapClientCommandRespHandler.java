package com.lafaspot.imapnio.async.client;

import java.util.List;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.IMAPResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * This class handles the business logic of how to process messages and handle events.
 */
public class ImapClientCommandRespHandler extends MessageToMessageDecoder<IMAPResponse> {

    /** Literal for the name registered in pipeline. */
    public static final String HANDLER_NAME = "ImapClientCommandRespHandler";

    /** The imap channel event processor. */
    private ImapCommandChannelEventProcessor processor;

    /**
     * Initialized a handler to process pipeline response and events. This handler should include client business logic.
     *
     * @param processor imap channel processor that handles the imap events
     */
    public ImapClientCommandRespHandler(@Nonnull final ImapCommandChannelEventProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void decode(final ChannelHandlerContext ctx, final IMAPResponse msg, final List<Object> out) {
        processor.handleChannelResponse(msg);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        processor.handleChannelException(cause);
    }

    /**
     * Receives an idle state event when READER_IDLE (no data was received for a while) or WRITER_IDLE (no data was sent for a while).
     *
     * @param ctx channel handler ctx
     * @param msg idle state event generated on idle connections by IdleStateHandler
     */
    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object msg) {
        if (msg instanceof IdleStateEvent) { // Handle the IdleState if needed
            final IdleStateEvent event = (IdleStateEvent) msg;
            if (event.state() == IdleState.READER_IDLE) {
                // handle idle event in processor itself: when during idleCommand, we allow server not to send, but disallow during other commands
                processor.handleIdleEvent(event);
            }
        }
    }

	/**
	 * Handles the event when a channel is closed(disconnected) either by server or
	 * client.
	 * 
	 * @param ctx channel handler ctx
	 */
    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        if (processor == null) {
            return; // cleanup() has been called, leave
        }
        processor.handleChannelClosed();
        cleanup();
    }

    /**
     * Avoids loitering.
     */
    private void cleanup() {
        processor = null;
    }
}
