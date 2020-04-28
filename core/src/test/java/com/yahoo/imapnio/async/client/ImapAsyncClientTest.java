package com.yahoo.imapnio.async.client;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.yahoo.imapnio.async.client.ImapAsyncClient.ImapClientChannelInitializer;
import com.yahoo.imapnio.async.client.ImapAsyncSession.DebugMode;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;
import com.yahoo.imapnio.async.netty.ImapClientConnectHandler;
import com.yahoo.imapnio.client.ImapClientRespReader;
import com.yahoo.imapnio.command.ImapClientRespDecoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Unit test for {@link ImapAsyncClient}.
 */
public class ImapAsyncClientTest {

    /** Timeout for connection. */
    private static final String SERVER_URI_STR = "imaps://one.two.three.com:993";

    /** Server URI without SSL protocol. */
    private static final String NO_SSL_SERVER_URI_STR = "imap://one.two.three.com:993";

    /** Time sequence for the clock tick in milliseconds. */
    private static final Long[] TIME_SEQUENCE = { 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L,
            23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L, 31L, 32L, 33L, 34L, 35L, 36L, 37L, 38L, 39L, 40L, 41L, 42L, 43L, 44L, 45L, 46L, 47L, 48L, 49L,
            50L, 51L, 52L, 53L, 54L, 55L, 56L, 57L, 58L, 59L, 60L };

    /** Clock instance. */
    private Clock clock;

    /**
     * Sets up instance before each test method.
     */
    @BeforeMethod
    public void beforeMethod() {
        clock = Mockito.mock(Clock.class);
        Mockito.when(clock.millis()).thenReturn(1L, TIME_SEQUENCE);
    }

    /**
     * Tests createSession method when successful.
     *
     * @throws SSLException will not throw
     * @throws URISyntaxException will not throw
     * @throws Exception when calling operationComplete() at GenericFutureListener
     */
    @Test
    public void testCreateSessionNoLocalAddressNoSNISuccessful() throws SSLException, URISyntaxException, Exception {

        final Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        final ChannelFuture nettyConnectFuture = Mockito.mock(ChannelFuture.class);
        Mockito.when(nettyConnectFuture.isSuccess()).thenReturn(true);
        final Channel nettyChannel = Mockito.mock(Channel.class);
        final ChannelPipeline nettyPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(nettyChannel.pipeline()).thenReturn(nettyPipeline);
        Mockito.when(nettyConnectFuture.channel()).thenReturn(nettyChannel);
        Mockito.when(bootstrap.connect(Mockito.anyString(), Mockito.anyInt())).thenReturn(nettyConnectFuture);

        final EventLoopGroup group = Mockito.mock(EventLoopGroup.class);
        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isTraceEnabled()).thenReturn(true);

        final ImapAsyncClient aclient = new ImapAsyncClient(clock, bootstrap, group, logger);

        final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
        config.setConnectionTimeoutMillis(5000);
        config.setReadTimeoutMillis(6000);
        final List<String> sniNames = null;

        // test create session
        final InetSocketAddress localAddress = null;
        final URI serverUri = new URI(SERVER_URI_STR);

        final String sessCtx = "abc@nowhere.com";
        final Future<ImapAsyncCreateSessionResponse> future = aclient.createSession(serverUri, config, localAddress, sniNames, DebugMode.DEBUG_OFF,
                sessCtx);

        // verify session creation
        Assert.assertNotNull(future, "Future for ImapAsyncSession should not be null.");

