package com.lafaspot.imapnio.async.client;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;
import com.lafaspot.imapnio.async.exception.ImapAsyncClientException.FailureType;
import com.lafaspot.imapnio.client.ImapClientRespReader;
import com.lafaspot.imapnio.command.ImapClientRespDecoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Imap async client implementation.
 */
public class ImapAsyncClient {

    /** Literal for imaps. */
    private static final String IMAPS = "imaps";

    /** Socket connect timeout. */
    public static final String CONFIG_CONNECTION_TIMEOUT_KEY = "mail.imap.connectiontimeout";

    /** Config key for inactivity timeout value - in seconds. */
    public static final String CONFIG_IMAP_INACTIVITY_TIMEOUT_KEY = "mail.imap.inactivity";

    /** IMAP command timeout. */
    public static final String CONFIG_IMAP_TIMEOUT_KEY = "mail.imap.timeout";

    /** Socket connect timeout value. */
    public static final String DEFAULT_CONNECTION_TIMEOUT_VALUE = "60000";

    /** IMAP command timeout value. */
    public static final String DEFAULT_IMAP_TIMEOUT_VALUE = "60000";

    /** Handler name for ssl handler. */
    public static final String SSL_HANDLER = "sslHandler";

    /** Handler name for idle sate handler. */
    private static final String IDLE_STATE_HANDLER_NAME = "idlestateHandler";

    /** Handler name for string decoder. */
    private static final String IMAP_LINE_DECODER_HANDLER_NAME = "ImapClientRespReader";

    /** Handler name for string decoder. */
    private static final String STRING_DECODER_HANDLER_NAME = "decoder";

    /** Handler name for string encoder. */
    private static final String STRING_ENCODER_HANDLER_NAME = "encoder";

    /** Handler name for string encoder. */
    private static final String STRING_IMAP_MSG_RESPONSE_NAME = "ImapClientRespDecoder";

    /** logger for sending error, warning, info, debug to the log file. */
    @Nonnull
    private final Logger logger;

    /** Counter for session. */
    private final AtomicInteger sessionCount = new AtomicInteger(1);

    /** The netty bootstrap. */
    private final Bootstrap bootstrap;

    /** Event loop group that will serve all channels for IMAP client. */
    private final EventLoopGroup group;

    /** The SSL context. */
    private final SslContext sslContext;

    /**
     * This class initialized the pipeline with the right handlers.
     */
    final class ImapClientChannelInitializer extends ChannelInitializer<SocketChannel> {
        /** Read timeout in seconds for channel. */
        private int imapReadTimeoutValue;

        /**
         * Initializes @{code ImapClientChannelInitializer} with the read time out value.
         *
         * @param imapReadTimeoutValue timeout value for server not responding
         */
        private ImapClientChannelInitializer(final int imapReadTimeoutValue) {
            this.imapReadTimeoutValue = imapReadTimeoutValue;
        }

        @Override
        protected void initChannel(final SocketChannel ch) {
            final ChannelPipeline pipeline = ch.pipeline();

            // only enable read timeout
            pipeline.addLast(IDLE_STATE_HANDLER_NAME, new IdleStateHandler(imapReadTimeoutValue, 0, 0)); // duplex
            pipeline.addLast(IMAP_LINE_DECODER_HANDLER_NAME, new ImapClientRespReader(Integer.MAX_VALUE)); // inbound
            pipeline.addLast(STRING_DECODER_HANDLER_NAME, new StringDecoder()); // inbound
            pipeline.addLast(STRING_ENCODER_HANDLER_NAME, new StringEncoder()); // outbound
            pipeline.addLast(STRING_IMAP_MSG_RESPONSE_NAME, new ImapClientRespDecoder()); // inbound to convert to IMAPResponse
        }
    }

    /**
     * Constructs a NIO based IMAP client.
     *
     * @param numOfThreads number of threads to be used by IMAP client
     * @throws SSLException when encountering an error to create a SslContext for this client
     */
    public ImapAsyncClient(final int numOfThreads) throws SSLException {
        this(new Bootstrap(), new NioEventLoopGroup(numOfThreads), LoggerFactory.getLogger(ImapAsyncClient.class));
    }

    /**
     * Constructs a NIO based IMAP client.
     *
     * @param bootstrap a {@link Bootstrap} instance that makes it easy to bootstrap a {@link Channel} to use for clients
     * @param group an @{link EventLoopGroup} instance allowing registering {@link Channel}s for processing later selection during the event loop
     * @param logger Logger instance
     * @throws SSLException when encountering an error to create a SslContext for this client
     */
    ImapAsyncClient(@Nonnull final Bootstrap bootstrap, @Nonnull final EventLoopGroup group, @Nonnull final Logger logger) throws SSLException {
        this.sslContext = SslContextBuilder.forClient().build();
        this.logger = logger;
        this.bootstrap = bootstrap;
        this.group = group;
        bootstrap.channel(NioSocketChannel.class); // for client
        bootstrap.group(group);
    }

