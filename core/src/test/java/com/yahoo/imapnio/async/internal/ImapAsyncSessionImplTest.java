package com.yahoo.imapnio.async.internal;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.mail.search.SearchException;

import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.client.ImapAsyncClient;
import com.yahoo.imapnio.async.client.ImapAsyncSession.DebugMode;
import com.yahoo.imapnio.async.client.ImapFuture;
import com.yahoo.imapnio.async.data.Capability;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;
import com.yahoo.imapnio.async.internal.ImapAsyncSessionImpl.ImapChannelClosedListener;
import com.yahoo.imapnio.async.request.AuthPlainCommand;
import com.yahoo.imapnio.async.request.CapaCommand;
import com.yahoo.imapnio.async.request.IdleCommand;
import com.yahoo.imapnio.async.request.ImapRequest;
import com.yahoo.imapnio.async.response.ImapAsyncResponse;
import com.yahoo.imapnio.async.response.ImapResponseMapper;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.compression.JdkZlibDecoder;
import io.netty.handler.codec.compression.JdkZlibEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * Unit test for {@code ImapAsyncSessionImpl}.
 */
public class ImapAsyncSessionImplTest {

    /** Dummy session id. */
    private static final int SESSION_ID = 123456;

    /** Timeout in milliseconds for making get on future. */
    private static final long FUTURE_GET_TIMEOUT_MILLIS = 5L;

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> c = ImapAsyncSessionImpl.class;
        fieldsToCheck = new HashSet<>();

