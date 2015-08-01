/**
 *
 */
package com.lafaspot.imapnio.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.URI;

import com.lafaspot.imapnio.exception.IMAPSessionException;
import com.lafaspot.imapnio.listener.SessionListener;

/**
 * Netty based NIO IMAP client.
 *
 * @author kraman
 *
 */
public class IMAPClient {
	
    /** The netty bootstrap. */
    private Bootstrap bootstrap;

    /** Event loop group that will serve all channels for IMAP client. */
    private final EventLoopGroup group;


    /**
     * Constructs a NIO based IMAP client.
     * @param threads number of threads to be used by IMAP client
     */
    public IMAPClient(final int threads) {
        this.bootstrap = new Bootstrap();
        this.group = new NioEventLoopGroup(threads);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.group(group);
    }

    /**
     * Create a new IMAP session.
     * @param uri IMAP server URI
     * @param listener client listener for connect/disconnect events
     * @return newly created session object
     * @throws IMAPSessionException on error
     */
    public IMAPSession createSession(final URI uri, final SessionListener listener) throws IMAPSessionException {
        return new IMAPSession(uri, bootstrap, group, listener);
    }

    /**
     * End a session contained within a client.
     *
     * @param session the session to end
     */
    public void endSession(final IMAPSession session) {
        session.disconnect();
    }

    /**
     * Close all of the sessions within a client, and shutdown the event group.
     */
    public void close() {
        this.group.shutdownGracefully();
    }

}
