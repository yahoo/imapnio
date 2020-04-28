package com.yahoo.imapnio.async.client;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yahoo.imapnio.async.client.ImapAsyncSession.DebugMode;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;
import com.yahoo.imapnio.async.internal.ImapAsyncSessionImpl;
import com.yahoo.imapnio.async.netty.ImapClientConnectHandler;
import com.yahoo.imapnio.client.ImapClientRespReader;
import com.yahoo.imapnio.command.ImapClientRespDecoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Imap async client implementation.
 */
public class ImapAsyncClient {

    /** Literal for imaps. */
    private static final String IMAPS = "imaps";

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

    /** Debug record. */
    private static final String CONNECT_RESULT_REC = "[{},{}] connect operationComplete. result={}, imapServerUri={}, sniNames={}";

    /** Client context not available. */
    private static final String NA_CLIENT_CONTEXT = "NA";

    /** Clock instance. */
    @Nonnull
    private final Clock clock;

    /** logger for sending error, warning, info, debug to the log file. */
    @Nonnull
    private final Logger logger;

    /** Counter for session. */
    private final AtomicLong sessionCount = new AtomicLong(1);

    /** The Netty bootstrap. */
    private final Bootstrap bootstrap;

    /** Event loop group that will serve all channels for IMAP client. */
    private final EventLoopGroup group;

    /**
     * This class initialized the pipeline with the right handlers.
     */
    final class ImapClientChannelInitializer extends ChannelInitializer<SocketChannel> {
        /** Read timeout for channel. */
        private int imapReadTimeoutValue;

        /** Unit for IdleStateHandler parameters. */
        private TimeUnit timeUnit;

        /**
         * Initializes {@link ImapClientChannelInitializer} with the read time out value.
         *
         * @param imapReadTimeoutValue timeout value for server not responding after write command is sent
         * @param unit unit of time
         */
        private ImapClientChannelInitializer(final int imapReadTimeoutValue, final TimeUnit unit) {
            this.imapReadTimeoutValue = imapReadTimeoutValue;
            this.timeUnit = unit;
        }

        @Override
        protected void initChannel(final SocketChannel ch) {
            final ChannelPipeline pipeline = ch.pipeline();

            // setting all idle timeout to ensure event will only be triggered when both read and write not happened for the given time
            pipeline.addLast(IDLE_STATE_HANDLER_NAME, new IdleStateHandler(0, 0, imapReadTimeoutValue, timeUnit)); // duplex
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
        this(Clock.systemUTC(), new Bootstrap(), new NioEventLoopGroup(numOfThreads), LoggerFactory.getLogger(ImapAsyncClient.class));
    }