        final ArgumentCaptor<ImapClientChannelInitializer> initializerCaptor = ArgumentCaptor.forClass(ImapClientChannelInitializer.class);
        Mockito.verify(bootstrap, Mockito.times(1)).handler(initializerCaptor.capture());
        Assert.assertEquals(initializerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");
        final ImapClientChannelInitializer initializer = initializerCaptor.getAllValues().get(0);

        // should not call this connect
        Mockito.verify(bootstrap, Mockito.times(0)).connect(Mockito.any(SocketAddress.class), Mockito.any(SocketAddress.class));
        // should call following connect
        Mockito.verify(bootstrap, Mockito.times(1)).connect(Mockito.anyString(), Mockito.anyInt());
        final ArgumentCaptor<GenericFutureListener> listenerCaptor = ArgumentCaptor.forClass(GenericFutureListener.class);
        Mockito.verify(nettyConnectFuture, Mockito.times(1)).addListener(listenerCaptor.capture());
        Assert.assertEquals(listenerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");

        // test connection established and channel initialized new
        final SocketChannel socketChannel = Mockito.mock(SocketChannel.class);
        final ChannelPipeline socketPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(socketChannel.pipeline()).thenReturn(socketPipeline);
        initializer.initChannel(socketChannel);

        // verify initChannel
        final ArgumentCaptor<ChannelHandler> handlerCaptor = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(socketPipeline, Mockito.times(5)).addLast(Mockito.anyString(), handlerCaptor.capture());
        Assert.assertEquals(handlerCaptor.getAllValues().size(), 5, "Unexpected count of ChannelHandler added.");
        // following order should be preserved
        Assert.assertEquals(handlerCaptor.getAllValues().get(0).getClass(), IdleStateHandler.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(1).getClass(), ImapClientRespReader.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(2).getClass(), StringDecoder.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(3).getClass(), StringEncoder.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(4).getClass(), ImapClientRespDecoder.class, "expected class mismatched.");

        // verify GenericFutureListener.operationComplete()
        final GenericFutureListener listener = listenerCaptor.getAllValues().get(0);
        listener.operationComplete(nettyConnectFuture);
        final ArgumentCaptor<ChannelHandler> handlerCaptorFirst = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(1)).addFirst(Mockito.anyString(), handlerCaptorFirst.capture());
        Assert.assertEquals(handlerCaptorFirst.getAllValues().size(), 1, "number of handlers mismatched.");
        Assert.assertEquals(handlerCaptorFirst.getAllValues().get(0).getClass(), SslHandler.class, "expected class mismatched.");

        final ArgumentCaptor<ChannelHandler> handlerCaptorLast = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(1)).addLast(Mockito.anyString(), handlerCaptorLast.capture());
        Assert.assertEquals(handlerCaptorLast.getAllValues().size(), 1, "Unexpected count of ChannelHandler added.");
        Assert.assertEquals(handlerCaptorLast.getAllValues().get(0).getClass(), ImapClientConnectHandler.class, "expected class mismatched.");
        // verify logging messages
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.eq("[{},{}] connect operationComplete. result={}, imapServerUri={}, sniNames={}"),
                Mockito.eq(Long.valueOf(2)), Mockito.eq("abc@nowhere.com"), Mockito.eq("success"), Mockito.eq("imaps://one.two.three.com:993"),
                Mockito.eq(null));
        // call shutdown
        aclient.shutdown();
        Mockito.verify(group, Mockito.times(1)).shutdownGracefully();
    }

    /**
     * Tests createSession method when successful.
     *
     * @throws SSLException will not throw
     * @throws URISyntaxException will not throw
     * @throws Exception when calling operationComplete() at GenericFutureListener
     */
    @Test
    public void testCreateSessionNoLocalAddressNoSSLSuccessful() throws SSLException, URISyntaxException, Exception {

        final Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        final ChannelFuture nettyConnectFuture = Mockito.mock(ChannelFuture.class);
        Mockito.when(nettyConnectFuture.isSuccess()).thenReturn(true);
        final Channel nettyChannel = Mockito.mock(Channel.class);
        final ChannelPipeline nettyPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(nettyChannel.pipeline()).thenReturn(nettyPipeline);
        Mockito.when(nettyConnectFuture.channel()).thenReturn(nettyChannel);
        Mockito.when(bootstrap.connect(Mockito.anyString(), Mockito.anyInt())).thenReturn(nettyConnectFuture);

        final EventLoopGroup group = Mockito.mock(EventLoopGroup.class);
        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isTraceEnabled()).thenReturn(true);

        final ImapAsyncClient aclient = new ImapAsyncClient(clock, bootstrap, group, logger);

        final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
        config.setConnectionTimeoutMillis(5000);
        config.setReadTimeoutMillis(6000);
        final List<String> sniNames = null;

        // test create session
        final InetSocketAddress localAddress = null;
        final URI serverUri = new URI(NO_SSL_SERVER_URI_STR);

        final Future<ImapAsyncCreateSessionResponse> future = aclient.createSession(serverUri, config, localAddress, sniNames, DebugMode.DEBUG_OFF);

        // verify session creation
        Assert.assertNotNull(future, "Future for ImapAsyncSession should not be null.");

        final ArgumentCaptor<ImapClientChannelInitializer> initializerCaptor = ArgumentCaptor.forClass(ImapClientChannelInitializer.class);
        Mockito.verify(bootstrap, Mockito.times(1)).handler(initializerCaptor.capture());
        Assert.assertEquals(initializerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");
        final ImapClientChannelInitializer initializer = initializerCaptor.getAllValues().get(0);

        // should not call this connect
        Mockito.verify(bootstrap, Mockito.times(0)).connect(Mockito.any(SocketAddress.class), Mockito.any(SocketAddress.class));
        // should call following connect
        Mockito.verify(bootstrap, Mockito.times(1)).connect(Mockito.anyString(), Mockito.anyInt());
        final ArgumentCaptor<GenericFutureListener> listenerCaptor = ArgumentCaptor.forClass(GenericFutureListener.class);
        Mockito.verify(nettyConnectFuture, Mockito.times(1)).addListener(listenerCaptor.capture());
        Assert.assertEquals(listenerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");

        // test connection established and channel initialized new
        final SocketChannel socketChannel = Mockito.mock(SocketChannel.class);
        final ChannelPipeline socketPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(socketChannel.pipeline()).thenReturn(socketPipeline);
        initializer.initChannel(socketChannel);

        // verify initChannel
        final ArgumentCaptor<ChannelHandler> handlerCaptor = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(socketPipeline, Mockito.times(5)).addLast(Mockito.anyString(), handlerCaptor.capture());
        Assert.assertEquals(handlerCaptor.getAllValues().size(), 5, "Unexpected count of ChannelHandler added.");
        // following order should be preserved
        Assert.assertEquals(handlerCaptor.getAllValues().get(0).getClass(), IdleStateHandler.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(1).getClass(), ImapClientRespReader.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(2).getClass(), StringDecoder.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(3).getClass(), StringEncoder.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(4).getClass(), ImapClientRespDecoder.class, "expected class mismatched.");

        // verify GenericFutureListener.operationComplete()
        final GenericFutureListener listener = listenerCaptor.getAllValues().get(0);
        listener.operationComplete(nettyConnectFuture);
        final ArgumentCaptor<ChannelHandler> handlerCaptorFirst = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(0)).addFirst(Mockito.anyString(), handlerCaptorFirst.capture());
        Assert.assertEquals(handlerCaptorFirst.getAllValues().size(), 0, "number of handlers mismatched.");

        final ArgumentCaptor<ChannelHandler> handlerCaptorLast = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(1)).addLast(Mockito.anyString(), handlerCaptorLast.capture());
        Assert.assertEquals(handlerCaptorLast.getAllValues().size(), 1, "Unexpected count of ChannelHandler added.");
        Assert.assertEquals(handlerCaptorLast.getAllValues().get(0).getClass(), ImapClientConnectHandler.class, "expected class mismatched.");
        // verify logging messages
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.eq("[{},{}] connect operationComplete. result={}, imapServerUri={}, sniNames={}"),
                Mockito.eq(Long.valueOf(2)), Mockito.eq("NA"), Mockito.eq("success"), Mockito.eq("imap://one.two.three.com:993"), Mockito.eq(null));
        // call shutdown
        aclient.shutdown();
        Mockito.verify(group, Mockito.times(1)).shutdownGracefully();
    }

    /**
     * Tests createSession method when successful.
     *
     * @throws SSLException will not throw
     * @throws URISyntaxException will not throw
     * @throws Exception when calling operationComplete() at GenericFutureListener
     */
    @Test
    public void testCreateSessionNoLocalAddressSNIEmptySuccessful() throws SSLException, URISyntaxException, Exception {

        final Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        final ChannelFuture nettyConnectFuture = Mockito.mock(ChannelFuture.class);
        Mockito.when(nettyConnectFuture.isSuccess()).thenReturn(true);
        final Channel nettyChannel = Mockito.mock(Channel.class);
        final ChannelPipeline nettyPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(nettyChannel.pipeline()).thenReturn(nettyPipeline);
        Mockito.when(nettyConnectFuture.channel()).thenReturn(nettyChannel);
        Mockito.when(bootstrap.connect(Mockito.anyString(), Mockito.anyInt())).thenReturn(nettyConnectFuture);

        final EventLoopGroup group = Mockito.mock(EventLoopGroup.class);
        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isTraceEnabled()).thenReturn(false);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        final ImapAsyncClient aclient = new ImapAsyncClient(clock, bootstrap, group, logger);

        final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
        config.setConnectionTimeoutMillis(5000);
        config.setReadTimeoutMillis(6000);
        final List<String> sniNames = new ArrayList<String>();

        // test create session
        final InetSocketAddress localAddress = null;
        final URI serverUri = new URI(SERVER_URI_STR);

        final Future<ImapAsyncCreateSessionResponse> future = aclient.createSession(serverUri, config, localAddress, sniNames, DebugMode.DEBUG_ON);

        // verify session creation
        Assert.assertNotNull(future, "Future for ImapAsyncSession should not be null.");

        final ArgumentCaptor<ImapClientChannelInitializer> initializerCaptor = ArgumentCaptor.forClass(ImapClientChannelInitializer.class);
        Mockito.verify(bootstrap, Mockito.times(1)).handler(initializerCaptor.capture());
        Assert.assertEquals(initializerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");
        final ImapClientChannelInitializer initializer = initializerCaptor.getAllValues().get(0);

        // should not call this connect
        Mockito.verify(bootstrap, Mockito.times(0)).connect(Mockito.any(SocketAddress.class), Mockito.any(SocketAddress.class));
        // should call following connect
        Mockito.verify(bootstrap, Mockito.times(1)).connect(Mockito.anyString(), Mockito.anyInt());
        final ArgumentCaptor<GenericFutureListener> listenerCaptor = ArgumentCaptor.forClass(GenericFutureListener.class);
        Mockito.verify(nettyConnectFuture, Mockito.times(1)).addListener(listenerCaptor.capture());
        Assert.assertEquals(listenerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");

        // test connection established and channel initialized new
        final SocketChannel socketChannel = Mockito.mock(SocketChannel.class);
        final ChannelPipeline socketPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(socketChannel.pipeline()).thenReturn(socketPipeline);
        initializer.initChannel(socketChannel);

        // verify initChannel
        final ArgumentCaptor<ChannelHandler> handlerCaptor = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(socketPipeline, Mockito.times(5)).addLast(Mockito.anyString(), handlerCaptor.capture());
        Assert.assertEquals(handlerCaptor.getAllValues().size(), 5, "Unexpected count of ChannelHandler added.");
        // following order should be preserved
        Assert.assertEquals(handlerCaptor.getAllValues().get(0).getClass(), IdleStateHandler.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(1).getClass(), ImapClientRespReader.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(2).getClass(), StringDecoder.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(3).getClass(), StringEncoder.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(4).getClass(), ImapClientRespDecoder.class, "expected class mismatched.");

        // verify GenericFutureListener.operationComplete()
        final GenericFutureListener listener = listenerCaptor.getAllValues().get(0);
        listener.operationComplete(nettyConnectFuture);
        final ArgumentCaptor<ChannelHandler> handlerCaptorFirst = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(1)).addFirst(Mockito.anyString(), handlerCaptorFirst.capture());
        Assert.assertEquals(handlerCaptorFirst.getAllValues().size(), 1, "number of handlers mismatched.");
        Assert.assertEquals(handlerCaptorFirst.getAllValues().get(0).getClass(), SslHandler.class, "expected class mismatched.");

        final ArgumentCaptor<ChannelHandler> handlerCaptorLast = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(1)).addLast(Mockito.anyString(), handlerCaptorLast.capture());
        Assert.assertEquals(handlerCaptorLast.getAllValues().size(), 1, "Unexpected count of ChannelHandler added.");
        Assert.assertEquals(handlerCaptorLast.getAllValues().get(0).getClass(), ImapClientConnectHandler.class, "expected class mismatched.");
        // verify if session level is on, whether debug call will be called
        // verify logging messages
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.eq("[{},{}] connect operationComplete. result={}, imapServerUri={}, sniNames={}"),
                Mockito.eq(Long.valueOf(2)), Mockito.eq("NA"), Mockito.eq("success"), Mockito.eq("imaps://one.two.three.com:993"),
                Mockito.eq(new ArrayList<String>()));
    }

    /**
     * Tests createSession method when successful with class level debug on and session level debug off.
     *
     * @throws SSLException will not throw
     * @throws URISyntaxException will not throw
     * @throws Exception when calling operationComplete() at GenericFutureListener
     */
    @Test
    public void testCreateSessionWithLocalAddressSniSuccessfulSessionDebugOff() throws SSLException, URISyntaxException, Exception {

        final Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        final ChannelFuture nettyConnectFuture = Mockito.mock(ChannelFuture.class);
        Mockito.when(nettyConnectFuture.isSuccess()).thenReturn(true);
        final Channel nettyChannel = Mockito.mock(Channel.class);
        final ChannelPipeline nettyPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(nettyChannel.pipeline()).thenReturn(nettyPipeline);
        Mockito.when(nettyConnectFuture.channel()).thenReturn(nettyChannel);
        Mockito.when(bootstrap.connect(Mockito.any(SocketAddress.class), Mockito.any(SocketAddress.class))).thenReturn(nettyConnectFuture);

        final EventLoopGroup group = Mockito.mock(EventLoopGroup.class);
        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        final ImapAsyncClient aclient = new ImapAsyncClient(clock, bootstrap, group, logger);

        final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
        config.setConnectionTimeoutMillis(5000);
        config.setReadTimeoutMillis(6000);
        final List<String> sniNames = new ArrayList<String>();
        sniNames.add("one.two.three.com");
        // test create session
        final InetSocketAddress localAddress = new InetSocketAddress("10.10.10.10", 23112);
        final URI serverUri = new URI(SERVER_URI_STR);
        final Future<ImapAsyncCreateSessionResponse> future = aclient.createSession(serverUri, config, localAddress, sniNames, DebugMode.DEBUG_OFF);

        // verify session creation
        Assert.assertNotNull(future, "Future for ImapAsyncSession should not be null.");

        final ArgumentCaptor<ImapClientChannelInitializer> initializerCaptor = ArgumentCaptor.forClass(ImapClientChannelInitializer.class);
        Mockito.verify(bootstrap, Mockito.times(1)).handler(initializerCaptor.capture());
        Assert.assertEquals(initializerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");
        final ImapClientChannelInitializer initializer = initializerCaptor.getAllValues().get(0);

        // should not call this connect
        Mockito.verify(bootstrap, Mockito.times(1)).connect(Mockito.any(SocketAddress.class), Mockito.any(SocketAddress.class));
        // should call following connect
        Mockito.verify(bootstrap, Mockito.times(0)).connect(Mockito.anyString(), Mockito.anyInt());
        final ArgumentCaptor<GenericFutureListener> listenerCaptor = ArgumentCaptor.forClass(GenericFutureListener.class);
        Mockito.verify(nettyConnectFuture, Mockito.times(1)).addListener(listenerCaptor.capture());
        Assert.assertEquals(listenerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");

        // test connection established and channel initialized new
        final SocketChannel socketChannel = Mockito.mock(SocketChannel.class);
        final ChannelPipeline socketPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(socketChannel.pipeline()).thenReturn(socketPipeline);
        initializer.initChannel(socketChannel);

        // verify initChannel
        final ArgumentCaptor<ChannelHandler> handlerCaptor = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(socketPipeline, Mockito.times(5)).addLast(Mockito.anyString(), handlerCaptor.capture());
        Assert.assertEquals(handlerCaptor.getAllValues().size(), 5, "Unexpected count of ChannelHandler added.");
        // following order should be preserved
        Assert.assertEquals(handlerCaptor.getAllValues().get(0).getClass(), IdleStateHandler.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(1).getClass(), ImapClientRespReader.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(2).getClass(), StringDecoder.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(3).getClass(), StringEncoder.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(4).getClass(), ImapClientRespDecoder.class, "expected class mismatched.");

        // verify GenericFutureListener.operationComplete()
        final GenericFutureListener listener = listenerCaptor.getAllValues().get(0);
        listener.operationComplete(nettyConnectFuture);
        final ArgumentCaptor<ChannelHandler> handlerCaptorFirst = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(1)).addFirst(Mockito.anyString(), handlerCaptorFirst.capture());
        Assert.assertEquals(handlerCaptorFirst.getAllValues().size(), 1, "number of handlers mismatched.");
        Assert.assertEquals(handlerCaptorFirst.getAllValues().get(0).getClass(), SslHandler.class, "expected class mismatched.");

        final ArgumentCaptor<ChannelHandler> handlerCaptorLast = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(1)).addLast(Mockito.anyString(), handlerCaptorLast.capture());
        Assert.assertEquals(handlerCaptorLast.getAllValues().size(), 1, "Unexpected count of ChannelHandler added.");
        Assert.assertEquals(handlerCaptorLast.getAllValues().get(0).getClass(), ImapClientConnectHandler.class, "expected class mismatched.");
        Mockito.verify(logger, Mockito.times(0)).debug(Mockito.anyString(), Mockito.any(Throwable.class));
    }

    /**
     * @return SSLContext instance
     * @throws KeyStoreException will not throw
     * @throws NoSuchAlgorithmException will not throw
     * @throws KeyManagementException will not throw
     */
    private SSLContext buildSSLContext() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init((KeyStore) null);
        final TrustManager[] tm = new TrustManager[] { tmf.getTrustManagers()[0] };
        sslContext.init(null, tm, null);
        return sslContext;
    }

    /**
     * Tests createSession method when successful with class level debug on and session level debug off.
     *
     * @throws SSLException will not throw
     * @throws URISyntaxException will not throw
     * @throws Exception when calling operationComplete() at GenericFutureListener
     */
    @Test
    public void testCreateSessionWithLocalAddressSniSuccessfulSessionDebugOn() throws SSLException, URISyntaxException, Exception {

        final Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        final ChannelFuture nettyConnectFuture = Mockito.mock(ChannelFuture.class);
        Mockito.when(nettyConnectFuture.isSuccess()).thenReturn(true);
        final Channel nettyChannel = Mockito.mock(Channel.class);
        final ChannelPipeline nettyPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(nettyChannel.pipeline()).thenReturn(nettyPipeline);
        Mockito.when(nettyConnectFuture.channel()).thenReturn(nettyChannel);
        Mockito.when(bootstrap.connect(Mockito.any(SocketAddress.class), Mockito.any(SocketAddress.class))).thenReturn(nettyConnectFuture);

        final EventLoopGroup group = Mockito.mock(EventLoopGroup.class);
        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        final ImapAsyncClient aclient = new ImapAsyncClient(clock, bootstrap, group, logger);

        final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
        config.setConnectionTimeoutMillis(5000);
        config.setReadTimeoutMillis(6000);
        final List<String> sniNames = new ArrayList<String>();
        sniNames.add("one.two.three.com");
        // test create session
        final InetSocketAddress localAddress = new InetSocketAddress("10.10.10.10", 23112);
        final URI serverUri = new URI(SERVER_URI_STR);

        final String sessCtx = "someUserId";

        final Future<ImapAsyncCreateSessionResponse> future = aclient.createSession(serverUri, config, localAddress, sniNames, DebugMode.DEBUG_ON,
                sessCtx, buildSSLContext());

        // verify session creation
        Assert.assertNotNull(future, "Future for ImapAsyncSession should not be null.");

        final ArgumentCaptor<ImapClientChannelInitializer> initializerCaptor = ArgumentCaptor.forClass(ImapClientChannelInitializer.class);
        Mockito.verify(bootstrap, Mockito.times(1)).handler(initializerCaptor.capture());
        Assert.assertEquals(initializerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");
        final ImapClientChannelInitializer initializer = initializerCaptor.getAllValues().get(0);

        // should not call this connect
        Mockito.verify(bootstrap, Mockito.times(1)).connect(Mockito.any(SocketAddress.class), Mockito.any(SocketAddress.class));
        // should call following connect
        Mockito.verify(bootstrap, Mockito.times(0)).connect(Mockito.anyString(), Mockito.anyInt());
        final ArgumentCaptor<GenericFutureListener> listenerCaptor = ArgumentCaptor.forClass(GenericFutureListener.class);
        Mockito.verify(nettyConnectFuture, Mockito.times(1)).addListener(listenerCaptor.capture());
        Assert.assertEquals(listenerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");

        // test connection established and channel initialized new
        final SocketChannel socketChannel = Mockito.mock(SocketChannel.class);
        final ChannelPipeline socketPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(socketChannel.pipeline()).thenReturn(socketPipeline);
        initializer.initChannel(socketChannel);

        // verify initChannel
        final ArgumentCaptor<ChannelHandler> handlerCaptor = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(socketPipeline, Mockito.times(5)).addLast(Mockito.anyString(), handlerCaptor.capture());
        Assert.assertEquals(handlerCaptor.getAllValues().size(), 5, "Unexpected count of ChannelHandler added.");
        // following order should be preserved
        Assert.assertEquals(handlerCaptor.getAllValues().get(0).getClass(), IdleStateHandler.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(1).getClass(), ImapClientRespReader.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(2).getClass(), StringDecoder.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(3).getClass(), StringEncoder.class, "expected class mismatched.");
        Assert.assertEquals(handlerCaptor.getAllValues().get(4).getClass(), ImapClientRespDecoder.class, "expected class mismatched.");

        // verify GenericFutureListener.operationComplete()
        final GenericFutureListener listener = listenerCaptor.getAllValues().get(0);
        listener.operationComplete(nettyConnectFuture);
        final ArgumentCaptor<ChannelHandler> handlerCaptorFirst = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(1)).addFirst(Mockito.anyString(), handlerCaptorFirst.capture());
        Assert.assertEquals(handlerCaptorFirst.getAllValues().size(), 1, "number of handlers mismatched.");
        Assert.assertEquals(handlerCaptorFirst.getAllValues().get(0).getClass(), SslHandler.class, "expected class mismatched.");

        final ArgumentCaptor<ChannelHandler> handlerCaptorLast = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(1)).addLast(Mockito.anyString(), handlerCaptorLast.capture());
        Assert.assertEquals(handlerCaptorLast.getAllValues().size(), 1, "Unexpected count of ChannelHandler added.");
        Assert.assertEquals(handlerCaptorLast.getAllValues().get(0).getClass(), ImapClientConnectHandler.class, "expected class mismatched.");
        // verify logging messages
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.eq("[{},{}] connect operationComplete. result={}, imapServerUri={}, sniNames={}"),
                Mockito.eq(Long.valueOf(2)), Mockito.eq("someUserId"), Mockito.eq("success"), Mockito.eq("imaps://one.two.three.com:993"),
                Mockito.eq(Collections.singletonList("one.two.three.com")));
    }

    /**
     * Tests createSession method when successful.
     *
     * @throws SSLException will not throw
     * @throws URISyntaxException will not throw
     * @throws Exception when calling operationComplete() at GenericFutureListener
     */
    @Test
    public void testCreateSessionNoLocalAddressConnectFailed() throws SSLException, URISyntaxException, Exception {

        final Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        final ChannelFuture nettyConnectFuture = Mockito.mock(ChannelFuture.class);
        Mockito.when(nettyConnectFuture.isSuccess()).thenReturn(false);
        final Channel nettyChannel = Mockito.mock(Channel.class);
        final ChannelPipeline nettyPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(nettyChannel.pipeline()).thenReturn(nettyPipeline);
        Mockito.when(nettyConnectFuture.channel()).thenReturn(nettyChannel);
        Mockito.when(bootstrap.connect(Mockito.anyString(), Mockito.anyInt())).thenReturn(nettyConnectFuture);

        final EventLoopGroup group = Mockito.mock(EventLoopGroup.class);
        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        final ImapAsyncClient aclient = new ImapAsyncClient(clock, bootstrap, group, logger);

        final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
        config.setConnectionTimeoutMillis(5000);
        config.setReadTimeoutMillis(6000);
        final List<String> sniNames = null;

        // test create session
        final InetSocketAddress localAddress = null;
        final URI serverUri = new URI(SERVER_URI_STR);
        final Future<ImapAsyncCreateSessionResponse> future = aclient.createSession(serverUri, config, localAddress, sniNames, DebugMode.DEBUG_OFF);

        // verify session creation
        Assert.assertNotNull(future, "Future for ImapAsyncSession should not be null.");

        final ArgumentCaptor<ImapClientChannelInitializer> initializerCaptor = ArgumentCaptor.forClass(ImapClientChannelInitializer.class);
        Mockito.verify(bootstrap, Mockito.times(1)).handler(initializerCaptor.capture());
        Assert.assertEquals(initializerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");
        final ImapClientChannelInitializer initializer = initializerCaptor.getAllValues().get(0);

        // should not call this connect
        Mockito.verify(bootstrap, Mockito.times(0)).connect(Mockito.any(SocketAddress.class), Mockito.any(SocketAddress.class));
        // should call following connect
        Mockito.verify(bootstrap, Mockito.times(1)).connect(Mockito.anyString(), Mockito.anyInt());
        final ArgumentCaptor<GenericFutureListener> listenerCaptor = ArgumentCaptor.forClass(GenericFutureListener.class);
        Mockito.verify(nettyConnectFuture, Mockito.times(1)).addListener(listenerCaptor.capture());
        Assert.assertEquals(listenerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");

        // verify GenericFutureListener.operationComplete()
        final GenericFutureListener listener = listenerCaptor.getAllValues().get(0);
        listener.operationComplete(nettyConnectFuture);
        final ArgumentCaptor<ChannelHandler> handlerCaptorFirst = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(0)).addFirst(Mockito.anyString(), handlerCaptorFirst.capture());
        Assert.assertEquals(handlerCaptorFirst.getAllValues().size(), 0, "number of handlers mismatched.");

        final ArgumentCaptor<ChannelHandler> handlerCaptorLast = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(0)).addLast(Mockito.anyString(), handlerCaptorLast.capture());
        Assert.assertEquals(handlerCaptorLast.getAllValues().size(), 0, "Unexpected count of ChannelHandler added.");
        // verify logging messages
        Mockito.verify(logger, Mockito.times(1)).error(Mockito.eq("[{},{}] connect operationComplete. result={}, imapServerUri={}, sniNames={}"),
                Mockito.eq("NA"), Mockito.eq("NA"), Mockito.eq("failure"), Mockito.eq("imaps://one.two.three.com:993"), Mockito.eq(null),
                Mockito.isA(ImapAsyncClientException.class));

    }

    /**
     * Tests createSession method with unknown host exception.
     *
     * @throws SSLException will not throw
     * @throws URISyntaxException will not throw
     * @throws Exception when calling operationComplete() at GenericFutureListener
     */
    @Test
    public void testCreateSessionUnknownHostConnectFailed() throws SSLException, URISyntaxException, Exception {

        final Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        final ChannelFuture nettyConnectFuture = Mockito.mock(ChannelFuture.class);
        Mockito.when(nettyConnectFuture.isSuccess()).thenReturn(false);
        final Channel nettyChannel = Mockito.mock(Channel.class);
        Mockito.when(nettyChannel.isActive()).thenReturn(false);
        final ChannelPipeline nettyPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(nettyChannel.pipeline()).thenReturn(nettyPipeline);
        Mockito.when(nettyConnectFuture.channel()).thenReturn(nettyChannel);
        Mockito.when(nettyConnectFuture.cause()).thenReturn(new UnknownHostException("Unknown host"));
        Mockito.when(bootstrap.connect(Mockito.anyString(), Mockito.anyInt())).thenReturn(nettyConnectFuture);

        final EventLoopGroup group = Mockito.mock(EventLoopGroup.class);
        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        final ImapAsyncClient aclient = new ImapAsyncClient(clock, bootstrap, group, logger);

        final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
        config.setConnectionTimeoutMillis(5000);
        config.setReadTimeoutMillis(6000);
        final List<String> sniNames = null;

        // test create session
        final InetSocketAddress localAddress = null;
        final URI serverUri = new URI(SERVER_URI_STR);
        final Future<ImapAsyncCreateSessionResponse> future = aclient.createSession(serverUri, config, localAddress, sniNames, DebugMode.DEBUG_OFF);

        // verify session creation
        Assert.assertNotNull(future, "Future for ImapAsyncSession should not be null.");

        final ArgumentCaptor<ImapClientChannelInitializer> initializerCaptor = ArgumentCaptor.forClass(ImapClientChannelInitializer.class);
        Mockito.verify(bootstrap, Mockito.times(1)).handler(initializerCaptor.capture());
        Assert.assertEquals(initializerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");
        final ImapClientChannelInitializer initializer = initializerCaptor.getAllValues().get(0);

        // should not call this connect
        Mockito.verify(bootstrap, Mockito.times(0)).connect(Mockito.any(SocketAddress.class), Mockito.any(SocketAddress.class));
        // should call following connect
        Mockito.verify(bootstrap, Mockito.times(1)).connect(Mockito.anyString(), Mockito.anyInt());
        final ArgumentCaptor<GenericFutureListener> listenerCaptor = ArgumentCaptor.forClass(GenericFutureListener.class);
        Mockito.verify(nettyConnectFuture, Mockito.times(1)).addListener(listenerCaptor.capture());
        Assert.assertEquals(listenerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");

        // verify GenericFutureListener.operationComplete()
        final GenericFutureListener listener = listenerCaptor.getAllValues().get(0);
        listener.operationComplete(nettyConnectFuture);
        final ArgumentCaptor<ChannelHandler> handlerCaptorFirst = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(0)).addFirst(Mockito.anyString(), handlerCaptorFirst.capture());
        Assert.assertEquals(handlerCaptorFirst.getAllValues().size(), 0, "number of handlers mismatched.");

        final ArgumentCaptor<ChannelHandler> handlerCaptorLast = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(0)).addLast(Mockito.anyString(), handlerCaptorLast.capture());
        Assert.assertEquals(handlerCaptorLast.getAllValues().size(), 0, "Unexpected count of ChannelHandler added.");
        // verify logging messages
        Mockito.verify(logger, Mockito.times(1)).error(Mockito.eq("[{},{}] connect operationComplete. result={}, imapServerUri={}, sniNames={}"),
                Mockito.eq("NA"), Mockito.eq("NA"), Mockito.eq("failure"), Mockito.eq("imaps://one.two.three.com:993"), Mockito.eq(null),
                Mockito.isA(ImapAsyncClientException.class));

        Assert.assertTrue(future.isDone(), "Future should be done.");
        try {
            future.get(5, TimeUnit.MILLISECONDS);
            Assert.fail("Should throw unknown host exception");
        } catch (final ExecutionException | InterruptedException ex) {
            Assert.assertNotNull(ex, "Expect exception to be thrown.");
            Assert.assertNotNull(ex.getCause(), "Expect cause.");
            Assert.assertEquals(ex.getClass(), ExecutionException.class, "Class type mismatch.");
            final Exception exception = (Exception) ex.getCause();
            Assert.assertEquals(exception.getClass(), ImapAsyncClientException.class, "Exception class type mismatch.");
            Assert.assertNotNull(exception.getCause(), "Cause should not be null");
            Assert.assertEquals(exception.getCause().getClass(), UnknownHostException.class, "Cause should be unknown host exception");
            Assert.assertSame(exception.getCause(), nettyConnectFuture.cause(), "Cause should be same object");
            Assert.assertEquals(((ImapAsyncClientException) exception).getFailureType(), FailureType.UNKNOWN_HOST_EXCEPTION,
                    "Exception type should be UNKNOWN_HOST_EXCEPTION");
        }

        Mockito.verify(nettyChannel, Mockito.times(1)).isActive();
        Mockito.verify(nettyChannel, Mockito.times(0)).close(); // since channel is not active
    }

    /**
     * Tests createSession method with ConnectTimeout exception.
     *
     * @throws SSLException will not throw
     * @throws URISyntaxException will not throw
     * @throws Exception when calling operationComplete() at GenericFutureListener
     */
    @Test
    public void testCreateSessionConnectionTimeoutFailed() throws SSLException, URISyntaxException, Exception {

        final Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        final ChannelFuture nettyConnectFuture = Mockito.mock(ChannelFuture.class);
        Mockito.when(nettyConnectFuture.isSuccess()).thenReturn(false);
        final Channel nettyChannel = Mockito.mock(Channel.class);
        final ChannelPipeline nettyPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(nettyChannel.pipeline()).thenReturn(nettyPipeline);
        Mockito.when(nettyChannel.isActive()).thenReturn(true);
        Mockito.when(nettyConnectFuture.channel()).thenReturn(nettyChannel);
        Mockito.when(nettyConnectFuture.cause()).thenReturn(new ConnectTimeoutException("connection timed out"));
        Mockito.when(bootstrap.connect(Mockito.anyString(), Mockito.anyInt())).thenReturn(nettyConnectFuture);

        final EventLoopGroup group = Mockito.mock(EventLoopGroup.class);
        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        final ImapAsyncClient aclient = new ImapAsyncClient(clock, bootstrap, group, logger);

        final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
        config.setConnectionTimeoutMillis(5000);
        config.setReadTimeoutMillis(6000);
        final List<String> sniNames = null;

        // test create session
        final InetSocketAddress localAddress = null;
        final URI serverUri = new URI(SERVER_URI_STR);
        final Future<ImapAsyncCreateSessionResponse> future = aclient.createSession(serverUri, config, localAddress, sniNames, DebugMode.DEBUG_OFF);

        // verify session creation
        Assert.assertNotNull(future, "Future for ImapAsyncSession should not be null.");

        final ArgumentCaptor<ImapClientChannelInitializer> initializerCaptor = ArgumentCaptor.forClass(ImapClientChannelInitializer.class);
        Mockito.verify(bootstrap, Mockito.times(1)).handler(initializerCaptor.capture());
        Assert.assertEquals(initializerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");
        final ImapClientChannelInitializer initializer = initializerCaptor.getAllValues().get(0);

        // should not call this connect
        Mockito.verify(bootstrap, Mockito.times(0)).connect(Mockito.any(SocketAddress.class), Mockito.any(SocketAddress.class));
        // should call following connect
        Mockito.verify(bootstrap, Mockito.times(1)).connect(Mockito.anyString(), Mockito.anyInt());
        final ArgumentCaptor<GenericFutureListener> listenerCaptor = ArgumentCaptor.forClass(GenericFutureListener.class);
        Mockito.verify(nettyConnectFuture, Mockito.times(1)).addListener(listenerCaptor.capture());
        Assert.assertEquals(listenerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");

        // verify GenericFutureListener.operationComplete()
        final GenericFutureListener listener = listenerCaptor.getAllValues().get(0);
        listener.operationComplete(nettyConnectFuture);
        final ArgumentCaptor<ChannelHandler> handlerCaptorFirst = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(0)).addFirst(Mockito.anyString(), handlerCaptorFirst.capture());
        Assert.assertEquals(handlerCaptorFirst.getAllValues().size(), 0, "number of handlers mismatched.");

        final ArgumentCaptor<ChannelHandler> handlerCaptorLast = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(0)).addLast(Mockito.anyString(), handlerCaptorLast.capture());
        Assert.assertEquals(handlerCaptorLast.getAllValues().size(), 0, "Unexpected count of ChannelHandler added.");
        // verify logging messages
        Mockito.verify(logger, Mockito.times(1)).error(Mockito.eq("[{},{}] connect operationComplete. result={}, imapServerUri={}, sniNames={}"),
                Mockito.eq("NA"), Mockito.eq("NA"), Mockito.eq("failure"), Mockito.eq("imaps://one.two.three.com:993"), Mockito.eq(null),
                Mockito.isA(ImapAsyncClientException.class));

        Assert.assertTrue(future.isDone(), "Future should be done.");
        try {
            future.get(5, TimeUnit.MILLISECONDS);
            Assert.fail("Should throw connect timeout exception");
        } catch (final ExecutionException | InterruptedException ex) {
            Assert.assertNotNull(ex, "Expect exception to be thrown.");
            Assert.assertNotNull(ex.getCause(), "Expect cause.");
            Assert.assertEquals(ex.getClass(), ExecutionException.class, "Class type mismatch.");
            final Exception exception = (Exception) ex.getCause();
            Assert.assertEquals(exception.getClass(), ImapAsyncClientException.class, "exception type mismatch." + ex);
            Assert.assertNotNull(exception.getCause(), "Cause should not be null");
            Assert.assertEquals(exception.getCause().getClass(), ConnectTimeoutException.class, "Cause should be connection timeout exception");
            Assert.assertSame(exception.getCause(), nettyConnectFuture.cause(), "Cause should be same object");
            Assert.assertEquals(((ImapAsyncClientException) exception).getFailureType(), FailureType.CONNECTION_TIMEOUT_EXCEPTION,
                    "Exception type should be CONNECTION_TIMEOUT_EXCEPTION");
        }
        Mockito.verify(nettyChannel, Mockito.times(1)).isActive();
        Mockito.verify(nettyChannel, Mockito.times(1)).close();
    }

    /**
     * Tests createSession method with ConnectTimeout exception.
     *
     * @throws SSLException will not throw
     * @throws URISyntaxException will not throw
     * @throws Exception when calling operationComplete() at GenericFutureListener
     */
    @Test
    public void testCreateSessionConnectionTimeoutFailedChannelIsNull() throws SSLException, URISyntaxException, Exception {

        final Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        final ChannelFuture nettyConnectFuture = Mockito.mock(ChannelFuture.class);
        Mockito.when(nettyConnectFuture.isSuccess()).thenReturn(false);

        final ChannelPipeline nettyPipeline = Mockito.mock(ChannelPipeline.class);

        Mockito.when(nettyConnectFuture.cause()).thenReturn(new ConnectTimeoutException("connection timed out"));
        Mockito.when(bootstrap.connect(Mockito.anyString(), Mockito.anyInt())).thenReturn(nettyConnectFuture);

        final EventLoopGroup group = Mockito.mock(EventLoopGroup.class);
        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        final ImapAsyncClient aclient = new ImapAsyncClient(clock, bootstrap, group, logger);

        final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
        config.setConnectionTimeoutMillis(5000);
        config.setReadTimeoutMillis(6000);
        final List<String> sniNames = null;

        // test create session
        final InetSocketAddress localAddress = null;
        final URI serverUri = new URI(SERVER_URI_STR);
        final Future<ImapAsyncCreateSessionResponse> future = aclient.createSession(serverUri, config, localAddress, sniNames, DebugMode.DEBUG_OFF);

        // verify session creation
        Assert.assertNotNull(future, "Future for ImapAsyncSession should not be null.");

        final ArgumentCaptor<ImapClientChannelInitializer> initializerCaptor = ArgumentCaptor.forClass(ImapClientChannelInitializer.class);
        Mockito.verify(bootstrap, Mockito.times(1)).handler(initializerCaptor.capture());
        Assert.assertEquals(initializerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");
        final ImapClientChannelInitializer initializer = initializerCaptor.getAllValues().get(0);

        // should not call this connect
        Mockito.verify(bootstrap, Mockito.times(0)).connect(Mockito.any(SocketAddress.class), Mockito.any(SocketAddress.class));
        // should call following connect
        Mockito.verify(bootstrap, Mockito.times(1)).connect(Mockito.anyString(), Mockito.anyInt());
        final ArgumentCaptor<GenericFutureListener> listenerCaptor = ArgumentCaptor.forClass(GenericFutureListener.class);
        Mockito.verify(nettyConnectFuture, Mockito.times(1)).addListener(listenerCaptor.capture());
        Assert.assertEquals(listenerCaptor.getAllValues().size(), 1, "Unexpected count of ImapClientChannelInitializer.");

        // verify GenericFutureListener.operationComplete()
        final GenericFutureListener listener = listenerCaptor.getAllValues().get(0);
        listener.operationComplete(nettyConnectFuture);
        final ArgumentCaptor<ChannelHandler> handlerCaptorFirst = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(0)).addFirst(Mockito.anyString(), handlerCaptorFirst.capture());
        Assert.assertEquals(handlerCaptorFirst.getAllValues().size(), 0, "number of handlers mismatched.");

        final ArgumentCaptor<ChannelHandler> handlerCaptorLast = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(nettyPipeline, Mockito.times(0)).addLast(Mockito.anyString(), handlerCaptorLast.capture());
        Assert.assertEquals(handlerCaptorLast.getAllValues().size(), 0, "Unexpected count of ChannelHandler added.");
        // verify logging messages
        Mockito.verify(logger, Mockito.times(1)).error(Mockito.eq("[{},{}] connect operationComplete. result={}, imapServerUri={}, sniNames={}"),
                Mockito.eq("NA"), Mockito.eq("NA"), Mockito.eq("failure"), Mockito.eq("imaps://one.two.three.com:993"), Mockito.eq(null),
                Mockito.isA(ImapAsyncClientException.class));

        Assert.assertTrue(future.isDone(), "Future should be done.");
        ImapAsyncClientException actual = null;
        try {
            future.get(5, TimeUnit.MILLISECONDS);
            Assert.fail("Should throw connect timeout exception");
        } catch (final ExecutionException | InterruptedException ex) {
            Assert.assertNotNull(ex, "Expect exception to be thrown.");
            Assert.assertNotNull(ex.getCause(), "Expect cause.");
            Assert.assertEquals(ex.getClass(), ExecutionException.class, "Class type mismatch.");
            final Exception exception = (Exception) ex.getCause();
            Assert.assertEquals(exception.getClass(), ImapAsyncClientException.class, "exception type mismatch." + ex);
            actual = (ImapAsyncClientException) exception;
        }

        Assert.assertNotNull(actual.getCause(), "Cause should not be null");
        Assert.assertEquals(actual.getCause().getClass(), ConnectTimeoutException.class, "Cause should be connection timeout exception");
        Assert.assertSame(actual.getCause(), nettyConnectFuture.cause(), "Cause should be same object");
        Assert.assertEquals(actual.getFailureType(), FailureType.CONNECTION_TIMEOUT_EXCEPTION,
                "Exception type should be CONNECTION_TIMEOUT_EXCEPTION");
    }

}