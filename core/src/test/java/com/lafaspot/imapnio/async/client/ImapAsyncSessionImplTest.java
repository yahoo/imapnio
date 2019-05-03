package com.lafaspot.imapnio.async.client;

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
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.async.client.ImapAsyncSessionImpl;
import com.lafaspot.imapnio.async.client.ImapFuture;
import com.lafaspot.imapnio.async.client.ImapAsyncSessionImpl.ImapChannelClosedListener;
import com.lafaspot.imapnio.async.data.Capability;
import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;
import com.lafaspot.imapnio.async.exception.ImapAsyncClientException.FailureType;
import com.lafaspot.imapnio.async.request.AuthPlainCommand;
import com.lafaspot.imapnio.async.request.CapaCommand;
import com.lafaspot.imapnio.async.request.IdleCommand;
import com.lafaspot.imapnio.async.request.ImapRequest;
import com.lafaspot.imapnio.async.response.ImapAsyncResponse;
import com.lafaspot.imapnio.async.response.ImapResponseMapper;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
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
     * Tests the whole life cyle flow: construct the session, execute, handle server response success, close session.
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
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // construct
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, SESSION_ID, pipeline);

        // execute Authticate plain command
        {
            final Map<String, List<String>> capas = new HashMap<String, List<String>>();
            final ImapRequest<String> cmd = new AuthPlainCommand("orange", "juicy", new Capability(capas));
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

            final IMAPResponse serverResp2 = new IMAPResponse("A001 OK AUTHENTICATE completed");
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
            Assert.assertEquals(endingResp.getTag(), "A001", "tag mismatched.");
        }

        {
            // execute capa
            final ImapRequest<String> cmd = new CapaCommand();
            final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);

            Mockito.verify(capaWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(3)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
            Mockito.verify(logger, Mockito.times(2)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

            // simulate write to server completed successfully
            Mockito.when(capaWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(capaWritePromise);

            // handle server response
            final IMAPResponse serverResp1 = new IMAPResponse(
                    "* CAPABILITY IMAP4rev1 SASL-IR AUTH=PLAIN AUTH=XOAUTH2 AUTH=OAUTHBEARER ID MOVE NAMESPACE");
            aSession.handleChannelResponse(serverResp1);
            final IMAPResponse serverResp2 = new IMAPResponse("A002 OK CAPABILITY completed");
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
            Assert.assertEquals(endingResp.getTag(), "A002", "tag mismatched.");
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

        // construct
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, SESSION_ID, pipeline);

        // execute
        final ImapRequest<String> cmd = new CapaCommand();
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
        Assert.assertNotNull(cause, "Expect cause.");
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
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, SESSION_ID, pipeline);

        // command queue is empty
        final IdleStateEvent idleEvent = null;
        aSession.handleIdleEvent(idleEvent);
    }

    /**
     * Tests constructing the session, executing, flusing to server failed.
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
        final ChannelPromise writePromise = Mockito.mock(ChannelPromise.class);
        final ChannelPromise closePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(writePromise).thenReturn(closePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // construct
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, SESSION_ID, pipeline);

        // execute
        final ImapRequest<String> cmd = new CapaCommand();
        final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);

        Mockito.verify(writePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
        Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        // simulate write to server completed successfully
        Mockito.when(writePromise.isSuccess()).thenReturn(false);
        aSession.operationComplete(writePromise);

        // verify that future should be done now since exception happens
        Assert.assertTrue(future.isDone(), "isDone() should be true now");
        {
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
            Assert.assertEquals(asynEx.getFaiureType(), FailureType.CHANNEL_EXCEPTION, "Failure type mismatched.");
        }
        // call operationComplete() again, it is an no-op and method should not throw since queue is empty
        aSession.operationComplete(writePromise);

        // perform close session
        Mockito.when(closePromise.isSuccess()).thenReturn(false);
        final ImapFuture<Boolean> closeFuture = aSession.close();
        final ArgumentCaptor<ImapChannelClosedListener> listenerCaptor = ArgumentCaptor.forClass(ImapChannelClosedListener.class);
        Mockito.verify(closePromise, Mockito.times(1)).addListener(listenerCaptor.capture());
        Assert.assertEquals(listenerCaptor.getAllValues().size(), 1, "Unexpected count of ImapChannelClosedListener.");
        final ImapChannelClosedListener closeListener = listenerCaptor.getAllValues().get(0);
        closeListener.operationComplete(closePromise);
        // close future should be done successfully
        Assert.assertTrue(closeFuture.isDone(), "close future should be done");
        {
            ExecutionException ex = null;
            try {
                closeFuture.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (final ExecutionException ee) {
                ex = ee;
            }
            Assert.assertNotNull(ex, "Expect exception to be thrown.");
            final Throwable cause = ex.getCause();
            Assert.assertNotNull(cause, "Expect cause.");
            Assert.assertEquals(cause.getClass(), ImapAsyncClientException.class, "Expected result mismatched.");
            final ImapAsyncClientException asynEx = (ImapAsyncClientException) cause;
            Assert.assertEquals(asynEx.getFaiureType(), FailureType.CLOSING_CONNECTION_FAILED, "Failure type mismatched.");
        }
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

        // construct
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, SESSION_ID, pipeline);

        // execute
        final ImapRequest<String> cmd = new CapaCommand();
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

        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(aSession), "Cleanup should set " + field.getName() + " as null");
        }
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
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, SESSION_ID, pipeline);

        // execute
        final ConcurrentLinkedQueue<IMAPResponse> serverResponesQ = new ConcurrentLinkedQueue<IMAPResponse>();
        final ImapRequest<String> cmd = new IdleCommand(serverResponesQ);
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
        final IMAPResponse serverResp4 = new IMAPResponse("A1 OK IDLE terminated");
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
        Assert.assertEquals(endingResp.getTag(), "A1", "tag mismatched.");

        // queue is empty now, calling terminateCommand again
        ImapAsyncClientException ex = null;
        try {
            aSession.terminateCommand(cmd);
        } catch (final ImapAsyncClientException ie) {
            ex = ie;
        }

        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        Assert.assertEquals(ex.getFaiureType(), FailureType.COMMAND_NOT_ALLOWED, "FailureTypet mismatched.");

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
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, SESSION_ID, pipeline);
        final ImapRequest<String> cmd = new CapaCommand();
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
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(channel, logger, SESSION_ID, pipeline);
        final ImapRequest<String> cmd = new CapaCommand();
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
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        Mockito.when(closePromise.isSuccess()).thenReturn(true);
        final ImapFuture<Boolean> closeFuture = aSession.close();
        // close future should be done successfully
        Assert.assertTrue(closeFuture.isDone(), "close future should be done");
        final Boolean closeResp = closeFuture.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(closeResp, "close() response mismatched.");
        Assert.assertTrue(closeResp, "close() response mismatched.");

    }

    /**
     * Tests execute method when channel is inactive.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testExecuteFailedChannelNullDebugOff() throws ImapAsyncClientException, IOException, SearchException {
        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(false);
        final ChannelPromise writePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(writePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(false);
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(null, logger, SESSION_ID, pipeline);
        final ImapRequest<String> cmd = new CapaCommand();
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
    }
}