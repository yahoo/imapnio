/**
 *
 */
package org.apache.imapnioclient.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLException;

import org.apache.imapnioclient.config.IMAPClientConfig;
import org.apache.imapnioclient.exception.IMAPSessionException;
import org.slf4j.LoggerFactory;

/**
 * Netty based NIO IMAP client.
 *
 * @author kraman
 *
 */
public enum IMAPClient {
	
	INSTANCE;

    /** Client configuration. */
    private final IMAPClientConfig config;

    private Bootstrap bootstrap;

    /** Event loop group that will serve all channels for IMAP client. */
    private final EventLoopGroup group;


    /** logger. */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IMAPClient.class);

    /**
     * Constructs a NIO based IMAP client.
     * @param config
     *            - client configuration to be used.
     */
    IMAPClient() {
        this.config = new IMAPClientConfig();
        this.bootstrap = new Bootstrap();
        this.group = new NioEventLoopGroup(config.EVENT_GROUP_NUM_THREADS);
    }

    public IMAPSession createSession(URI uri) throws IMAPSessionException {
        return new IMAPSession(uri, bootstrap, group);
    }

    /**
     * End a session contained within a client.
     *
     * @param session
     */
    public void endSession(IMAPSession session) {
        session.disconnect();
    }

    /**
     * Close all of the sessions within a client, and shutdown the event group.
     */
    public void close() {
        this.group.shutdownGracefully();
    }

}
