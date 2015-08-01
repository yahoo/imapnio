package com.lafaspot.imapnio.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lafaspot.imapnio.listener.SessionListener;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * Listener for the channel.
 * @author kraman
 *
 */
public class IMAPChannelListener implements ChannelHandler {

    /** logger. */
    private static final Logger log = LoggerFactory.getLogger(IMAPChannelListener.class);

    /** The IMAP session. */
    private final IMAPSession session;

    /**
     * Constructs a listener.
     * @param session IMAP session
     */
    public IMAPChannelListener(final IMAPSession session) {
        this.session = session;
    }

     /**
      * Callback when a new handler has been added to channel.
      * @param ctx the channel handler context
      */
    public void handlerAdded(final ChannelHandlerContext ctx)  {
        if (log.isDebugEnabled()) {
            log.debug("channel - added");
        }
    }

    /**
     * A channel has been removed, socket disconnect event. Call the client listener if one was registered.
     *
     * @param ctx the channel handler context
     */
    public void handlerRemoved(final ChannelHandlerContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("channel closed - removed");
        }
        if (null != session && session.getSessionListener() != null) {
            ((SessionListener) session.getSessionListener()).onDisconnect(session);
        }
    }

    /**
     * Got exception from channel. Call the client listener if one was registered.
     * @param ctx context
     * @param cause failure reason
     */
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        if (log.isDebugEnabled()) {
            log.debug("channel closed - exception", cause);
        }
        if (null != session && session.getSessionListener() != null) {
            ((SessionListener) session.getSessionListener()).onDisconnect(session);
        }
    }

}
