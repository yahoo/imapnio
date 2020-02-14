package com.yahoo.imapnio.async.netty;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.client.ImapAsyncCreateSessionResponse;
import com.yahoo.imapnio.async.client.ImapAsyncSession.DebugMode;
import com.yahoo.imapnio.async.client.ImapFuture;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * Unit test for {@code ImapClientConnectHandler}.
 */
public class ImapClientConnectHandlerTest {

    /** Dummy session id. */
    private static final int SESSION_ID = 123456;

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> c = ImapClientConnectHandler.class;
        fieldsToCheck = new HashSet<>();

        for (final Field declaredField : c.getDeclaredFields()) {
            if (!declaredField.getType().isPrimitive() && !Modifier.isStatic(declaredField.getModifiers())) {
                declaredField.setAccessible(true);
                fieldsToCheck.add(declaredField);
            }
        }
    }

    /**
     * Tests decode method when successful.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ProtocolException will not throw
     * @throws IOException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws TimeoutException will not throw
     */
    @Test
    public void testDecodeConnectSuccess() throws IllegalArgumentException, IllegalAccessException, IOException, ProtocolException,
            InterruptedException, ExecutionException, TimeoutException {
        final ImapFuture<ImapAsyncCreateSessionResponse> imapFuture = new ImapFuture<ImapAsyncCreateSessionResponse>();
        final Logger logger = Mockito.mock(Logger.class);

        final String sessCtx = "Titanosauria@long.neck";
        final ImapClientConnectHandler handler = new ImapClientConnectHandler(imapFuture, logger, DebugMode.DEBUG_ON, SESSION_ID, sessCtx);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(ctx.pipeline()).thenReturn(pipeline);

        final IMAPResponse resp = new IMAPResponse("* OK [CAPABILITY IMAP4rev1 SASL-IR AUTH=PLAIN] IMAP4rev1 Hello");
        final List<Object> out = new ArrayList<Object>();
        handler.decode(ctx, resp, out);

        Mockito.verify(pipeline, Mockito.times(1)).remove(Mockito.anyString());
        Assert.assertTrue(imapFuture.isDone(), "Future should be done");
        final ImapAsyncCreateSessionResponse asyncSession = imapFuture.get(5, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(asyncSession, "Expect ImapAsyncSession not to be null");
    }

    /**
     * Tests decode method when successful.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ProtocolException will not throw
     * @throws IOException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     * @throws TimeoutException will not throw
     */
    @Test
    public void testDecodeConnectFailed() throws IllegalArgumentException, IllegalAccessException, IOException, ProtocolException,
            InterruptedException, ExecutionException, TimeoutException {
        final ImapFuture<ImapAsyncCreateSessionResponse> imapFuture = new ImapFuture<ImapAsyncCreateSessionResponse>();
        final Logger logger = Mockito.mock(Logger.class);

        final String sessCtx = "Titanosauria@long.neck";
        final ImapClientConnectHandler handler = new ImapClientConnectHandler(imapFuture, logger, DebugMode.DEBUG_ON, SESSION_ID, sessCtx);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(ctx.pipeline()).thenReturn(pipeline);

        final String msg = "* BAD [CAPABILITY IMAP4rev1 SASL-IR AUTH=PLAIN] IMAP4rev1 Hello";
        final IMAPResponse resp = new IMAPResponse(msg);
        final List<Object> out = new ArrayList<Object>();
        handler.decode(ctx, resp, out);

        Mockito.verify(logger, Mockito.times(1)).error(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(pipeline, Mockito.times(1)).remove(Mockito.anyString());

        Assert.assertTrue(imapFuture.isDone(), "Future should be done");
        ExecutionException ex = null;
        try {
            imapFuture.get(5, TimeUnit.MILLISECONDS);
        } catch (final ExecutionException ee) {
            ex = ee;
        }

        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        Assert.assertNotNull(ex.getCause(), "Expect cause.");
        Assert.assertEquals(ex.getCause().getClass(), ImapAsyncClientException.class, "Expected result mismatched.");
    }

    /**
     * Tests exceptionCaught method.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws InterruptedException will not throw
     * @throws TimeoutException will not throw
     */
    @Test
    public void testExceptionCaught() throws IllegalArgumentException, IllegalAccessException, InterruptedException, TimeoutException {
        final ImapFuture<ImapAsyncCreateSessionResponse> imapFuture = new ImapFuture<ImapAsyncCreateSessionResponse>();
        final Logger logger = Mockito.mock(Logger.class);

        final String sessCtx = "Titanosauria@long.neck";
        final ImapClientConnectHandler handler = new ImapClientConnectHandler(imapFuture, logger, DebugMode.DEBUG_ON, SESSION_ID, sessCtx);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        final TimeoutException timeoutEx = new TimeoutException("too late, my friend");
        handler.exceptionCaught(ctx, timeoutEx);

        Assert.assertTrue(imapFuture.isDone(), "Future should be done");
        ExecutionException ex = null;
        try {
            imapFuture.get(5, TimeUnit.MILLISECONDS);
        } catch (final ExecutionException ee) {
            ex = ee;
        }
        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        Assert.assertNotNull(ex.getCause(), "Expect cause.");
        Assert.assertEquals(ex.getCause().getClass(), ImapAsyncClientException.class, "Expected result mismatched.");
    }

    /**
     * Tests exceptionCaught method with unKnownHostException.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws InterruptedException will not throw
     * @throws TimeoutException will not throw
     */
    @Test
    public void testExceptionCaughtWithUnKnownHost() throws IllegalArgumentException, IllegalAccessException, InterruptedException, TimeoutException {
        final ImapFuture<ImapAsyncCreateSessionResponse> imapFuture = new ImapFuture<ImapAsyncCreateSessionResponse>();
        final Logger logger = Mockito.mock(Logger.class);

        final String sessCtx = "Titanosauria@long.neck";
        final ImapClientConnectHandler handler = new ImapClientConnectHandler(imapFuture, logger, DebugMode.DEBUG_ON, SESSION_ID, sessCtx);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        final UnknownHostException unknownHostEx = new UnknownHostException("Unknown host");
        handler.exceptionCaught(ctx, unknownHostEx);

        ExecutionException ex = null;
        Assert.assertTrue(imapFuture.isDone(), "Future should be done");
        try {
            imapFuture.get(5, TimeUnit.MILLISECONDS);
        } catch (final ExecutionException ee) {
            ex = ee;
        }
        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        Assert.assertNotNull(ex.getCause(), "Expect cause.");
        Assert.assertEquals(ex.getCause().getClass(), ImapAsyncClientException.class, "Expected result mismatched.");
        final ImapAsyncClientException imapEx = (ImapAsyncClientException) ex.getCause();
        Assert.assertNotNull(imapEx.getCause(), "expect cause");
        Assert.assertEquals(imapEx.getCause().getClass(), UnknownHostException.class, "Cause should be UnknownHost exception.");
        Assert.assertEquals(imapEx.getFaiureType(), FailureType.UNKNOWN_HOST_EXCEPTION, "Failure type mismatch");
    }

    /**
     * Tests exceptionCaught method with ConnectTimeout exception.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws InterruptedException will not throw
     * @throws TimeoutException will not throw
     */
    @Test
    public void testExceptionCaughtWithConnectTimeoutException()
            throws IllegalArgumentException, IllegalAccessException, InterruptedException, TimeoutException {
        final ImapFuture<ImapAsyncCreateSessionResponse> imapFuture = new ImapFuture<ImapAsyncCreateSessionResponse>();
        final Logger logger = Mockito.mock(Logger.class);

        final String sessCtx = "Titanosauria@long.neck";
        final ImapClientConnectHandler handler = new ImapClientConnectHandler(imapFuture, logger, DebugMode.DEBUG_ON, SESSION_ID, sessCtx);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        final ConnectTimeoutException connectTimeoutEx = new ConnectTimeoutException("connection timeout");
        handler.exceptionCaught(ctx, connectTimeoutEx);

        Assert.assertTrue(imapFuture.isDone(), "Future should be done");
        ExecutionException ex = null;
        try {
            imapFuture.get(5, TimeUnit.MILLISECONDS);
        } catch (final ExecutionException ee) {
            ex = ee;
        }
        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        Assert.assertNotNull(ex.getCause(), "Expect cause.");
        Assert.assertEquals(ex.getCause().getClass(), ImapAsyncClientException.class, "Expected result mismatched.");
        final ImapAsyncClientException imapEx = (ImapAsyncClientException) ex.getCause();
        Assert.assertNotNull(imapEx.getCause(), "expect cause");
        Assert.assertEquals(imapEx.getCause().getClass(), ConnectTimeoutException.class, "Cause should be ConnectTimeout exception.");
        Assert.assertEquals(imapEx.getFaiureType(), FailureType.CONNECTION_TIMEOUT_EXCEPTION, "Failure type mismatch");
    }

    /**
     * Tests userEventTriggered method and the event is IdleStateEvent, state is READ_IDLE.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws InterruptedException will not throw
     * @throws TimeoutException will not throw
     */
    @Test
    public void testUserEventTriggeredIdleStateEventReadIdle()
            throws IllegalArgumentException, IllegalAccessException, InterruptedException, TimeoutException {
        final ImapFuture<ImapAsyncCreateSessionResponse> imapFuture = new ImapFuture<ImapAsyncCreateSessionResponse>();
        final Logger logger = Mockito.mock(Logger.class);

        final String sessCtx = "Titanosauria@long.neck";
        final ImapClientConnectHandler handler = new ImapClientConnectHandler(imapFuture, logger, DebugMode.DEBUG_ON, SESSION_ID, sessCtx);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        final IdleStateEvent idleEvent = Mockito.mock(IdleStateEvent.class);
        Mockito.when(idleEvent.state()).thenReturn(IdleState.READER_IDLE);
        handler.userEventTriggered(ctx, idleEvent);

        // verify ChannelHandlerContext close happens
        Mockito.verify(ctx, Mockito.times(1)).close();
        Assert.assertTrue(imapFuture.isDone(), "Future should be done");
        ExecutionException ex = null;
        try {
            imapFuture.get(5, TimeUnit.MILLISECONDS);
        } catch (final ExecutionException ee) {
            ex = ee;
        }
        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        final Throwable cause = ex.getCause();
        Assert.assertNotNull(cause, "Expect cause.");
        Assert.assertEquals(cause.getClass(), ImapAsyncClientException.class, "Expected result mismatched.");
        final ImapAsyncClientException aEx = (ImapAsyncClientException) cause;
        Assert.assertEquals(aEx.getFaiureType(), FailureType.CONNECTION_FAILED_EXCEED_IDLE_MAX, "Falure type mismatched");

        // call channelInactive, should not encounter npe
        handler.channelInactive(ctx);
    }

    /**
     * Tests userEventTriggered method and the event is IdleStateEvent, state is NOT READ_IDLE.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testUserEventTriggeredIdleStateEventNotReadIdle() throws IllegalArgumentException, IllegalAccessException {
        final ImapFuture<ImapAsyncCreateSessionResponse> imapFuture = new ImapFuture<ImapAsyncCreateSessionResponse>();
        final Logger logger = Mockito.mock(Logger.class);

        final String sessCtx = "Titanosauria@long.neck";
        final ImapClientConnectHandler handler = new ImapClientConnectHandler(imapFuture, logger, DebugMode.DEBUG_ON, SESSION_ID, sessCtx);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        final IdleStateEvent idleEvent = Mockito.mock(IdleStateEvent.class);
        Mockito.when(idleEvent.state()).thenReturn(IdleState.WRITER_IDLE);
        handler.userEventTriggered(ctx, idleEvent);
        Assert.assertFalse(imapFuture.isDone(), "Future should NOT be done");
    }

    /**
     * Tests userEventTriggered method and the event is NOT IdleStateEvent.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testUserEventTriggeredNotIdleStateEvent() throws IllegalArgumentException, IllegalAccessException {
        final ImapFuture<ImapAsyncCreateSessionResponse> imapFuture = new ImapFuture<ImapAsyncCreateSessionResponse>();
        final Logger logger = Mockito.mock(Logger.class);

        final String sessCtx = "Titanosauria@long.neck";
        final ImapClientConnectHandler handler = new ImapClientConnectHandler(imapFuture, logger, DebugMode.DEBUG_ON, SESSION_ID, sessCtx);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        final String otherEvent = new String("king is coming!!!");
        handler.userEventTriggered(ctx, otherEvent);
        Assert.assertFalse(imapFuture.isDone(), "Future should NOT be done");
    }

    /**
     * Tests channelInactive method.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws InterruptedException will not throw
     * @throws TimeoutException will not throw
     */
    @Test
    public void testChannelInactive() throws IllegalArgumentException, IllegalAccessException, InterruptedException, TimeoutException {
        final ImapFuture<ImapAsyncCreateSessionResponse> imapFuture = new ImapFuture<ImapAsyncCreateSessionResponse>();
        final Logger logger = Mockito.mock(Logger.class);

        final String sessCtx = "Titanosauria@long.neck";
        final ImapClientConnectHandler handler = new ImapClientConnectHandler(imapFuture, logger, DebugMode.DEBUG_ON, SESSION_ID, sessCtx);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        handler.channelInactive(ctx);

        Assert.assertTrue(imapFuture.isDone(), "Future should be done");
        ExecutionException ex = null;
        try {
            imapFuture.get(5, TimeUnit.MILLISECONDS);
        } catch (final ExecutionException ee) {
            ex = ee;
        }
        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        Assert.assertNotNull(ex.getCause(), "Expect cause.");
        Assert.assertEquals(ex.getCause().getClass(), ImapAsyncClientException.class, "Expected result mismatched.");

        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(handler), "Cleanup should set " + field.getName() + " as null");
        }

        // call channelInactive again, should not encounter npe
        handler.channelInactive(ctx);
    }
}