    /**
     * Connects to the remote server asynchronously and returns a future for the ImapSession if connection is established.
     **
     * @param serverUri IMAP server URI
     * @param properties configuration to be used for this session
     * @param localAddress the local network interface to us
     * @param sniNames Server Name Indication names list
     * @return the ChannelFuture object
     */
    public ImapFuture<ImapAsyncSession> createSession(@Nonnull final URI serverUri, @Nonnull final Properties properties,
            @Nullable final InetSocketAddress localAddress, @Nullable final List<String> sniNames) {
        // ------------------------------------------------------------
        // check if required properties are present
        properties.putIfAbsent(CONFIG_CONNECTION_TIMEOUT_KEY, DEFAULT_CONNECTION_TIMEOUT_VALUE);
        properties.putIfAbsent(CONFIG_IMAP_TIMEOUT_KEY, DEFAULT_IMAP_TIMEOUT_VALUE);

        // ------------------------------------------------------------
        // setup ChannelInitializer, handlers here need to be session-less
        bootstrap.handler(new ImapClientChannelInitializer(Integer.parseInt(properties.getProperty(CONFIG_IMAP_TIMEOUT_KEY))));

        // ------------------------------------------------------------
        // connect to remote server now, setup connection timeout time before connection
        final Integer connectTimeout = Integer.parseInt(properties.getProperty(CONFIG_CONNECTION_TIMEOUT_KEY));
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);

        final ImapFuture<ImapAsyncSession> sessionFuture = new ImapFuture<ImapAsyncSession>();
        final ChannelFuture nettyConnectFuture;
        if (null != localAddress) {
            final InetSocketAddress remoteAddress = new InetSocketAddress(serverUri.getHost(), serverUri.getPort());
            nettyConnectFuture = bootstrap.connect(remoteAddress, localAddress);
        } else {
            nettyConnectFuture = bootstrap.connect(serverUri.getHost(), serverUri.getPort());
        }

        // setup listener to handle connection done event
        nettyConnectFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(final Future<? super Void> future) {
                if (future.isSuccess()) {

                    // add the session specific handlers
                    final Channel ch = nettyConnectFuture.channel();
                    final ChannelPipeline pipeline = ch.pipeline();

                    // ------------------------------------------------------------
                    // setup session
                    final boolean isSSL = serverUri.getScheme().toLowerCase().equals(IMAPS);

                    if (isSSL) {
                        final List<SNIServerName> serverNames = new ArrayList<SNIServerName>();
                        if (null != sniNames && !sniNames.isEmpty()) { // SNI support
                            for (final String sni : sniNames) {
                                serverNames.add(new SNIHostName(sni));
                            }
                            final SSLParameters params = new SSLParameters();
                            params.setServerNames(serverNames);
                            final SSLEngine engine = sslContext.newEngine(ch.alloc());
                            engine.setSSLParameters(params);
                            pipeline.addFirst(SSL_HANDLER, new SslHandler(engine)); // in/outbound
                        } else {
                            // in/outbound
                            pipeline.addFirst(SSL_HANDLER, sslContext.newHandler(ch.alloc(), serverUri.getHost(), serverUri.getPort()));
                        }
                    }
                    final int sessionId = sessionCount.incrementAndGet();
                    pipeline.addLast(ImapClientConnectHandler.HANDLER_NAME,
                            new ImapClientConnectHandler(sessionFuture, LoggerFactory.getLogger(ImapAsyncSessionImpl.class), sessionId));

                    if (logger.isDebugEnabled()) {
                        logger.debug("{}, connect operationComplete with success, imapServerUri={}", sessionId, serverUri.toASCIIString());
                    }
                    // connect action is not done until we receive the first OK response from server, so we CANNOT call it done here
                } else { // failure case
                    final ImapAsyncClientException ex = new ImapAsyncClientException(FailureType.CONNECTION_FAILED_EXCEPTION, future.cause());
                    sessionFuture.done(ex);
                    logger.error("connect operationComplete with success, imapServerUri={}", serverUri.toASCIIString(), ex);
                }
            }
        });

        return sessionFuture;
    }

    /**
     * Closes all of the sessions within a client, and shutdown the event group.
     */
    public void shutdown() {
        this.group.shutdownGracefully();
    }
}
