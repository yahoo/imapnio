/**
 *
 */
package com.lafaspot.imapnio.client;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * Class that handles the inactivity timeout callback from NETTY.
 *
 * @author kraman
 *
 */
public class IMAPClientInactivityHandler extends ChannelDuplexHandler {

    /** IMAP session being used. */
    private final IMAPSession session;

    /**
     * Construct a new inactivity handler.
     *
     * @param session IMAP session used
     */
    public IMAPClientInactivityHandler(final IMAPSession session) {
        this.session = session;
        if (session.getLogger().isDebug()) {
            session.getLogger().debug("constructing the inactivity handler", null);
        }
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        if (session.getLogger().isDebug()) {
            session.getLogger().debug("Got event - inactivity handler" + evt, null);
        }
        if (evt instanceof IdleStateEvent) {
            session.getConnectionListener().onInactivityTimeout(session);
        } else {
            session.getLogger().error("Got unknow event in inactivity-handler" + evt, null);
        }
    }

}
