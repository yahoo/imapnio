/**
 *
 */
package com.lafaspot.imapnio.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.URI;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import com.lafaspot.imapnio.exception.IMAPSessionException;
import com.lafaspot.imapnio.listener.IMAPConnectionListener;
import com.lafaspot.logfast.logging.LogManager;

/**
 * Netty based IMAP NIO client.
 *
 * @author kraman
 *
 */
public class IMAPClient {

    /** instance id used for debug. */
    private final String instanceId = Integer.toString(new Random(System.nanoTime()).nextInt());

    /** counter for session. */
    private AtomicInteger sessionCounter = new AtomicInteger(1);

    /** The netty bootstrap. */
    private final Bootstrap bootstrap;

    /** Event loop group that will serve all channels for IMAP client. */
    private final EventLoopGroup group;

    /**
     * Constructs a NIO based IMAP client.
     *
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
     *
     * @param uri IMAP server URI
     * @param config configuration to be used for this session
     * @param listener client listener for connect/disconnect events
     * @param logManager instance of the LogManager
     * @return newly created session object
     * @throws IMAPSessionException on error
     */
    public IMAPSession createSession(@Nonnull final URI uri, @Nonnull final Properties config, @Nonnull final IMAPConnectionListener listener,
            @Nonnull final LogManager logManager)
            throws IMAPSessionException {
        String sessionId = new StringBuffer("[").append(instanceId).append(sessionCounter.incrementAndGet()).append("]").toString();
        return new IMAPSession(sessionId, uri, config, bootstrap, group, listener, logManager);
    }

    /**
     * Close all of the sessions within a client, and shutdown the event group.
     */
    public void shutdown() {
        this.group.shutdownGracefully();
    }

}
