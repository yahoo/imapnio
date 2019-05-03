package com.lafaspot.imapnio.async.client;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.SSLException;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.async.client.ImapAsyncClient;
import com.lafaspot.imapnio.async.client.ImapAsyncSession;
import com.lafaspot.imapnio.async.client.ImapClientConnectHandler;
import com.lafaspot.imapnio.async.client.ImapFuture;
import com.lafaspot.imapnio.async.client.ImapAsyncClient.ImapClientChannelInitializer;
import com.lafaspot.imapnio.client.ImapClientRespReader;
import com.lafaspot.imapnio.command.ImapClientRespDecoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Unit test for {@code ImapAsyncClient}.
 */
public class ImapAsyncClientTest {

    /** Timeout for connection. */
    private static final String CONNECTION_TIMEOUT = "mail.imap.connectiontimeout";

    /** Timeout for connection. */
    private static final String SERVER_URI_STR = "imaps://one.two.three.yahoo.com:993";

    /** Timeout for connection. */
    private static final String NO_SSL_SERVER_URI_STR = "imap://one.two.three.yahoo.com:993";

    /**
     * Tests createSession method when successful.
     *
     * @throws SSLException will not throw
     * @throws URISyntaxException will not throw
     * @throws Exception when calling operationComplete() at GenericFutureListenr
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
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        final ImapAsyncClient aclient = new ImapAsyncClient(bootstrap, group, logger);


        final Properties properties = new Properties();
        properties.put(CONNECTION_TIMEOUT, String.valueOf("5000"));
        properties.put("mail.imap.timeout", String.valueOf("60000"));
        properties.put("mail.imap.inactivity", "3");
        final List<String> sniNames = null;

        // test create sesssion
        final InetSocketAddress localAddress = null;
        final URI serverUri = new URI(SERVER_URI_STR);
        final ImapFuture<ImapAsyncSession> future = aclient.createSession(serverUri, properties, localAddress, sniNames);

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

        // test connection estabalished and channel initialized new
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
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        // call shutdown
        aclient.shutdown();
        Mockito.verify(group, Mockito.times(1)).shutdownGracefully();
    }

    /**
     * Tests createSession method when successful.
     *
     * @throws SSLException will not throw
     * @throws URISyntaxException will not throw
     * @throws Exception when calling operationComplete() at GenericFutureListenr
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
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        final ImapAsyncClient aclient = new ImapAsyncClient(bootstrap, group, logger);

        final Properties properties = new Properties();
        properties.put(CONNECTION_TIMEOUT, String.valueOf("5000"));
        properties.put("mail.imap.timeout", String.valueOf("60000"));
        properties.put("mail.imap.inactivity", "3");
        final List<String> sniNames = null;

        // test create sesssion
        final InetSocketAddress localAddress = null;
        final URI serverUri = new URI(NO_SSL_SERVER_URI_STR);
        final ImapFuture<ImapAsyncSession> future = aclient.createSession(serverUri, properties, localAddress, sniNames);

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

        // test connection estabalished and channel initialized new
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
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        // call shutdown
        aclient.shutdown();
        Mockito.verify(group, Mockito.times(1)).shutdownGracefully();
    }

    /**
     * Tests createSession method when successful.
     *
     * @throws SSLException will not throw
     * @throws URISyntaxException will not throw
     * @throws Exception when calling operationComplete() at GenericFutureListenr
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
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        final ImapAsyncClient aclient = new ImapAsyncClient(bootstrap, group, logger);

        final Properties properties = new Properties();
        properties.put(CONNECTION_TIMEOUT, String.valueOf("5000"));
        properties.put("mail.imap.timeout", String.valueOf("60000"));
        properties.put("mail.imap.inactivity", "3");
        final List<String> sniNames = new ArrayList<String>();

        // test create sesssion
        final InetSocketAddress localAddress = null;
        final URI serverUri = new URI(SERVER_URI_STR);
        final ImapFuture<ImapAsyncSession> future = aclient.createSession(serverUri, properties, localAddress, sniNames);

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

        // test connection estabalished and channel initialized new
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
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    /**
     * Tests createSession method when successful.
     *
     * @throws SSLException will not throw
     * @throws URISyntaxException will not throw
     * @throws Exception when calling operationComplete() at GenericFutureListenr
     */
    @Test
    public void testCreateSessionWithLocalAddressSniSuccessful() throws SSLException, URISyntaxException, Exception {

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

        final ImapAsyncClient aclient = new ImapAsyncClient(bootstrap, group, logger);

        final Properties properties = new Properties();
        properties.put(CONNECTION_TIMEOUT, String.valueOf("5000"));
        properties.put("mail.imap.timeout", String.valueOf("60000"));
        properties.put("mail.imap.inactivity", "3");
        final List<String> sniNames = new ArrayList<String>();
        sniNames.add("imap.mail.yahoo.com");
        // test create sesssion
        final InetSocketAddress localAddress = new InetSocketAddress("10.10.10.10", 23112);
        final URI serverUri = new URI(SERVER_URI_STR);
        final ImapFuture<ImapAsyncSession> future = aclient.createSession(serverUri, properties, localAddress, sniNames);

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

        // test connection estabalished and channel initialized new
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
     * Tests createSession method when successful.
     *
     * @throws SSLException will not throw
     * @throws URISyntaxException will not throw
     * @throws Exception when calling operationComplete() at GenericFutureListenr
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

        final ImapAsyncClient aclient = new ImapAsyncClient(bootstrap, group, logger);

        final Properties properties = new Properties();
        properties.put(CONNECTION_TIMEOUT, String.valueOf("5000"));
        properties.put("mail.imap.timeout", String.valueOf("60000"));
        properties.put("mail.imap.inactivity", "3");
        final List<String> sniNames = null;

        // test create sesssion
        final InetSocketAddress localAddress = null;
        final URI serverUri = new URI(SERVER_URI_STR);
        final ImapFuture<ImapAsyncSession> future = aclient.createSession(serverUri, properties, localAddress, sniNames);

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
        Mockito.verify(logger, Mockito.times(1)).error(Mockito.anyString(), Mockito.anyString(), Mockito.any(Throwable.class));
    }

}