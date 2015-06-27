/**
 *
 */
package com.yahoo.mail.imapnio.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLException;

import org.slf4j.LoggerFactory;

import com.yahoo.mail.imapnio.client.config.IMAPClientConfig;

/**
 * Netty based NIO IMAP client.
 *
 * @author kraman
 *
 */
public class IMAPClient {

    /** Client configuration. */
    private final IMAPClientConfig config;

    private Bootstrap bootstrap;

    /** Event loop group that will serve all channels for IMAP client. */
    private final EventLoopGroup group;


    /** logger. */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IMAPClient.class);

    /**
     * Constructs a NIO based IMAP client.
     *
     * @param config
     *            - client configuration to be used.
     * @param worker
     *            - the worker threadpool to be used.
     */
    public IMAPClient(final ExecutorService worker) {
        this.config = new IMAPClientConfig();
        this.bootstrap = new Bootstrap();
        this.group = new NioEventLoopGroup(config.EVENT_GROUP_NUM_THREADS);
    }

    public IMAPSession createSession(URI uri, String authType) throws SSLException, NoSuchAlgorithmException,
            InterruptedException {
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

    // test
    public static void main(String args[]) throws InterruptedException, SSLException, NoSuchAlgorithmException, URISyntaxException {
        final IMAPClient client = new IMAPClient(Executors.newScheduledThreadPool(5));
        final IMAPSession session = client.createSession(new URI("imaps://imap.gmail.com:993"), null);
        ChannelFuture future = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", null);
        future.awaitUninterruptibly();
    }
}