        for (final Field declaredField : c.getDeclaredFields()) {
            if (!declaredField.getType().isPrimitive() && !Modifier.isStatic(declaredField.getModifiers())) {
                declaredField.setAccessible(true);
                fieldsToCheck.add(declaredField);
            }
        }
    }

    /**
     * Tests the whole life cycle flow: construct the session, execute, handle server response success, close session.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testExecuteAuthCapaAndFlushHandleResponseCloseSessionAllSuccess() throws ImapAsyncClientException, IOException, ProtocolException,
            InterruptedException, ExecutionException, TimeoutException, SearchException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise authWritePromise = Mockito.mock(ChannelPromise.class); // first
        final ChannelPromise authWritePromise2 = Mockito.mock(ChannelPromise.class); // after +
        final ChannelPromise capaWritePromise = Mockito.mock(ChannelPromise.class);
        final ChannelPromise closePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(authWritePromise).thenReturn(authWritePromise2).thenReturn(capaWritePromise)
                .thenReturn(closePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(false);

        // construct, both class level and session level debugging are off
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_OFF, SESSION_ID, pipeline);

        // execute Authenticate plain command
        {
            final Map<String, List<String>> capas = new HashMap<String, List<String>>();
            final ImapRequest cmd = new AuthPlainCommand("orange", "juicy", new Capability(capas));
            final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);
            Mockito.verify(authWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
            Mockito.verify(logger, Mockito.times(0)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

            // simulate write to server completed successfully
            Mockito.when(authWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(authWritePromise);

            // handle server response
            final IMAPResponse serverResp1 = new IMAPResponse("+");
            // following will call getNextCommandLineAfterContinuation
            aSession.handleChannelResponse(serverResp1);
            Mockito.verify(channel, Mockito.times(2)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));

            final IMAPResponse serverResp2 = new IMAPResponse("a1 OK AUTHENTICATE completed");
            aSession.handleChannelResponse(serverResp2);

            // verify that future should be done now
            Assert.assertTrue(future.isDone(), "isDone() should be true now");
            final ImapAsyncResponse asyncResp = future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            final Collection<IMAPResponse> lines = asyncResp.getResponseLines();
            Assert.assertEquals(lines.size(), 2, "responses count mismatched.");
            final Iterator<IMAPResponse> it = lines.iterator();
            final IMAPResponse continuationResp = it.next();
            Assert.assertNotNull(continuationResp, "Result mismatched.");
            Assert.assertTrue(continuationResp.isContinuation(), "Response.isContinuation() mismatched.");
            final IMAPResponse endingResp = it.next();
            Assert.assertNotNull(endingResp, "Result mismatched.");
            Assert.assertTrue(endingResp.isOK(), "Response.isOK() mismatched.");
            Assert.assertEquals(endingResp.getTag(), "a1", "tag mismatched.");
            // verify no log messages
            Mockito.verify(logger, Mockito.times(0)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        }

        {
            // setting debug on for the session
            Mockito.when(logger.isDebugEnabled()).thenReturn(true);
            aSession.setDebugMode(DebugMode.DEBUG_ON);
            // execute capa
            final ImapRequest cmd = new CapaCommand();
            final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);

            Mockito.verify(capaWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(3)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
            Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

            // simulate write to server completed successfully
            Mockito.when(capaWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(capaWritePromise);

            // handle server response
            final IMAPResponse serverResp1 = new IMAPResponse(
                    "* CAPABILITY IMAP4rev1 SASL-IR AUTH=PLAIN AUTH=XOAUTH2 AUTH=OAUTHBEARER ID MOVE NAMESPACE");
            aSession.handleChannelResponse(serverResp1);
            final IMAPResponse serverResp2 = new IMAPResponse("a2 OK CAPABILITY completed");
            aSession.handleChannelResponse(serverResp2);

            // verify that future should be done now
            Assert.assertTrue(future.isDone(), "isDone() should be true now");
            final ImapAsyncResponse asyncResp = future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            final Collection<IMAPResponse> lines = asyncResp.getResponseLines();
            Assert.assertEquals(lines.size(), 2, "responses count mismatched.");
            final Iterator<IMAPResponse> it = lines.iterator();
            final IMAPResponse capaResp = it.next();
            Assert.assertNotNull(capaResp, "Result mismatched.");
            Assert.assertFalse(capaResp.isContinuation(), "Response.isContinuation() mismatched.");
            final ImapResponseMapper parser = new ImapResponseMapper();
            final Capability capa = parser.readValue(lines.toArray(new IMAPResponse[0]), Capability.class);
            Assert.assertTrue(capa.hasCapability("IMAP4rev1".toUpperCase()), "One capability missed.");
            Assert.assertTrue(capa.hasCapability("SASL-IR"), "One capability missed.");
            Assert.assertTrue(capa.hasCapability("ID"), "One capability missed.");
            Assert.assertTrue(capa.hasCapability("MOVE"), "One capability missed.");
            Assert.assertTrue(capa.hasCapability("NAMESPACE"), "One capability missed.");
            final List<String> authValues = capa.getCapability("AUTH");
            Assert.assertNotNull(authValues, "One Auth value missed.");
            Assert.assertEquals(authValues.size(), 3, "One Auth value missed");
            Assert.assertEquals(authValues.get(0), "PLAIN", "One Auth value missed");
            Assert.assertEquals(authValues.get(1), "XOAUTH2", "One Auth value missed");
            Assert.assertEquals(authValues.get(2), "OAUTHBEARER", "One Auth value missed");

            final IMAPResponse endingResp = it.next();
            Assert.assertNotNull(endingResp, "Result mismatched.");
            Assert.assertTrue(endingResp.isOK(), "Response.isOK() mismatched.");
            Assert.assertEquals(endingResp.getTag(), "a2", "tag mismatched.");// verify logging messages
            final ArgumentCaptor<String> logCapture = ArgumentCaptor.forClass(String.class);
            Mockito.verify(logger, Mockito.times(3)).debug(Mockito.anyString(), Mockito.anyString(), logCapture.capture());
            final List<String> logMsgs = logCapture.getAllValues();
            Assert.assertNotNull(logMsgs, "log messages mismatched.");
            Assert.assertEquals(logMsgs.size(), 3, "log messages mismatched.");
            Assert.assertEquals(logMsgs.get(0), "a2 CAPABILITY\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logMsgs.get(1), "* CAPABILITY IMAP4rev1 SASL-IR AUTH=PLAIN AUTH=XOAUTH2 AUTH=OAUTHBEARER ID MOVE NAMESPACE",
                    "log messages from server mismatched.");
            Assert.assertEquals(logMsgs.get(2), "a2 OK CAPABILITY completed", "log messages from server mismatched.");
        }

        // perform close session
        Mockito.when(closePromise.isSuccess()).thenReturn(true);
        final ImapFuture<Boolean> closeFuture = aSession.close();
        final ArgumentCaptor<ImapChannelClosedListener> listenerCaptor = ArgumentCaptor.forClass(ImapChannelClosedListener.class);
        Mockito.verify(closePromise, Mockito.times(1)).addListener(listenerCaptor.capture());
        Assert.assertEquals(listenerCaptor.getAllValues().size(), 1, "Unexpected count of ImapChannelClosedListener.");
        final ImapChannelClosedListener closeListener = listenerCaptor.getAllValues().get(0);
        closeListener.operationComplete(closePromise);
        // close future should be done successfully
        Assert.assertTrue(closeFuture.isDone(), "close future should be done");
        final Boolean closeResp = closeFuture.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(closeResp, "close() response mismatched.");
        Assert.assertTrue(closeResp, "close() response mismatched.");
    }

    /**
     * Tests when server responses CompressCommand success and there is no ssl handler.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testExecuteAuthCompressHandleResponseNoSslHandler() throws ImapAsyncClientException, IOException, ProtocolException,
            InterruptedException, ExecutionException, TimeoutException, SearchException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise authWritePromise = Mockito.mock(ChannelPromise.class); // first
        final ChannelPromise authWritePromise2 = Mockito.mock(ChannelPromise.class); // after +
        final ChannelPromise compressWritePromise = Mockito.mock(ChannelPromise.class);
        final ChannelPromise closePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(authWritePromise).thenReturn(authWritePromise2).thenReturn(compressWritePromise)
                .thenReturn(closePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isTraceEnabled()).thenReturn(true);

        // construct, class level debug is enabled, session level debug is disabled
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_OFF, SESSION_ID, pipeline);

        // execute Authenticate plain command
        {
            final Map<String, List<String>> capas = new HashMap<String, List<String>>();
            final ImapRequest cmd = new AuthPlainCommand("orange", "juicy", new Capability(capas));
            final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);
            Mockito.verify(authWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));

            // simulate write to server completed successfully
            Mockito.when(authWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(authWritePromise);

            // handle server response
            final IMAPResponse serverResp1 = new IMAPResponse("+");
            // following will call getNextCommandLineAfterContinuation
            aSession.handleChannelResponse(serverResp1);
            Mockito.verify(channel, Mockito.times(2)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));

            final IMAPResponse serverResp2 = new IMAPResponse("a1 OK AUTHENTICATE completed");
            aSession.handleChannelResponse(serverResp2);

            // verify that future should be done now
            Assert.assertTrue(future.isDone(), "isDone() should be true now");
            final ImapAsyncResponse asyncResp = future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            final Collection<IMAPResponse> lines = asyncResp.getResponseLines();
            Assert.assertEquals(lines.size(), 2, "responses count mismatched.");
            final Iterator<IMAPResponse> it = lines.iterator();
            final IMAPResponse continuationResp = it.next();
            Assert.assertNotNull(continuationResp, "Result mismatched.");
            Assert.assertTrue(continuationResp.isContinuation(), "Response.isContinuation() mismatched.");
            final IMAPResponse endingResp = it.next();
            Assert.assertNotNull(endingResp, "Result mismatched.");
            Assert.assertTrue(endingResp.isOK(), "Response.isOK() mismatched.");
            Assert.assertEquals(endingResp.getTag(), "a1", "tag mismatched.");
            // verify logging messages
            final ArgumentCaptor<String> logCapture = ArgumentCaptor.forClass(String.class);
            Mockito.verify(logger, Mockito.times(3)).debug(Mockito.anyString(), Mockito.anyString(), logCapture.capture());
            final List<String> logMsgs = logCapture.getAllValues();
            Assert.assertNotNull(logMsgs, "log messages mismatched.");
            Assert.assertEquals(logMsgs.size(), 3, "log messages mismatched.");
            Assert.assertEquals(logMsgs.get(0), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logMsgs.get(1), "+", "log messages from server mismatched.");
            Assert.assertEquals(logMsgs.get(2), "a1 OK AUTHENTICATE completed", "log messages from server mismatched.");
        }

        {
            // start Compression
            final ImapFuture<ImapAsyncResponse> future = aSession.startCompression();

            Mockito.verify(compressWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(3)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));

            // simulate write to server completed successfully
            Mockito.when(compressWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(compressWritePromise);

            // handle server response
            final IMAPResponse serverResp1 = new IMAPResponse("a2 OK Success");
            aSession.handleChannelResponse(serverResp1);

            Mockito.verify(pipeline, Mockito.times(1)).addFirst(Matchers.eq("DEFLATER"), Matchers.isA(JdkZlibDecoder.class));
            Mockito.verify(pipeline, Mockito.times(1)).addFirst(Matchers.eq("INFLATER"), Matchers.isA(JdkZlibEncoder.class));
            // verify that future should be done now
            Assert.assertTrue(future.isDone(), "isDone() should be true now");
            final ImapAsyncResponse asyncResp = future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            final Collection<IMAPResponse> lines = asyncResp.getResponseLines();
            Assert.assertEquals(lines.size(), 1, "responses count mismatched.");
            final Iterator<IMAPResponse> it = lines.iterator();
            final IMAPResponse compressResp = it.next();
            Assert.assertNotNull(compressResp, "Result mismatched.");
            Assert.assertFalse(compressResp.isContinuation(), "Response.isContinuation() mismatched.");
            Assert.assertTrue(compressResp.isOK(), "Response.isOK() mismatched.");
            Assert.assertEquals(compressResp.getTag(), "a2", "tag mismatched.");
            // verify logging messages
            final ArgumentCaptor<String> logCapture = ArgumentCaptor.forClass(String.class);
            Mockito.verify(logger, Mockito.times(5)).debug(Mockito.anyString(), Mockito.anyString(), logCapture.capture());
            final List<String> logMsgs = logCapture.getAllValues();
            Assert.assertNotNull(logMsgs, "log messages mismatched.");
            Assert.assertEquals(logMsgs.size(), 5, "log messages mismatched.");
            Assert.assertEquals(logMsgs.get(0), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logMsgs.get(1), "+", "log messages from server mismatched.");
            Assert.assertEquals(logMsgs.get(2), "a1 OK AUTHENTICATE completed", "log messages from server mismatched.");
            Assert.assertEquals(logMsgs.get(3), "a2 COMPRESS DEFLATE\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logMsgs.get(4), "a2 OK Success", "log messages from server mismatched.");
        }
    }

    /**
     * Tests when server responses CompressCommand success and there is ssl handler.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testExecuteAuthCompressHandleResponseWithSslHandler() throws ImapAsyncClientException, IOException, ProtocolException,
            InterruptedException, ExecutionException, TimeoutException, SearchException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        final SslHandler sslHandler = Mockito.mock(SslHandler.class);
        Mockito.when(pipeline.get(ImapAsyncClient.SSL_HANDLER)).thenReturn(sslHandler);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise authWritePromise = Mockito.mock(ChannelPromise.class); // first
        final ChannelPromise authWritePromise2 = Mockito.mock(ChannelPromise.class); // after +
        final ChannelPromise compressWritePromise = Mockito.mock(ChannelPromise.class);
        final ChannelPromise closePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(authWritePromise).thenReturn(authWritePromise2).thenReturn(compressWritePromise)
                .thenReturn(closePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // construct, turn on session level debugging by having logger.isDebugEnabled() true and session level debug on
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline);

        // execute Authenticate plain command
        {
            final Map<String, List<String>> capas = new HashMap<String, List<String>>();
            final ImapRequest cmd = new AuthPlainCommand("orange", "juicy", new Capability(capas));
            final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);
            Mockito.verify(authWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));

            // simulate write to server completed successfully
            Mockito.when(authWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(authWritePromise);

            // handle server response
            final IMAPResponse serverResp1 = new IMAPResponse("+");
            // following will call getNextCommandLineAfterContinuation
            aSession.handleChannelResponse(serverResp1);
            Mockito.verify(channel, Mockito.times(2)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));

            final IMAPResponse serverResp2 = new IMAPResponse("a1 OK AUTHENTICATE completed");
            aSession.handleChannelResponse(serverResp2);

            // verify that future should be done now
            Assert.assertTrue(future.isDone(), "isDone() should be true now");
            final ImapAsyncResponse asyncResp = future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            final Collection<IMAPResponse> lines = asyncResp.getResponseLines();
            Assert.assertEquals(lines.size(), 2, "responses count mismatched.");
            final Iterator<IMAPResponse> it = lines.iterator();
            final IMAPResponse continuationResp = it.next();
            Assert.assertNotNull(continuationResp, "Result mismatched.");
            Assert.assertTrue(continuationResp.isContinuation(), "Response.isContinuation() mismatched.");
            final IMAPResponse endingResp = it.next();
            Assert.assertNotNull(endingResp, "Result mismatched.");
            Assert.assertTrue(endingResp.isOK(), "Response.isOK() mismatched.");
            Assert.assertEquals(endingResp.getTag(), "a1", "tag mismatched.");
            // verify logging messages
            final ArgumentCaptor<String> logCapture = ArgumentCaptor.forClass(String.class);
            Mockito.verify(logger, Mockito.times(3)).debug(Mockito.anyString(), Mockito.anyString(), logCapture.capture());
            final List<String> logMsgs = logCapture.getAllValues();
            Assert.assertNotNull(logMsgs, "log messages mismatched.");
            Assert.assertEquals(logMsgs.size(), 3, "log messages mismatched.");
            Assert.assertEquals(logMsgs.get(0), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logMsgs.get(1), "+", "log messages from server mismatched.");
            Assert.assertEquals(logMsgs.get(2), "a1 OK AUTHENTICATE completed", "log messages from server mismatched.");
        }

        {
            // start Compression
            final ImapFuture<ImapAsyncResponse> future = aSession.startCompression();

            Mockito.verify(compressWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(3)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));

            // simulate write to server completed successfully
            Mockito.when(compressWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(compressWritePromise);

            // handle server response
            final IMAPResponse serverResp1 = new IMAPResponse("a2 OK Success");
            aSession.handleChannelResponse(serverResp1);

            Mockito.verify(pipeline, Mockito.times(1)).addAfter(Matchers.eq(ImapAsyncClient.SSL_HANDLER), Matchers.eq("DEFLATER"),
                    Matchers.isA(JdkZlibDecoder.class));
            Mockito.verify(pipeline, Mockito.times(1)).addAfter(Matchers.eq(ImapAsyncClient.SSL_HANDLER), Matchers.eq("INFLATER"),
                    Matchers.isA(JdkZlibEncoder.class));
            // verify that future should be done now
            Assert.assertTrue(future.isDone(), "isDone() should be true now");
            final ImapAsyncResponse asyncResp = future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            final Collection<IMAPResponse> lines = asyncResp.getResponseLines();
            Assert.assertEquals(lines.size(), 1, "responses count mismatched.");
            final Iterator<IMAPResponse> it = lines.iterator();
            final IMAPResponse compressResp = it.next();
            Assert.assertNotNull(compressResp, "Result mismatched.");
            Assert.assertFalse(compressResp.isContinuation(), "Response.isContinuation() mismatched.");
            Assert.assertTrue(compressResp.isOK(), "Response.isOK() mismatched.");
            Assert.assertEquals(compressResp.getTag(), "a2", "tag mismatched.");
            // verify logging messages
            final ArgumentCaptor<String> logCapture = ArgumentCaptor.forClass(String.class);
            Mockito.verify(logger, Mockito.times(5)).debug(Mockito.anyString(), Mockito.anyString(), logCapture.capture());
            final List<String> logMsgs = logCapture.getAllValues();
            Assert.assertNotNull(logMsgs, "log messages mismatched.");
            Assert.assertEquals(logMsgs.size(), 5, "log messages mismatched.");
            Assert.assertEquals(logMsgs.get(0), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logMsgs.get(1), "+", "log messages from server mismatched.");
            Assert.assertEquals(logMsgs.get(2), "a1 OK AUTHENTICATE completed", "log messages from server mismatched.");
            Assert.assertEquals(logMsgs.get(3), "a2 COMPRESS DEFLATE\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logMsgs.get(4), "a2 OK Success", "log messages from server mismatched.");
        }
    }

    /**
     * Tests when server responses CompressCommand fails.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testExecuteAuthCompressFailedHandleResponse() throws ImapAsyncClientException, IOException, ProtocolException, InterruptedException,
            ExecutionException, TimeoutException, SearchException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise authWritePromise = Mockito.mock(ChannelPromise.class); // first
        final ChannelPromise authWritePromise2 = Mockito.mock(ChannelPromise.class); // after +
        final ChannelPromise compressWritePromise = Mockito.mock(ChannelPromise.class);
        final ChannelPromise closePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(authWritePromise).thenReturn(authWritePromise2).thenReturn(compressWritePromise)
                .thenReturn(closePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // construct, turn on session level debugging by having logger.isDebugEnabled() true and session level debug on
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline);

        // execute Authenticate plain command
        {
            final Map<String, List<String>> capas = new HashMap<String, List<String>>();
            final ImapRequest cmd = new AuthPlainCommand("orange", "juicy", new Capability(capas));
            final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);
            Mockito.verify(authWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
            Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

            // simulate write to server completed successfully
            Mockito.when(authWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(authWritePromise);

            // handle server response
            final IMAPResponse serverResp1 = new IMAPResponse("+");
            // following will call getNextCommandLineAfterContinuation
            aSession.handleChannelResponse(serverResp1);
            Mockito.verify(channel, Mockito.times(2)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));

            final IMAPResponse serverResp2 = new IMAPResponse("a1 OK AUTHENTICATE completed");
            aSession.handleChannelResponse(serverResp2);

            // verify that future should be done now
            Assert.assertTrue(future.isDone(), "isDone() should be true now");
            final ImapAsyncResponse asyncResp = future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            final Collection<IMAPResponse> lines = asyncResp.getResponseLines();
            Assert.assertEquals(lines.size(), 2, "responses count mismatched.");
            final Iterator<IMAPResponse> it = lines.iterator();
            final IMAPResponse continuationResp = it.next();
            Assert.assertNotNull(continuationResp, "Result mismatched.");
            Assert.assertTrue(continuationResp.isContinuation(), "Response.isContinuation() mismatched.");
            final IMAPResponse endingResp = it.next();
            Assert.assertNotNull(endingResp, "Result mismatched.");
            Assert.assertTrue(endingResp.isOK(), "Response.isOK() mismatched.");
            Assert.assertEquals(endingResp.getTag(), "a1", "tag mismatched.");
            // verify logging messages
            final ArgumentCaptor<String> logCapture = ArgumentCaptor.forClass(String.class);
            Mockito.verify(logger, Mockito.times(3)).debug(Mockito.anyString(), Mockito.anyString(), logCapture.capture());
            final List<String> logMsgs = logCapture.getAllValues();
            Assert.assertNotNull(logMsgs, "log messages mismatched.");
            Assert.assertEquals(logMsgs.size(), 3, "log messages mismatched.");
            Assert.assertEquals(logMsgs.get(0), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logMsgs.get(1), "+", "log messages from server mismatched.");
            Assert.assertEquals(logMsgs.get(2), "a1 OK AUTHENTICATE completed", "log messages from server mismatched.");
        }

        {
            // start Compression
            final ImapFuture<ImapAsyncResponse> future = aSession.startCompression();

            Mockito.verify(compressWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(3)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));

            // simulate write to server completed successfully
            Mockito.when(compressWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(compressWritePromise);

            // handle server response
            final IMAPResponse serverResp1 = new IMAPResponse("a2 NO Success");
            aSession.handleChannelResponse(serverResp1);

            Mockito.verify(pipeline, Mockito.times(0)).addFirst(Matchers.eq("DEFLATER"), Matchers.isA(JdkZlibDecoder.class));
            Mockito.verify(pipeline, Mockito.times(0)).addFirst(Matchers.eq("INFLATER"), Matchers.isA(JdkZlibEncoder.class));
            // verify that future should be done now
            Assert.assertTrue(future.isDone(), "isDone() should be true now");
            final ImapAsyncResponse asyncResp = future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            final Collection<IMAPResponse> lines = asyncResp.getResponseLines();
            Assert.assertEquals(lines.size(), 1, "responses count mismatched.");
            final Iterator<IMAPResponse> it = lines.iterator();
            final IMAPResponse compressResp = it.next();
            Assert.assertNotNull(compressResp, "Result mismatched.");
            Assert.assertFalse(compressResp.isContinuation(), "Response.isContinuation() mismatched.");
            Assert.assertFalse(compressResp.isOK(), "Response.isOK() mismatched.");
            Assert.assertEquals(compressResp.getTag(), "a2", "tag mismatched.");
            // verify logging messages
            final ArgumentCaptor<String> logCapture = ArgumentCaptor.forClass(String.class);
            Mockito.verify(logger, Mockito.times(5)).debug(Mockito.anyString(), Mockito.anyString(), logCapture.capture());
            final List<String> logMsgs = logCapture.getAllValues();
            Assert.assertNotNull(logMsgs, "log messages mismatched.");
            Assert.assertEquals(logMsgs.size(), 5, "log messages mismatched.");
            Assert.assertEquals(logMsgs.get(0), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logMsgs.get(1), "+", "log messages from server mismatched.");
            Assert.assertEquals(logMsgs.get(2), "a1 OK AUTHENTICATE completed", "log messages from server mismatched.");
            Assert.assertEquals(logMsgs.get(3), "a2 COMPRESS DEFLATE\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logMsgs.get(4), "a2 NO Success", "log messages from server mismatched.");
        }
    }

    /**
     * Tests server idles event happens while command queue is NOT empty.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testHandleIdleEventQueueNotEmpty() throws ImapAsyncClientException, IOException, ProtocolException, InterruptedException,
            ExecutionException, TimeoutException, SearchException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise writePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(writePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // construct, turn on session level debugging by having logger.isDebugEnabled() true and session level debug on
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline);

        // execute
        final ImapRequest cmd = new CapaCommand();
        final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);

        final IdleStateEvent idleEvent = null;
        aSession.handleIdleEvent(idleEvent);

        // verify that future should be done now since exception happens
        Assert.assertTrue(future.isDone(), "isDone() should be true now");

        ExecutionException ex = null;
        try {
            future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (final ExecutionException ee) {
            ex = ee;
        }
        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        final Throwable cause = ex.getCause();
        Assert.assertNotNull(cause, "Expect ExecutionException.getCause() to be present.");
        Assert.assertEquals(cause.getClass(), ImapAsyncClientException.class, "Expected result mismatched.");
        final ImapAsyncClientException asynEx = (ImapAsyncClientException) cause;
        Assert.assertEquals(asynEx.getFaiureType(), FailureType.CHANNEL_TIMEOUT, "Failure type mismatched.");
    }

    /**
     * Tests server idles event happens while command queue is empty.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     */
    @Test
    public void testHandleIdleEventQueueEmpty()
            throws ImapAsyncClientException, IOException, ProtocolException, InterruptedException, ExecutionException, TimeoutException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise writePromise = Mockito.mock(ChannelPromise.class);
        final ChannelPromise closePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(writePromise).thenReturn(closePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // construct
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline);

        // command queue is empty
        final IdleStateEvent idleEvent = null;
        aSession.handleIdleEvent(idleEvent);
    }

    /**
     * Tests constructing the session, executing, flushing to server failed.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testExecuteAndFlushToServerFailedCloseSessionFailed() throws ImapAsyncClientException, IOException, ProtocolException,
            InterruptedException, ExecutionException, TimeoutException, SearchException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise writeToServerPromise = Mockito.mock(ChannelPromise.class);
        final ChannelPromise closePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(closePromise.isSuccess()).thenReturn(true);
        Mockito.when(channel.newPromise()).thenReturn(writeToServerPromise).thenReturn(closePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // construct
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline);

        // execute
        final ImapRequest cmd = new CapaCommand();
        final ImapFuture<ImapAsyncResponse> cmdFuture = aSession.execute(cmd);

        Mockito.verify(writeToServerPromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
        Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        // simulate write to server completed with isSuccess() false
        Mockito.when(writeToServerPromise.isSuccess()).thenReturn(false);
        aSession.operationComplete(writeToServerPromise);
        // Above operationComplete() will call requestDoneWithException(), which will call close() to close the channel. Simulating it by making
        // channel inactive
        Mockito.when(channel.isActive()).thenReturn(false);

        // verify that future should be done now since exception happens
        Assert.assertTrue(cmdFuture.isDone(), "isDone() should be true now");
        {
            ExecutionException ex = null;
            try {
                cmdFuture.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (final ExecutionException ee) {
                ex = ee;
            }
            Assert.assertNotNull(ex, "Expect exception to be thrown.");
            final Throwable cause = ex.getCause();
            Assert.assertNotNull(cause, "Expect cause.");
            Assert.assertEquals(cause.getClass(), ImapAsyncClientException.class, "Expected result mismatched.");
            final ImapAsyncClientException asynEx = (ImapAsyncClientException) cause;
            Assert.assertEquals(asynEx.getFaiureType(), FailureType.CHANNEL_EXCEPTION, "Failure type mismatched.");
        }

        // since write to server failed, session.close() would be called by requestDoneWithException(), verify that operationComplete listener is
        // registered
        final ArgumentCaptor<ImapChannelClosedListener> listenerCaptor = ArgumentCaptor.forClass(ImapChannelClosedListener.class);
        Mockito.verify(closePromise, Mockito.times(1)).addListener(listenerCaptor.capture());
        Assert.assertEquals(listenerCaptor.getAllValues().size(), 1, "Unexpected count of ImapChannelClosedListener.");
        final ImapChannelClosedListener closeListener = listenerCaptor.getAllValues().get(0);

        // simulate channel calling ImapChannelClosedListener.operationComplete()
        closeListener.operationComplete(closePromise);

        // simulate channel triggering handleChannelClosed()
        aSession.handleChannelClosed();
        // verify channel is closed / cleared
        Assert.assertTrue(aSession.isChannelClosed(), "channel should be closed by now.");

        // call operationComplete() again, it is an no-op and method should not throw
        aSession.operationComplete(writeToServerPromise);

        // perform close session again from caller, it should just return since session is closed already
        Mockito.when(closePromise.isSuccess()).thenReturn(false);
        final ImapFuture<Boolean> closeFuture = aSession.close();

        // close future should be done successfully even for 2nd time
        Assert.assertTrue(closeFuture.isDone(), "close future should be done");
        final Boolean isSuccess = closeFuture.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(isSuccess, "Result mismatched.");
        Assert.assertTrue(isSuccess, "Result mismatched.");

        // verify logging messages
        final ArgumentCaptor<Object> logCapture = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(logger, Mockito.times(3)).debug(Mockito.anyString(), Mockito.anyString(), logCapture.capture());
        final List<Object> logMsgs = logCapture.getAllValues();
        Assert.assertNotNull(logMsgs, "log messages mismatched.");
        Assert.assertEquals(logMsgs.size(), 3, "log messages mismatched.");
        Assert.assertEquals(logMsgs.get(0), "a1 CAPABILITY\r\n", "log messages from client mismatched.");
        Assert.assertEquals(logMsgs.get(1), "Closing the session via close().", "log messages from client mismatched.");
        Assert.assertEquals(logMsgs.get(2), "Session is confirmed closed.", "Error message mismatched.");

        final ArgumentCaptor<ImapAsyncClientException> errCapture = ArgumentCaptor.forClass(ImapAsyncClientException.class);
        Mockito.verify(logger, Mockito.times(1)).error(Mockito.anyString(), Mockito.anyString(), errCapture.capture());
        final List<ImapAsyncClientException> errMsgs = errCapture.getAllValues();
        final ImapAsyncClientException e = errMsgs.get(0);
        Assert.assertNotNull(e, "Log error for exception is missing");
        Assert.assertEquals(e.getFaiureType(), FailureType.CHANNEL_EXCEPTION, "Class mismatched.");

        // calling setDebugMode() on a closed session, should not throw NPE
        aSession.setDebugMode(DebugMode.DEBUG_ON);
    }

    /**
     * Tests constructing the session, execute, and channel is closed abruptly before server response is back.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testExecuteChannelCloseBeforeServerResponseArrived() throws ImapAsyncClientException, IOException, ProtocolException,
            InterruptedException, ExecutionException, TimeoutException, IllegalArgumentException, IllegalAccessException, SearchException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise writePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(writePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // construct, class level logging is off, session level logging is on
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline);

        // execute
        final ImapRequest cmd = new CapaCommand();
        final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);

        Mockito.verify(writePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
        Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        // simulate channel closed
        aSession.handleChannelClosed();

        // verify that future should be done now since exception happens
        Assert.assertTrue(future.isDone(), "isDone() should be true now");
        ExecutionException ex = null;
        try {
            future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (final ExecutionException ee) {
            ex = ee;
        }
        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        final Throwable cause = ex.getCause();
        Assert.assertNotNull(cause, "Expect cause.");
        Assert.assertEquals(cause.getClass(), ImapAsyncClientException.class, "Expected result mismatched.");
        final ImapAsyncClientException asynEx = (ImapAsyncClientException) cause;
        Assert.assertEquals(asynEx.getFaiureType(), FailureType.CHANNEL_DISCONNECTED, "Failure type mismatched.");
        // verify logging messages
        final ArgumentCaptor<Object> logCapture = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(logger, Mockito.times(3)).debug(Mockito.anyString(), Mockito.anyString(), logCapture.capture());
        final List<Object> logMsgs = logCapture.getAllValues();
        Assert.assertNotNull(logMsgs, "log messages mismatched.");
        Assert.assertEquals(logMsgs.size(), 3, "log messages mismatched.");
        Assert.assertEquals(logMsgs.get(0), "a1 CAPABILITY\r\n", "log messages from client mismatched.");
        Assert.assertEquals(logMsgs.get(1), "Session is confirmed closed.", "log messages mismatched.");
        Assert.assertEquals(logMsgs.get(2), "Closing the session via close().", "log messages mismatched.");

        final ArgumentCaptor<ImapAsyncClientException> errCapture = ArgumentCaptor.forClass(ImapAsyncClientException.class);
        Mockito.verify(logger, Mockito.times(1)).error(Mockito.anyString(), Mockito.anyString(), errCapture.capture());
        final List<ImapAsyncClientException> errMsgs = errCapture.getAllValues();
        final ImapAsyncClientException e = errMsgs.get(0);
        Assert.assertNotNull(e, "Log error for exception is missing");
        Assert.assertEquals(e.getFaiureType(), FailureType.CHANNEL_DISCONNECTED, "Class mismatched.");
    }

    /**
     * Tests constructing the session, execute, and channel is closed abruptly before server response is back. In this test, the log level is in info,
     * not in debug, we want to make sure it does not call debug().
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testExecuteChannelCloseBeforeServerResponseArrivedLogLevelInfo() throws ImapAsyncClientException, IOException, ProtocolException,
            InterruptedException, ExecutionException, TimeoutException, IllegalArgumentException, IllegalAccessException, SearchException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise writePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(writePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isInfoEnabled()).thenReturn(true);
        Mockito.when(logger.isDebugEnabled()).thenReturn(false);

        // construct, class level logging is off, session level logging is on
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline);

        // execute
        final ImapRequest cmd = new CapaCommand();
        final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);

        Mockito.verify(writePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
        Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
        // Ensure there is no call to debug() method
        Mockito.verify(logger, Mockito.times(0)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        // simulate channel closed
        aSession.handleChannelClosed();

        // verify that future should be done now since exception happens
        Assert.assertTrue(future.isDone(), "isDone() should be true now");
        ExecutionException ex = null;
        try {
            future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (final ExecutionException ee) {
            ex = ee;
        }
        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        final Throwable cause = ex.getCause();
        Assert.assertNotNull(cause, "Expect cause.");
        Assert.assertEquals(cause.getClass(), ImapAsyncClientException.class, "Expected result mismatched.");
        final ImapAsyncClientException asynEx = (ImapAsyncClientException) cause;
        Assert.assertEquals(asynEx.getFaiureType(), FailureType.CHANNEL_DISCONNECTED, "Failure type mismatched.");
        // verify logging messages
        final ArgumentCaptor<Object> logCapture = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(logger, Mockito.times(0)).debug(Mockito.anyString(), Mockito.anyString(), logCapture.capture());
        final List<Object> logMsgs = logCapture.getAllValues();
        Assert.assertNotNull(logMsgs, "log messages mismatched.");
        Assert.assertEquals(logMsgs.size(), 0, "log messages mismatched.");

        final ArgumentCaptor<ImapAsyncClientException> errCapture = ArgumentCaptor.forClass(ImapAsyncClientException.class);
        Mockito.verify(logger, Mockito.times(1)).error(Mockito.anyString(), Mockito.anyString(), errCapture.capture());
        final List<ImapAsyncClientException> errMsgs = errCapture.getAllValues();
        final ImapAsyncClientException e = errMsgs.get(0);
        Assert.assertNotNull(e, "Log error for exception is missing");
        Assert.assertEquals(e.getFaiureType(), FailureType.CHANNEL_DISCONNECTED, "Class mismatched.");
    }

    /**
     * Tests the whole success flow: construct the session, execute idle, handle server response, terminate.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testExecuteIdleHandleResponseFlushCompleteTerminateSuccess() throws ImapAsyncClientException, IOException, ProtocolException,
            InterruptedException, ExecutionException, TimeoutException, SearchException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise writePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(writePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // construct
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline);

        // execute
        final ConcurrentLinkedQueue<IMAPResponse> serverResponesQ = new ConcurrentLinkedQueue<IMAPResponse>();
        final ImapRequest cmd = new IdleCommand(serverResponesQ);
        ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);

        Mockito.verify(writePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
        Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        // simulate write to server completed successfully
        Mockito.when(writePromise.isSuccess()).thenReturn(true);
        aSession.operationComplete(writePromise);

        // handle server response
        final IMAPResponse serverResp1 = new IMAPResponse("+ idling");
        aSession.handleChannelResponse(serverResp1);
        final IMAPResponse serverResp2 = new IMAPResponse("* 2 EXPUNGE");
        aSession.handleChannelResponse(serverResp2);
        final IMAPResponse serverResp3 = new IMAPResponse("* 3 EXISTS");
        aSession.handleChannelResponse(serverResp3);
        // verify that future should NOT be done
        Assert.assertFalse(future.isDone(), "isDone() result mismatched.");

        // should not encounter exception
        final IdleStateEvent idleEvent = null;
        aSession.handleIdleEvent(idleEvent);
        // verify that future should NOT be done in case there is exception
        Assert.assertFalse(future.isDone(), "isDone() result mismatched.");

        // terminate the idle mode
        future = aSession.terminateCommand(cmd);
        final IMAPResponse serverResp4 = new IMAPResponse("a1 OK IDLE terminated");
        aSession.handleChannelResponse(serverResp4);
        // verify that future should be done now
        Assert.assertTrue(future.isDone(), "isDone() should be true now");
        final ImapAsyncResponse asyncResp = future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        final Collection<IMAPResponse> lines = asyncResp.getResponseLines();
        final int expectedCount = 4;
        Assert.assertEquals(lines.size(), expectedCount, "responses count mismatched.");
        final Iterator<IMAPResponse> it = lines.iterator();
        Assert.assertEquals(it.next(), serverResp1, "response mismatched.");
        Assert.assertEquals(it.next(), serverResp2, "response mismatched.");
        Assert.assertEquals(it.next(), serverResp3, "response mismatched.");
        final IMAPResponse endingResp = it.next();
        Assert.assertNotNull(endingResp, "Result mismatched.");
        Assert.assertTrue(endingResp.isOK(), "Response.isOK() mismatched.");
        Assert.assertEquals(endingResp.getTag(), "a1", "tag mismatched.");

        // queue is empty now, calling terminateCommand again
        ImapAsyncClientException ex = null;
        try {
            aSession.terminateCommand(cmd);
        } catch (final ImapAsyncClientException ie) {
            ex = ie;
        }

        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        Assert.assertEquals(ex.getFaiureType(), FailureType.COMMAND_NOT_ALLOWED, "FailureTypet mismatched.");
        // verify logging messages for all idle communication
        final ArgumentCaptor<String> logCapture = ArgumentCaptor.forClass(String.class);
        Mockito.verify(logger, Mockito.times(6)).debug(Mockito.anyString(), Mockito.anyString(), logCapture.capture());
        final List<String> logMsgs = logCapture.getAllValues();
        Assert.assertNotNull(logMsgs, "log messages mismatched.");
        Assert.assertEquals(logMsgs.size(), 6, "log messages mismatched.");
        Assert.assertEquals(logMsgs.get(0), "a1 IDLE\r\n", "log messages from client mismatched.");
        Assert.assertEquals(logMsgs.get(1), "+ idling", "log messages from server mismatched.");
        Assert.assertEquals(logMsgs.get(2), "* 2 EXPUNGE", "log messages from server mismatched.");
        Assert.assertEquals(logMsgs.get(3), "* 3 EXISTS", "log messages from server mismatched.");
        Assert.assertEquals(logMsgs.get(4), "DONE\r\n", "log messages from client mismatched.");
        Assert.assertEquals(logMsgs.get(5), "a1 OK IDLE terminated", "log messages from server mismatched.");
    }

    /**
     * Tests execute method when command queue is not empty.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testExecuteFailedDueToQueueNotEmpty() throws ImapAsyncClientException, IOException, SearchException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise writePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(writePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline);
        final ImapRequest cmd = new CapaCommand();
        aSession.execute(cmd);

        Mockito.verify(writePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
        Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));

        ImapAsyncClientException ex = null;
        try {
            // execute again, queue is not empty
            aSession.execute(cmd);
        } catch (final ImapAsyncClientException asyncEx) {
            ex = asyncEx;
        }
        Assert.assertNotNull(ex, "Exception should occur.");
        Assert.assertEquals(ex.getFaiureType(), FailureType.COMMAND_NOT_ALLOWED, "Failure type mismatched.");
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    /**
     * Tests execute method when channel is inactive, then call close() to close session.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testExecuteFailedChannelInactiveAndCloseChannel()
            throws ImapAsyncClientException, IOException, InterruptedException, ExecutionException, TimeoutException, SearchException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(false);
        final ChannelPromise writePromise = Mockito.mock(ChannelPromise.class);
        final ChannelPromise closePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(writePromise).thenReturn(closePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // constructor, class level debug is off, but session level is on
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline);
        final ImapRequest cmd = new CapaCommand();
        ImapAsyncClientException ex = null;
        try {
            // execute again, queue is not empty
            aSession.execute(cmd);
        } catch (final ImapAsyncClientException asyncEx) {
            ex = asyncEx;
        }
        Assert.assertNotNull(ex, "Exception should occur.");
        Assert.assertEquals(ex.getFaiureType(), FailureType.OPERATION_PROHIBITED_ON_CLOSED_CHANNEL, "Failure type mismatched.");

        Mockito.verify(writePromise, Mockito.times(0)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
        Mockito.verify(channel, Mockito.times(0)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
        // encountering the above exception in execute(), will not log the command sent over the wire
        Mockito.verify(logger, Mockito.times(0)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        Mockito.when(closePromise.isSuccess()).thenReturn(true);
        final ImapFuture<Boolean> closeFuture = aSession.close();
        // close future should be done successfully
        Assert.assertTrue(closeFuture.isDone(), "close future should be done");
        final Boolean closeResp = closeFuture.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(closeResp, "close() response mismatched.");
        Assert.assertTrue(closeResp, "close() response mismatched.");

    }

    /**
     * Tests close() method and its close listener. Specifically testing operationComplete with a future that returns false for isSuccess().
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testCloseSessionOperationCompleteFutureIsUnsuccessful() throws ImapAsyncClientException, IOException, ProtocolException,
            InterruptedException, ExecutionException, TimeoutException, SearchException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise closePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(closePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(false);

        // construct, both class level and session level debugging are off
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_OFF, SESSION_ID, pipeline);

        // perform close session, isSuccess() returns false
        Mockito.when(closePromise.isSuccess()).thenReturn(false);
        final ImapFuture<Boolean> closeFuture = aSession.close();
        final ArgumentCaptor<ImapChannelClosedListener> listenerCaptor = ArgumentCaptor.forClass(ImapChannelClosedListener.class);
        Mockito.verify(closePromise, Mockito.times(1)).addListener(listenerCaptor.capture());
        Assert.assertEquals(listenerCaptor.getAllValues().size(), 1, "Unexpected count of ImapChannelClosedListener.");
        final ImapChannelClosedListener closeListener = listenerCaptor.getAllValues().get(0);
        closeListener.operationComplete(closePromise);
        // close future should be done successfully
        Assert.assertTrue(closeFuture.isDone(), "close future should be done");

        Throwable actual = null;
        try {
            // execute again, queue is not empty
            closeFuture.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (final ExecutionException e) {
            actual = e.getCause();
        }
        Assert.assertNotNull(actual, "Exception should occur.");
        Assert.assertEquals(actual.getClass(), ImapAsyncClientException.class, "Class type mismatched.");
        final ImapAsyncClientException asyncClientEx = (ImapAsyncClientException) actual;
        Assert.assertEquals(asyncClientEx.getFaiureType(), FailureType.CLOSING_CONNECTION_FAILED, "Failure type mismatched.");
    }

    /**
     * Tests the whole life cycle flow: construct the session, execute, handle server response success, close session.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testExecuteAuthHandleResponseChannelInactive() throws ImapAsyncClientException, IOException, ProtocolException, InterruptedException,
            ExecutionException, TimeoutException, SearchException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise authWritePromise = Mockito.mock(ChannelPromise.class); // first
        final ChannelPromise authWritePromise2 = Mockito.mock(ChannelPromise.class); // after +
        final ChannelPromise capaWritePromise = Mockito.mock(ChannelPromise.class);
        final ChannelPromise closePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(authWritePromise).thenReturn(authWritePromise2).thenReturn(capaWritePromise)
                .thenReturn(closePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // construct, both class level and session level debugging are off
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_OFF, SESSION_ID, pipeline);

        // execute Authenticate plain command
        {
            final Map<String, List<String>> capas = new HashMap<String, List<String>>();
            final ImapRequest cmd = new AuthPlainCommand("orange", "juicy", new Capability(capas));
            final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);
            Mockito.verify(authWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
            Mockito.verify(logger, Mockito.times(0)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

            // simulate write to server completed successfully
            Mockito.when(authWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(authWritePromise);

            // handle server response
            final IMAPResponse serverResp1 = new IMAPResponse("+");

            // simulate that channel is closed
            Mockito.when(channel.isActive()).thenReturn(false);
            aSession.setDebugMode(DebugMode.DEBUG_ON);

            // following will call getNextCommandLineAfterContinuation
            aSession.handleChannelResponse(serverResp1);

            // verify that future should be done now
            Assert.assertTrue(future.isDone(), "isDone() should be true now");
            ExecutionException ex = null;
            try {
                future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (final ExecutionException ee) {
                ex = ee;
            }
            Assert.assertNotNull(ex, "Expect exception to be thrown.");
            final Throwable cause = ex.getCause();
            Assert.assertNotNull(cause, "Expect ExecutionException.getCause() to be present.");
            Assert.assertEquals(cause.getClass(), ImapAsyncClientException.class, "Expected result mismatched.");
            final ImapAsyncClientException asynEx = (ImapAsyncClientException) cause;
            Assert.assertEquals(asynEx.getFaiureType(), FailureType.CHANNEL_EXCEPTION, "Failure type mismatched.");

            // verify logging messages
            final ArgumentCaptor<String> logCapture = ArgumentCaptor.forClass(String.class);
            Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), logCapture.capture());
            final List<String> logMsgs = logCapture.getAllValues();
            Assert.assertNotNull(logMsgs, "log messages mismatched.");
            Assert.assertEquals(logMsgs.size(), 1, "log messages mismatched.");
            Assert.assertEquals(logMsgs.get(0), "+", "log messages from client mismatched.");

            final ArgumentCaptor<ImapAsyncClientException> errCapture = ArgumentCaptor.forClass(ImapAsyncClientException.class);
            Mockito.verify(logger, Mockito.times(1)).error(Mockito.anyString(), Mockito.anyString(), errCapture.capture());
            final List<ImapAsyncClientException> errMsgs = errCapture.getAllValues();
            final ImapAsyncClientException e = errMsgs.get(0);
            Assert.assertNotNull(e, "Log error for exception is missing");
            Assert.assertEquals(e.getFaiureType(), FailureType.CHANNEL_EXCEPTION, "Class mismatched.");

            // no more responses in the queue, calling handleResponse should return right away without any indexOutOfBound
            aSession.handleChannelResponse(serverResp1);
        }

    }

    /**
     * Tests when server responses CompressCommand success and there is no ssl handler.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testExecuteAuthCompressHandleResponseChannelIsClosed() throws ImapAsyncClientException, IOException, ProtocolException,
            InterruptedException, ExecutionException, TimeoutException, SearchException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise authWritePromise = Mockito.mock(ChannelPromise.class); // first
        final ChannelPromise authWritePromise2 = Mockito.mock(ChannelPromise.class); // after +
        final ChannelPromise compressWritePromise = Mockito.mock(ChannelPromise.class);
        final ChannelPromise closePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(authWritePromise).thenReturn(authWritePromise2).thenReturn(compressWritePromise)
                .thenReturn(closePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isTraceEnabled()).thenReturn(true);

        // construct, class level debug is enabled, session level debug is disabled
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, DebugMode.DEBUG_OFF, SESSION_ID, pipeline);

        // execute Authenticate plain command
        {
            final Map<String, List<String>> capas = new HashMap<String, List<String>>();
            final ImapRequest cmd = new AuthPlainCommand("orange", "juicy", new Capability(capas));
            final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);
            Mockito.verify(authWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));

            // simulate write to server completed successfully
            Mockito.when(authWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(authWritePromise);

            // handle server response
            final IMAPResponse serverResp1 = new IMAPResponse("+");
            // following will call getNextCommandLineAfterContinuation
            aSession.handleChannelResponse(serverResp1);
            Mockito.verify(channel, Mockito.times(2)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));

            final IMAPResponse serverResp2 = new IMAPResponse("a1 OK AUTHENTICATE completed");
            aSession.handleChannelResponse(serverResp2);

            // verify that future should be done now
            Assert.assertTrue(future.isDone(), "isDone() should be true now");
            final ImapAsyncResponse asyncResp = future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            final Collection<IMAPResponse> lines = asyncResp.getResponseLines();
            Assert.assertEquals(lines.size(), 2, "responses count mismatched.");
            final Iterator<IMAPResponse> it = lines.iterator();
            final IMAPResponse continuationResp = it.next();
            Assert.assertNotNull(continuationResp, "Result mismatched.");
            Assert.assertTrue(continuationResp.isContinuation(), "Response.isContinuation() mismatched.");
            final IMAPResponse endingResp = it.next();
            Assert.assertNotNull(endingResp, "Result mismatched.");
            Assert.assertTrue(endingResp.isOK(), "Response.isOK() mismatched.");
            Assert.assertEquals(endingResp.getTag(), "a1", "tag mismatched.");
            // verify logging messages
            final ArgumentCaptor<String> logCapture = ArgumentCaptor.forClass(String.class);
            Mockito.verify(logger, Mockito.times(3)).debug(Mockito.anyString(), Mockito.anyString(), logCapture.capture());
            final List<String> logMsgs = logCapture.getAllValues();
            Assert.assertNotNull(logMsgs, "log messages mismatched.");
            Assert.assertEquals(logMsgs.size(), 3, "log messages mismatched.");
            Assert.assertEquals(logMsgs.get(0), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logMsgs.get(1), "+", "log messages from server mismatched.");
            Assert.assertEquals(logMsgs.get(2), "a1 OK AUTHENTICATE completed", "log messages from server mismatched.");
        }

        {
            // start Compression
            final ImapFuture<ImapAsyncResponse> future = aSession.startCompression();

            Mockito.verify(compressWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(3)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));

            // simulate write to server completed successfully
            Mockito.when(compressWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(compressWritePromise);

            // handle server response
            // simulate that channel is closed
            Mockito.when(channel.isActive()).thenReturn(false);
            final IMAPResponse serverResp1 = new IMAPResponse("a2 OK Success");
            aSession.handleChannelResponse(serverResp1);

            // verify deflater and inflater handlers are not added
            Mockito.verify(pipeline, Mockito.times(0)).addFirst(Matchers.eq("DEFLATER"), Matchers.isA(JdkZlibDecoder.class));
            Mockito.verify(pipeline, Mockito.times(0)).addFirst(Matchers.eq("INFLATER"), Matchers.isA(JdkZlibEncoder.class));
            // verify that future should be done now
            Assert.assertTrue(future.isDone(), "isDone() should be true now");
            ExecutionException ex = null;
            try {
                future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (final ExecutionException ee) {
                ex = ee;
            }
            Assert.assertNotNull(ex, "Expect exception to be thrown.");
            final Throwable cause = ex.getCause();
            Assert.assertNotNull(cause, "Expect ExecutionException.getCause() to be present.");
            Assert.assertEquals(cause.getClass(), ImapAsyncClientException.class, "Expected result mismatched.");
            final ImapAsyncClientException asynEx = (ImapAsyncClientException) cause;
            Assert.assertEquals(asynEx.getFaiureType(), FailureType.OPERATION_PROHIBITED_ON_CLOSED_CHANNEL, "Failure type mismatched.");

            // verify logging messages
            final ArgumentCaptor<String> logCapture = ArgumentCaptor.forClass(String.class);
            Mockito.verify(logger, Mockito.times(5)).debug(Mockito.anyString(), Mockito.anyString(), logCapture.capture());
            final List<String> logMsgs = logCapture.getAllValues();
            Assert.assertNotNull(logMsgs, "log messages mismatched.");
            Assert.assertEquals(logMsgs.size(), 5, "log messages mismatched.");
            Assert.assertEquals(logMsgs.get(0), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logMsgs.get(1), "+", "log messages from server mismatched.");
            Assert.assertEquals(logMsgs.get(2), "a1 OK AUTHENTICATE completed", "log messages from server mismatched.");
            Assert.assertEquals(logMsgs.get(3), "a2 COMPRESS DEFLATE\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logMsgs.get(4), "a2 OK Success", "log messages from server mismatched.");
        }
    }

}