    /**
     * Constructs a NIO based IMAP client.
     *
     * @param clock Clock instance
     * @param bootstrap a {@link Bootstrap} instance that makes it easy to bootstrap a {@link Channel} to use for clients
     * @param group an @{link EventLoopGroup} instance allowing registering {@link Channel}s for processing later selection during the event loop
     * @param logger Logger instance
     */
    ImapAsyncClient(@Nonnull final Clock clock, @Nonnull final Bootstrap bootstrap, @Nonnull final EventLoopGroup group,
            @Nonnull final Logger logger) {
        this.clock = clock;
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
     * @param config configuration to be used for this session/connection
     * @param localAddress the local network interface to us
     * @param sniNames Server Name Indication names list
     * @param logOpt session logging option for the session to be created
     * @return the ChannelFuture object
     */
    public Future<ImapAsyncCreateSessionResponse> createSession(@Nonnull final URI serverUri, @Nonnull final ImapAsyncSessionConfig config,
            @Nullable final InetSocketAddress localAddress, @Nullable final List<String> sniNames, @Nonnull final DebugMode logOpt) {
        return createSession(serverUri, config, localAddress, sniNames, logOpt, NA_CLIENT_CONTEXT, null);
    }

    /**
     * Connects to the remote server asynchronously and returns a future for the ImapSession if connection is established.
     **
     * @param serverUri IMAP server URI
     * @param config configuration to be used for this session/connection
     * @param localAddress the local network interface to us
     * @param sniNames Server Name Indication names list
     * @param logOpt session logging option for the session to be created
     * @param sessionCtx context associated with the session created. Its toString() will be called upon displaying exception or debug logging
     * @return the ChannelFuture object
     */
    public Future<ImapAsyncCreateSessionResponse> createSession(@Nonnull final URI serverUri, @Nonnull final ImapAsyncSessionConfig config,
            @Nullable final InetSocketAddress localAddress, @Nullable final List<String> sniNames, @Nonnull final DebugMode logOpt,
            @Nonnull final Object sessionCtx) {
        return createSession(serverUri, config, localAddress, sniNames, logOpt, sessionCtx, null);
    }

    /**
     * Connects to the remote server asynchronously and returns a future for the ImapSession if connection is established.
     **
     * @param serverUri IMAP server URI
     * @param config configuration to be used for this session/connection
     * @param localAddress the local network interface to us
     * @param sniNames Server Name Indication names list
     * @param logOpt session logging option for the session to be created
     * @param sessionCtx context associated with the session created. Its toString() will be called upon displaying exception or debug logging
     * @param jdkSslContext a pre-configured {@link SSLContext} which uses JDK's SSL/TLS implementation
     * @return the ChannelFuture object
     */
    public Future<ImapAsyncCreateSessionResponse> createSession(@Nonnull final URI serverUri, @Nonnull final ImapAsyncSessionConfig config,
            @Nullable final InetSocketAddress localAddress, @Nullable final List<String> sniNames, @Nonnull final DebugMode logOpt,
            @Nonnull final Object sessionCtx, @Nullable final SSLContext jdkSslContext) {

        final boolean isSessionDebugOn = (logOpt == DebugMode.DEBUG_ON);
        // ------------------------------------------------------------
        // obtain config values
        final int connectionTimeMillis = config.getConnectionTimeoutMillis();
        final int readTimeMillis = config.getReadTimeoutMillis();

        // ------------------------------------------------------------
        // setup ChannelInitializer, handlers here need to be session-less
        bootstrap.handler(new ImapClientChannelInitializer(readTimeMillis, TimeUnit.MILLISECONDS));

        // ------------------------------------------------------------
        // connect to remote server now, setup connection timeout time before connection
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeMillis);

        final ImapFuture<ImapAsyncCreateSessionResponse> sessionFuture = new ImapFuture<ImapAsyncCreateSessionResponse>();
        final ChannelFuture nettyConnectFuture;
        if (null != localAddress) {
            final InetSocketAddress remoteAddress = new InetSocketAddress(serverUri.getHost(), serverUri.getPort());
            nettyConnectFuture = bootstrap.connect(remoteAddress, localAddress);
        } else {
            nettyConnectFuture = bootstrap.connect(serverUri.getHost(), serverUri.getPort());
        }

        // setup listener to handle connection done event
        nettyConnectFuture.addListener(new GenericFutureListener<io.netty.util.concurrent.Future<? super Void>>() {
            @Override
            public void operationComplete(final io.netty.util.concurrent.Future<? super Void> future) {
                if (future.isSuccess()) {

                    // add the session specific handlers
                    final Channel ch = nettyConnectFuture.channel();
                    final ChannelPipeline pipeline = ch.pipeline();

                    // ------------------------------------------------------------
                    // setup session
                    final boolean isSSL = serverUri.getScheme().toLowerCase().equals(IMAPS);

                    if (isSSL) {
                        SslContext sslContext;
                        try {
                            // if callers want to use their predefined SSLContext, we need to wrap it with JdkSslContext
                            sslContext = (jdkSslContext == null) ? SslContextBuilder.forClient().build()
                                    : new JdkSslContext(jdkSslContext, true, ClientAuth.NONE);
                        } catch (final SSLException e) {
                            final ImapAsyncClientException ex = new ImapAsyncClientException(FailureType.CONNECTION_SSL_EXCEPTION, e);
                            sessionFuture.done(ex);
                            logger.error(CONNECT_RESULT_REC, "NA", sessionCtx.toString(), "failure", serverUri.toASCIIString(), sniNames, ex);
                            closeChannel(ch);
                            return;
                        }
                        final List<SNIServerName> serverNames = new ArrayList<SNIServerName>();
                        if (null != sniNames && !sniNames.isEmpty()) { // SNI support
                            for (final String sni : sniNames) {
                                serverNames.add(new SNIHostName(sni));
                            }
                            final SSLParameters params = new SSLParameters();
                            params.setServerNames(serverNames);

                            final SSLEngine engine = sslContext.newEngine(ch.alloc(), serverUri.getHost(), serverUri.getPort());
                            engine.setSSLParameters(params);
                            pipeline.addFirst(SSL_HANDLER, new SslHandler(engine)); // in/outbound
                        } else {
                            // in/outbound
                            pipeline.addFirst(SSL_HANDLER, sslContext.newHandler(ch.alloc(), serverUri.getHost(), serverUri.getPort()));
                        }
                    }

                    final long sessionId = sessionCount.incrementAndGet();
                    sessionCount.compareAndSet(Long.MAX_VALUE - 1, 1); // roll back to 1 if reaching the max
                    pipeline.addLast(ImapClientConnectHandler.HANDLER_NAME, new ImapClientConnectHandler(clock, sessionFuture,
                            LoggerFactory.getLogger(ImapAsyncSessionImpl.class), logOpt, sessionId, sessionCtx));

                    if (logger.isTraceEnabled() || isSessionDebugOn) {
                        logger.debug(CONNECT_RESULT_REC, sessionId, sessionCtx.toString(), "success", serverUri.toASCIIString(), sniNames);
                    }
                    // connect action is not done until we receive the first OK response from server, so we CANNOT call it done here
                } else { // failure case
                    final Throwable cause = future.cause();
                    FailureType type = null;
                    if (cause instanceof UnknownHostException) {
                        type = FailureType.UNKNOWN_HOST_EXCEPTION;
                    } else if (cause instanceof ConnectTimeoutException) {
                        type = FailureType.CONNECTION_TIMEOUT_EXCEPTION;
                    } else {
                        type = FailureType.CONNECTION_FAILED_EXCEPTION;
                    }
                    final ImapAsyncClientException ex = new ImapAsyncClientException(type, cause);
                    sessionFuture.done(ex);
                    logger.error(CONNECT_RESULT_REC, "NA", sessionCtx.toString(), "failure", serverUri.toASCIIString(), sniNames, ex);
                    closeChannel(nettyConnectFuture.channel());
                }
            }
        });

        return sessionFuture;
    }

    /**
     * Closes channel.
     *
     * @param channel the channel
     */
    private void closeChannel(@Nullable final Channel channel) {
        if (channel != null && channel.isActive()) {
            channel.close();
        }
    }

    /**
     * Closes all of the sessions within a client, and shutdown the event group.
     */
    public void shutdown() {
        this.group.shutdownGracefully();
    }
}
