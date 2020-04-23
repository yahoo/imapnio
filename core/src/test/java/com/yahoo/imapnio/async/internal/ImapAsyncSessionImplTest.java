package com.yahoo.imapnio.async.internal;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
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

import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
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
import com.yahoo.imapnio.async.request.AuthXoauth2Command;
import com.yahoo.imapnio.async.request.CapaCommand;
import com.yahoo.imapnio.async.request.IdleCommand;
import com.yahoo.imapnio.async.request.ImapRequest;
import com.yahoo.imapnio.async.response.ImapAsyncResponse;
import com.yahoo.imapnio.async.response.ImapResponseMapper;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.compression.JdkZlibDecoder;
import io.netty.handler.codec.compression.JdkZlibEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * Unit test for {@link ImapAsyncSessionImpl}.
 */
public class ImapAsyncSessionImplTest {

    /** Dummy session id. */
    private static final Long SESSION_ID = Long.valueOf(123456);

    /** Dummy user id. */
    private static final String USER_ID = "Argentinosaurus@long.enough";

    /** Timeout in milliseconds for making get on future. */
    private static final long FUTURE_GET_TIMEOUT_MILLIS = 5L;

    /** Time sequence for the clock tick in milliseconds. */
    private static final Long[] TIME_SEQUENCE = { 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L,
            23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L, 31L, 32L, 33L, 34L, 35L, 36L, 37L, 38L, 39L, 40L, 41L, 42L, 43L, 44L, 45L, 46L, 47L, 48L, 49L,
            50L, 51L, 52L, 53L, 54L, 55L, 56L, 57L, 58L, 59L, 60L };

    /** Clock instance. */
    private Clock clock;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> c = ImapAsyncSessionImpl.class;
        /** Fields to check for cleanup. */
        final Set<Field> fieldsToCheck = new HashSet<>();

        for (final Field declaredField : c.getDeclaredFields()) {
            if (!declaredField.getType().isPrimitive() && !Modifier.isStatic(declaredField.getModifiers())) {
                declaredField.setAccessible(true);
                fieldsToCheck.add(declaredField);
            }
        }
    }

    /**
     * Sets up instance before each test method.
     */
    @BeforeMethod
    public void beforeMethod() {
        clock = Mockito.mock(Clock.class);
        Mockito.when(clock.millis()).thenReturn(1L, TIME_SEQUENCE);
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
     */
    @Test
    public void testExecuteAuthCapaAndFlushHandleResponseCloseSessionAllSuccess()
            throws ImapAsyncClientException, IOException, ProtocolException, InterruptedException, ExecutionException, TimeoutException {

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

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_OFF, SESSION_ID, pipeline, sessionCtx);

        // execute Authenticate plain command
        {
            final Map<String, List<String>> capas = new HashMap<String, List<String>>();
            final ImapRequest cmd = new AuthPlainCommand("orange", "juicy", new Capability(capas));
            final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);
            Mockito.verify(authWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
            Mockito.verify(logger, Mockito.times(0)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

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
            Mockito.verify(logger, Mockito.times(0)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
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
            Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

            // simulate write to server completed successfully
            Mockito.when(capaWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(capaWritePromise);

            // handle server response
            final IMAPResponse serverResp1 = new IMAPResponse(
                    "* CAPABILITY IMAP4rev1 SASL-IR AUTH=PLAIN AUTH=XOAUTH2 AUTH=OAUTHBEARER ID MOVE NAMESPACE");
            aSession.handleChannelResponse(serverResp1);

            final IMAPResponse serverRespJunk = new IMAPResponse("@@@@@* some junk MOVE NAMESPACE");
            aSession.handleChannelResponse(serverRespJunk);
            Assert.assertFalse(future.isDone(), "isDone() should be false.");

            final IMAPResponse anotherTaggedResp = new IMAPResponse("a1 OK but not the tag u sent!");
            aSession.handleChannelResponse(anotherTaggedResp);
            Assert.assertFalse(future.isDone(), "isDone() should be false.");

            final IMAPResponse serverResp2 = new IMAPResponse("a2 OK CAPABILITY completed");
            aSession.handleChannelResponse(serverResp2);

            // verify that future should be done now
            Assert.assertTrue(future.isDone(), "isDone() should be true now");
            final ImapAsyncResponse asyncResp = future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            final Collection<IMAPResponse> lines = asyncResp.getResponseLines();
            Assert.assertEquals(lines.size(), 4, "responses count mismatched.");
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

            // first is junk response
            final IMAPResponse resp1 = it.next();
            Assert.assertNotNull(resp1, "Result mismatched.");
            Assert.assertEquals(resp1, serverRespJunk, "response mismatched.");

            // 2nd is a tagged response but tag does not match a2
            final IMAPResponse resp2 = it.next();
            Assert.assertNotNull(resp2, "Result mismatched.");
            Assert.assertEquals(resp2, anotherTaggedResp, "response mismatched.");

            final IMAPResponse endingResp = it.next();
            Assert.assertNotNull(endingResp, "Result mismatched.");
            Assert.assertTrue(endingResp.isOK(), "Response.isOK() mismatched.");
            Assert.assertEquals(endingResp.getTag(), "a2", "tag mismatched.");
            // verify logging messages
            final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(logger, Mockito.times(5)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                    allArgsCapture.capture());

            // since it is vargs, 5 calls with 3 parameters all accumulate to one list
            final List<Object> logArgs = allArgsCapture.getAllValues();
            Assert.assertNotNull(logArgs, "log messages mismatched.");
            Assert.assertEquals(logArgs.size(), 15, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(2), "a2 CAPABILITY\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(5), "* CAPABILITY IMAP4rev1 SASL-IR AUTH=PLAIN AUTH=XOAUTH2 AUTH=OAUTHBEARER ID MOVE NAMESPACE",
                    "log messages from server mismatched.");
            Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(8), "@@@@@* some junk MOVE NAMESPACE", "Error message mismatched.");
            Assert.assertEquals(logArgs.get(9), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(10), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(11), "a1 OK but not the tag u sent!", "Error message mismatched.");
            Assert.assertEquals(logArgs.get(12), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(13), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(14), "a2 OK CAPABILITY completed", "Error message mismatched.");
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
     * Tests Xoauth2 command with SASL-IR disabled. Verifies the logging message.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     */
    @Test
    public void testExecuteAuthXoauth2InvalidTokenNoSASLIR()
            throws ImapAsyncClientException, IOException, ProtocolException, InterruptedException, ExecutionException, TimeoutException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise authWritePromise = Mockito.mock(ChannelPromise.class); // first
        final ChannelPromise authWritePromise2 = Mockito.mock(ChannelPromise.class); // after +
        Mockito.when(channel.newPromise()).thenReturn(authWritePromise).thenReturn(authWritePromise2);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isTraceEnabled()).thenReturn(true);

        // construct, both class level and session level debugging are off
        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_OFF, SESSION_ID, pipeline, sessionCtx);

        // execute Authenticate XOAUTH2 command
        {
            final Map<String, List<String>> capas = new HashMap<String, List<String>>();
            final ImapRequest cmd = new AuthXoauth2Command("orange", "someToken", new Capability(capas));
            final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);
            Mockito.verify(authWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
            Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

            // simulate write to server completed successfully
            Mockito.when(authWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(authWritePromise);

            // handle server response
            final IMAPResponse serverResp1 = new IMAPResponse("+");
            // following will call getNextCommandLineAfterContinuation
            aSession.handleChannelResponse(serverResp1);
            Mockito.verify(channel, Mockito.times(2)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));

            // invalid token message from server
            final IMAPResponse serverResp2 = new IMAPResponse(
                    "+ eyJzdGF0dXMiOiI0MDAiLCJzY2hlbWVzIjoiQmVhcmVyIiwic2NvcGUiOiJodHRwczovL21haWwuZ29vZ2xlLmNvbS8ifQ==");
            aSession.handleChannelResponse(serverResp2);

            final IMAPResponse serverResp3 = new IMAPResponse("a1 BAD Invalid SASL argument.");
            aSession.handleChannelResponse(serverResp3);

            // verify that future should be done now
            Assert.assertTrue(future.isDone(), "isDone() should be true now");
            final ImapAsyncResponse asyncResp = future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            final Collection<IMAPResponse> lines = asyncResp.getResponseLines();
            Assert.assertEquals(lines.size(), 3, "responses count mismatched.");

            final Iterator<IMAPResponse> it = lines.iterator();
            // first response is +
            final IMAPResponse continuationResp1 = it.next();
            Assert.assertNotNull(continuationResp1, "Result mismatched.");
            Assert.assertTrue(continuationResp1.isContinuation(), "Response.isContinuation() mismatched.");

            // second response is "+ eyJzdGF0dXMiOiI0MDAiLCJzY2hlbWVzIjoiQmVhcmVyIiwic2NvcGUiOiJodHRwczovL21haWwuZ29vZ2xlLmNvbS8ifQ=="
            final IMAPResponse continuationResp2 = it.next();
            Assert.assertNotNull(continuationResp2, "Result mismatched.");
            Assert.assertTrue(continuationResp2.isContinuation(), "Response.isContinuation() mismatched.");
            Assert.assertEquals(continuationResp2.getRest(),
                    "eyJzdGF0dXMiOiI0MDAiLCJzY2hlbWVzIjoiQmVhcmVyIiwic2NvcGUiOiJodHRwczovL21haWwuZ29vZ2xlLmNvbS8ifQ==",
                    "server response mismatched.");

            // 3rd response is
            final IMAPResponse endingResp = it.next();
            Assert.assertNotNull(endingResp, "Result mismatched.");
            Assert.assertTrue(endingResp.isBAD(), "Response.isOK() mismatched.");
            Assert.assertEquals(endingResp.getTag(), "a1", "tag mismatched.");

            // verify logging messages
            final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(logger, Mockito.times(6)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                    allArgsCapture.capture());

            // since it is vargs, 6 calls to debug() with 3 parameters all accumulate to one list, 6 * 3 =18
            final List<Object> logArgs = allArgsCapture.getAllValues();
            Assert.assertNotNull(logArgs, "log messages mismatched.");
            Assert.assertEquals(logArgs.size(), 18, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(2), "a1 AUTHENTICATE XOAUTH2\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(5), "+", "log messages from server mismatched.");
            Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(8), "AUTHENTICATE XOAUTH2 FOR USER:orange", "Error message mismatched.");
            Assert.assertEquals(logArgs.get(9), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(10), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(11), "+ eyJzdGF0dXMiOiI0MDAiLCJzY2hlbWVzIjoiQmVhcmVyIiwic2NvcGUiOiJodHRwczovL21haWwuZ29vZ2xlLmNvbS8ifQ==",
                    "Error message mismatched.");
            Assert.assertEquals(logArgs.get(12), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(13), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(14), "*\r\n", "log messages from server mismatched.");
            Assert.assertEquals(logArgs.get(15), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(16), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(17), "a1 BAD Invalid SASL argument.", "Error message mismatched.");
        }
    }

    /**
     * Tests Xoauth2 command with SASL-IR enabled. Verifies the logging message.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     */
    @Test
    public void testExecuteAuthXoauth2InvalidTokenSASLIREnabled()
            throws ImapAsyncClientException, IOException, ProtocolException, InterruptedException, ExecutionException, TimeoutException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise authWritePromise = Mockito.mock(ChannelPromise.class); // first
        final ChannelPromise authWritePromise2 = Mockito.mock(ChannelPromise.class); // after +
        Mockito.when(channel.newPromise()).thenReturn(authWritePromise).thenReturn(authWritePromise2);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isTraceEnabled()).thenReturn(true);

        // construct, both class level and session level debugging are off
        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_OFF, SESSION_ID, pipeline, sessionCtx);

        // execute Authenticate XOAUTH2 command
        {
            final Map<String, List<String>> capas = new HashMap<String, List<String>>();
            capas.put("SASL-IR", Collections.singletonList("SASL-IR"));
            final ImapRequest cmd = new AuthXoauth2Command("orange", "someToken", new Capability(capas));
            final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);
            Mockito.verify(authWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
            Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

            // simulate write to server completed successfully
            Mockito.when(authWritePromise.isSuccess()).thenReturn(true);
            aSession.operationComplete(authWritePromise);

            // invalid token message from server
            final IMAPResponse serverResp2 = new IMAPResponse(
                    "+ eyJzdGF0dXMiOiI0MDAiLCJzY2hlbWVzIjoiQmVhcmVyIiwic2NvcGUiOiJodHRwczovL21haWwuZ29vZ2xlLmNvbS8ifQ==");
            aSession.handleChannelResponse(serverResp2);

            final IMAPResponse serverResp3 = new IMAPResponse("a1 BAD Invalid SASL argument.");
            aSession.handleChannelResponse(serverResp3);

            // verify that future should be done now
            Assert.assertTrue(future.isDone(), "isDone() should be true now");
            final ImapAsyncResponse asyncResp = future.get(FUTURE_GET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            final Collection<IMAPResponse> lines = asyncResp.getResponseLines();
            Assert.assertEquals(lines.size(), 2, "responses count mismatched.");

            final Iterator<IMAPResponse> it = lines.iterator();

            // second response is "+ eyJzdGF0dXMiOiI0MDAiLCJzY2hlbWVzIjoiQmVhcmVyIiwic2NvcGUiOiJodHRwczovL21haWwuZ29vZ2xlLmNvbS8ifQ=="
            final IMAPResponse continuationResp2 = it.next();
            Assert.assertNotNull(continuationResp2, "Result mismatched.");
            Assert.assertTrue(continuationResp2.isContinuation(), "Response.isContinuation() mismatched.");
            Assert.assertEquals(continuationResp2.getRest(),
                    "eyJzdGF0dXMiOiI0MDAiLCJzY2hlbWVzIjoiQmVhcmVyIiwic2NvcGUiOiJodHRwczovL21haWwuZ29vZ2xlLmNvbS8ifQ==",
                    "server response mismatched.");

            // 3rd response is
            final IMAPResponse endingResp = it.next();
            Assert.assertNotNull(endingResp, "Result mismatched.");
            Assert.assertTrue(endingResp.isBAD(), "Response.isOK() mismatched.");
            Assert.assertEquals(endingResp.getTag(), "a1", "tag mismatched.");

            // verify logging messages
            final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(logger, Mockito.times(4)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                    allArgsCapture.capture());

            // since it is vargs, 4 calls to debug() with 3 parameters all accumulate to one list, 4 * 3 =12
            final List<Object> logArgs = allArgsCapture.getAllValues();
            Assert.assertNotNull(logArgs, "log messages mismatched.");
            Assert.assertEquals(logArgs.size(), 12, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(2), "AUTHENTICATE XOAUTH2 FOR USER:orange", "Error message mismatched.");
            Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(5), "+ eyJzdGF0dXMiOiI0MDAiLCJzY2hlbWVzIjoiQmVhcmVyIiwic2NvcGUiOiJodHRwczovL21haWwuZ29vZ2xlLmNvbS8ifQ==",
                    "Error message mismatched.");
            Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(8), "*\r\n", "log messages from server mismatched.");
            Assert.assertEquals(logArgs.get(9), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(10), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(11), "a1 BAD Invalid SASL argument.", "Error message mismatched.");
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
     */
    @Test
    public void testExecuteAuthCompressHandleResponseNoSslHandler()
            throws ImapAsyncClientException, IOException, ProtocolException, InterruptedException, ExecutionException, TimeoutException {

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

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_OFF, SESSION_ID, pipeline, sessionCtx);

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
            final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(logger, Mockito.times(4)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                    allArgsCapture.capture());

            // since it is vargs, 4 calls with 3 parameters all accumulate to one list, 4 * 3 =12
            final List<Object> logArgs = allArgsCapture.getAllValues();
            Assert.assertNotNull(logArgs, "log messages mismatched.");
            Assert.assertEquals(logArgs.size(), 12, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(2), "a1 AUTHENTICATE PLAIN\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(5), "+", "log messages from server mismatched.");
            Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(8), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(9), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(10), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(11), "a1 OK AUTHENTICATE completed", "Error message mismatched.");
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
            final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(logger, Mockito.times(6)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                    allArgsCapture.capture());

            // since it is vargs, 6 calls with 3 parameters all accumulate to one list, 6 * 3 =18
            final List<Object> logArgs = allArgsCapture.getAllValues();
            Assert.assertNotNull(logArgs, "log messages mismatched.");
            Assert.assertEquals(logArgs.size(), 18, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(2), "a1 AUTHENTICATE PLAIN\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(5), "+", "log messages from server mismatched.");
            Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(8), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(9), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(10), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(11), "a1 OK AUTHENTICATE completed", "Error message mismatched.");
            Assert.assertEquals(logArgs.get(12), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(13), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(14), "a2 COMPRESS DEFLATE\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(15), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(16), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(17), "a2 OK Success", "log messages from server mismatched.");
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
     */
    @Test
    public void testExecuteAuthCompressHandleResponseWithSslHandler()
            throws ImapAsyncClientException, IOException, ProtocolException, InterruptedException, ExecutionException, TimeoutException {

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

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline, sessionCtx);

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
            final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(logger, Mockito.times(4)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                    allArgsCapture.capture());

            // since it is vargs, 4 calls with 3 parameters all accumulate to one list, 4 * 3 =12
            final List<Object> logArgs = allArgsCapture.getAllValues();
            Assert.assertNotNull(logArgs, "log messages mismatched.");
            Assert.assertEquals(logArgs.size(), 12, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(2), "a1 AUTHENTICATE PLAIN\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(5), "+", "log messages from server mismatched.");
            Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(8), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(9), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(10), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(11), "a1 OK AUTHENTICATE completed", "Error message mismatched.");
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
            final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(logger, Mockito.times(6)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                    allArgsCapture.capture());

            // since it is vargs, 6 calls with 3 parameters all accumulate to one list, 6 * 3 =18
            final List<Object> logArgs = allArgsCapture.getAllValues();
            Assert.assertNotNull(logArgs, "log messages mismatched.");
            Assert.assertEquals(logArgs.size(), 18, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(2), "a1 AUTHENTICATE PLAIN\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(5), "+", "log messages from server mismatched.");
            Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(8), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(9), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(10), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(11), "a1 OK AUTHENTICATE completed", "Error message mismatched.");
            Assert.assertEquals(logArgs.get(12), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(13), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(14), "a2 COMPRESS DEFLATE\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(15), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(16), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(17), "a2 OK Success", "log messages from server mismatched.");
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
     */
    @Test
    public void testExecuteAuthCompressFailedHandleResponse()
            throws ImapAsyncClientException, IOException, ProtocolException, InterruptedException, ExecutionException, TimeoutException {

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

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline, sessionCtx);

        // execute Authenticate plain command
        {
            final Map<String, List<String>> capas = new HashMap<String, List<String>>();
            final ImapRequest cmd = new AuthPlainCommand("orange", "juicy", new Capability(capas));
            final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);
            Mockito.verify(authWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
            Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

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
            final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(logger, Mockito.times(4)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                    allArgsCapture.capture());

            // since it is vargs, 4 calls with 3 parameters all accumulate to one list, 4 * 3 =12
            final List<Object> logArgs = allArgsCapture.getAllValues();
            Assert.assertNotNull(logArgs, "log messages mismatched.");
            Assert.assertEquals(logArgs.size(), 12, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(2), "a1 AUTHENTICATE PLAIN\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(5), "+", "log messages from server mismatched.");
            Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(8), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(9), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(10), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(11), "a1 OK AUTHENTICATE completed", "Error message mismatched.");
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
            final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(logger, Mockito.times(6)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                    allArgsCapture.capture());

            // since it is vargs, 6 calls with 3 parameters all accumulate to one list, 6 * 3 =18
            final List<Object> logArgs = allArgsCapture.getAllValues();
            Assert.assertNotNull(logArgs, "log messages mismatched.");
            Assert.assertEquals(logArgs.size(), 18, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(2), "a1 AUTHENTICATE PLAIN\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(5), "+", "log messages from server mismatched.");
            Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(8), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(9), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(10), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(11), "a1 OK AUTHENTICATE completed", "Error message mismatched.");
            Assert.assertEquals(logArgs.get(12), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(13), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(14), "a2 COMPRESS DEFLATE\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(15), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(16), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(17), "a2 NO Success", "log messages from server mismatched.");
        }
    }

    /**
     * Tests server idle event happens while command queue is NOT empty and command in queue is in REQUEST_SENT state.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws TimeoutException will not throw
     * @throws InterruptedException will not throw
     */
    @Test
    public void testHandleIdleEventQueueNotEmptyAndCommandSentToServer() throws ImapAsyncClientException, InterruptedException, TimeoutException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise writePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(writePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // construct, turn on session level debugging by having logger.isDebugEnabled() true and session level debug on

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline, sessionCtx);

        // execute
        final ImapRequest cmd = new CapaCommand();
        final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);

        // simulate command sent to server
        final ChannelFuture writeCompleteFuture = Mockito.mock(ChannelFuture.class);
        Mockito.when(writeCompleteFuture.isSuccess()).thenReturn(true);
        aSession.operationComplete(writeCompleteFuture);

        // idle event happened
        final IdleStateEvent idleEvent = null;
        aSession.handleIdleEvent(idleEvent);

        // verify that future should be done now since channel timeout exception happens
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
        Assert.assertEquals(asynEx.getFailureType(), FailureType.CHANNEL_TIMEOUT, "Failure type mismatched.");
        Assert.assertEquals(asynEx.getMessage(),
                "failureType=CHANNEL_TIMEOUT,sId=123456,uId=Argentinosaurus@long.enough,cmdTag:a1,cmdType:CAPABILITY,cmdSent:1",
                "Error message mismatched.");
    }

    /**
     * Tests server idle event happens while command queue is NOT empty, and command is in NOT_SENT state.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testHandleIdleEventQueueNotEmptyCommandNotSentToServer() throws ImapAsyncClientException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise writePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(writePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // construct, turn on session level debugging by having logger.isDebugEnabled() true and session level debug on

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline, sessionCtx);

        // execute
        final ImapRequest cmd = new CapaCommand();
        final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);

        // idle event is triggered but command in queue is in NOT_SENT state
        final IdleStateEvent idleEvent = null;
        aSession.handleIdleEvent(idleEvent);

        // verify that future should NOT be done since channel timeout exception did not happen
        Assert.assertFalse(future.isDone(), "isDone() should be true now");
    }

    /**
     * Tests server idles event happens while command queue is empty.
     *
     */
    @Test
    public void testHandleIdleEventQueueEmpty() {

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

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline, sessionCtx);

        // command queue is empty
        final IdleStateEvent idleEvent = null;
        aSession.handleIdleEvent(idleEvent);
    }

    /**
     * Tests constructing the session, executing, flushing to server failed.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     */
    @Test
    public void testExecuteAndFlushToServerFailedCloseSessionFailed()
            throws ImapAsyncClientException, InterruptedException, ExecutionException, TimeoutException {

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

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline, sessionCtx);

        // execute
        final ImapRequest cmd = new CapaCommand();
        final ImapFuture<ImapAsyncResponse> cmdFuture = aSession.execute(cmd);

        Mockito.verify(writeToServerPromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
        Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

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
            Assert.assertEquals(asynEx.getFailureType(), FailureType.CHANNEL_EXCEPTION, "Failure type mismatched.");
            Assert.assertEquals(asynEx.getMessage(),
                    "failureType=CHANNEL_EXCEPTION,sId=123456,uId=Argentinosaurus@long.enough,cmdTag:a1,cmdType:CAPABILITY,cmdSent:1",
                    "Error message mismatched.");
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
        final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(logger, Mockito.times(4)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                allArgsCapture.capture());

        // since it is vargs, 3 calls with 3 parameters all accumulate to one list
        final List<Object> logArgs = allArgsCapture.getAllValues();
        Assert.assertNotNull(logArgs, "log messages mismatched.");
        Assert.assertEquals(logArgs.size(), 12, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(2), "a1 CAPABILITY\r\n", "log messages from client mismatched.");
        Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(5).getClass(), ImapAsyncClientException.class, "class mismatched.");
        final ImapAsyncClientException e = (ImapAsyncClientException) logArgs.get(5);
        Assert.assertEquals(e.getFailureType(), FailureType.CHANNEL_EXCEPTION, "Class mismatched.");
        Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(8), "Closing the session via close().", "log messages from client mismatched.");
        Assert.assertEquals(logArgs.get(9), SESSION_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(10), USER_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(11), "Session is confirmed closed.", "Error message mismatched.");

        final ArgumentCaptor<Object> errCapture = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(logger, Mockito.times(0)).error(Mockito.anyString(), errCapture.capture(), errCapture.capture(), errCapture.capture());

        // calling setDebugMode() on a closed session, should not throw NPE
        aSession.setDebugMode(DebugMode.DEBUG_ON);
    }

    /**
     * Tests constructing the session, execute, and channel is closed abruptly before server response is back.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws TimeoutException will not throw
     * @throws InterruptedException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testExecuteChannelCloseBeforeServerResponseArrived()
            throws ImapAsyncClientException, InterruptedException, TimeoutException, IllegalArgumentException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise writePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(writePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // construct, class level logging is off, session level logging is on

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline, sessionCtx);

        // execute
        final ImapRequest cmd = new CapaCommand();
        final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);

        Mockito.verify(writePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
        Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

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
        Assert.assertEquals(asynEx.getFailureType(), FailureType.CHANNEL_DISCONNECTED, "Failure type mismatched.");
        Assert.assertEquals(asynEx.getMessage(),
                "failureType=CHANNEL_DISCONNECTED,sId=123456,uId=Argentinosaurus@long.enough,cmdTag:a1,cmdType:CAPABILITY,cmdSent:0",
                "Error message mismatched.");

        // verify logging messages
        final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(logger, Mockito.times(4)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                allArgsCapture.capture());

        // since it is vargs, 4 calls with 3 parameters all accumulate to one list
        final List<Object> logArgs = allArgsCapture.getAllValues();
        Assert.assertNotNull(logArgs, "log messages mismatched.");
        Assert.assertEquals(logArgs.size(), 12, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(2), "a1 CAPABILITY\r\n", "log messages from client mismatched.");
        Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(5), "Session is confirmed closed.", "log messages from client mismatched.");
        Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(8).getClass(), ImapAsyncClientException.class, "class mismatched.");
        final ImapAsyncClientException e = (ImapAsyncClientException) logArgs.get(8);
        Assert.assertNotNull(e, "Log error for exception is missing");
        Assert.assertEquals(e.getFailureType(), FailureType.CHANNEL_DISCONNECTED, "Class mismatched.");
        Assert.assertEquals(logArgs.get(9), SESSION_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(10), USER_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(11), "Closing the session via close().", "Error message mismatched.");

        final ArgumentCaptor<Object> errCapture = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(logger, Mockito.times(0)).error(Mockito.anyString(), errCapture.capture(), errCapture.capture(), errCapture.capture());

    }

    /**
     * Tests constructing the session, execute, and channel is closed abruptly before server response is back. In this test, the log level is in info,
     * not in debug, we want to make sure it does not call debug().
     *
     * @throws ImapAsyncClientException will not throw
     * @throws TimeoutException will not throw
     * @throws InterruptedException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testExecuteChannelCloseBeforeServerResponseArrivedLogLevelInfo()
            throws ImapAsyncClientException, InterruptedException, TimeoutException, IllegalArgumentException {

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

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline, sessionCtx);

        // execute
        final ImapRequest cmd = new CapaCommand();
        final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);

        Mockito.verify(writePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
        Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
        // Ensure there is no call to debug() method
        Mockito.verify(logger, Mockito.times(0)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

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
        Assert.assertEquals(asynEx.getFailureType(), FailureType.CHANNEL_DISCONNECTED, "Failure type mismatched.");
        // verify logging messages
        final ArgumentCaptor<Object> errCapture = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(logger, Mockito.times(0)).debug(Mockito.anyString(), errCapture.capture(), errCapture.capture(), errCapture.capture());
        Mockito.verify(logger, Mockito.times(0)).debug(Mockito.anyString(), Mockito.any(), Mockito.any());
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
     */
    @Test
    public void testExecuteIdleHandleResponseFlushCompleteTerminateSuccess()
            throws ImapAsyncClientException, IOException, ProtocolException, InterruptedException, ExecutionException, TimeoutException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise writePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(writePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        // construct

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline, sessionCtx);

        // execute
        final ConcurrentLinkedQueue<IMAPResponse> serverResponseQ = new ConcurrentLinkedQueue<IMAPResponse>();
        final ImapRequest cmd = new IdleCommand(serverResponseQ);
        ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);

        Mockito.verify(writePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
        Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

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
        Assert.assertEquals(ex.getFailureType(), FailureType.COMMAND_NOT_ALLOWED, "FailureType mismatched.");

        // verify logging messages
        final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(logger, Mockito.times(6)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                allArgsCapture.capture());

        // since it is vargs, 6 calls with 3 parameters all accumulate to one list, 6 * 3 =18
        final List<Object> logArgs = allArgsCapture.getAllValues();
        Assert.assertNotNull(logArgs, "log messages mismatched.");
        Assert.assertEquals(logArgs.size(), 18, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(2), "a1 IDLE\r\n", "log messages from client mismatched.");
        Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(5), "+ idling", "log messages from server mismatched.");
        Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(8), "* 2 EXPUNGE", "Error message mismatched.");
        Assert.assertEquals(logArgs.get(9), SESSION_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(10), USER_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(11), "* 3 EXISTS", "Error message mismatched.");
        Assert.assertEquals(logArgs.get(12), SESSION_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(13), USER_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(14), "DONE\r\n", "log messages from client mismatched.");
        Assert.assertEquals(logArgs.get(15), SESSION_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(16), USER_ID, "log messages mismatched.");
        Assert.assertEquals(logArgs.get(17), "a1 OK IDLE terminated", "log messages from server mismatched.");
    }

    /**
     * Tests execute method when command queue is not empty.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testExecuteFailedDueToQueueNotEmpty() throws ImapAsyncClientException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise writePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(writePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(true);

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline, sessionCtx);
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
        Assert.assertEquals(ex.getFailureType(), FailureType.COMMAND_NOT_ALLOWED, "Failure type mismatched.");
        Mockito.verify(logger, Mockito.times(1)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    /**
     * Tests execute method when channel is inactive, then call close() to close session.
     *
     * @throws TimeoutException will not throw
     * @throws ExecutionException will not throw
     * @throws InterruptedException will not throw
     */
    @Test
    public void testExecuteFailedChannelInactiveAndCloseChannel() throws InterruptedException, ExecutionException, TimeoutException {

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

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline, sessionCtx);
        final ImapRequest cmd = new CapaCommand();
        ImapAsyncClientException ex = null;
        try {
            // execute again, queue is not empty
            aSession.execute(cmd);
        } catch (final ImapAsyncClientException asyncEx) {
            ex = asyncEx;
        }
        Assert.assertNotNull(ex, "Exception should occur.");
        Assert.assertEquals(ex.getFailureType(), FailureType.OPERATION_PROHIBITED_ON_CLOSED_CHANNEL, "Failure type mismatched.");

        Mockito.verify(writePromise, Mockito.times(0)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
        Mockito.verify(channel, Mockito.times(0)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
        // encountering the above exception in execute(), will not log the command sent over the wire
        Mockito.verify(logger, Mockito.times(0)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

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
     * @throws TimeoutException will not throw
     * @throws InterruptedException will not throw
     */
    @Test
    public void testCloseSessionOperationCompleteFutureIsUnsuccessful() throws InterruptedException, TimeoutException {

        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(channel.isActive()).thenReturn(true);
        final ChannelPromise closePromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channel.newPromise()).thenReturn(closePromise);

        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logger.isDebugEnabled()).thenReturn(false);

        // construct, both class level and session level debugging are off

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_OFF, SESSION_ID, pipeline, sessionCtx);

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
        Assert.assertEquals(asyncClientEx.getFailureType(), FailureType.CLOSING_CONNECTION_FAILED, "Failure type mismatched.");
    }

    /**
     * Tests the whole life cycle flow: construct the session, execute, handle server response success, close session.
     *
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws TimeoutException will not throw
     * @throws InterruptedException will not throw
     */
    @Test
    public void testExecuteAuthHandleResponseChannelInactive()
            throws ImapAsyncClientException, IOException, ProtocolException, InterruptedException, TimeoutException {

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

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_OFF, SESSION_ID, pipeline, sessionCtx);

        // execute Authenticate plain command
        {
            final Map<String, List<String>> capas = new HashMap<String, List<String>>();
            final ImapRequest cmd = new AuthPlainCommand("orange", "juicy", new Capability(capas));
            final ImapFuture<ImapAsyncResponse> future = aSession.execute(cmd);
            Mockito.verify(authWritePromise, Mockito.times(1)).addListener(Mockito.any(ImapAsyncSessionImpl.class));
            Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.anyString(), Mockito.isA(ChannelPromise.class));
            Mockito.verify(logger, Mockito.times(0)).debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

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
            Assert.assertEquals(asynEx.getFailureType(), FailureType.CHANNEL_EXCEPTION, "Failure type mismatched.");

            // verify logging messages
            final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(logger, Mockito.times(3)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                    allArgsCapture.capture());

            // since it is vargs, 3 calls with 3 parameters all accumulate to one list, 3 * 3 =9
            final List<Object> logArgs = allArgsCapture.getAllValues();
            Assert.assertNotNull(logArgs, "log messages mismatched.");
            Assert.assertEquals(logArgs.size(), 9, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(2), "+", "log messages from server mismatched.");
            Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(5), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(8).getClass(), ImapAsyncClientException.class, "class mismatched.");
            final ImapAsyncClientException e = (ImapAsyncClientException) logArgs.get(8);
            Assert.assertNotNull(e, "Log error for exception is missing");
            Assert.assertEquals(e.getFailureType(), FailureType.CHANNEL_EXCEPTION, "Class mismatched.");

            final ArgumentCaptor<Object> errCapture = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(logger, Mockito.times(0)).error(Mockito.anyString(), errCapture.capture(), errCapture.capture(), errCapture.capture());

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
     */
    @Test
    public void testExecuteAuthCompressHandleResponseChannelIsClosed()
            throws ImapAsyncClientException, IOException, ProtocolException, InterruptedException, ExecutionException, TimeoutException {

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

        final String sessionCtx = USER_ID;
        final ImapAsyncSessionImpl aSession = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_OFF, SESSION_ID, pipeline, sessionCtx);

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
            final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(logger, Mockito.times(4)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                    allArgsCapture.capture());

            // since it is vargs, 4 calls with 3 parameters all accumulate to one list, 4 * 3 =12
            final List<Object> logArgs = allArgsCapture.getAllValues();
            Assert.assertNotNull(logArgs, "log messages mismatched.");
            Assert.assertEquals(logArgs.size(), 12, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(2), "a1 AUTHENTICATE PLAIN\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(5), "+", "log messages from server mismatched.");
            Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(8), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(9), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(10), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(11), "a1 OK AUTHENTICATE completed", "Error message mismatched.");
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
            Assert.assertEquals(asynEx.getFailureType(), FailureType.OPERATION_PROHIBITED_ON_CLOSED_CHANNEL, "Failure type mismatched.");

            // verify logging messages
            final ArgumentCaptor<Object> allArgsCapture = ArgumentCaptor.forClass(Object.class);
            Mockito.verify(logger, Mockito.times(7)).debug(Mockito.anyString(), allArgsCapture.capture(), allArgsCapture.capture(),
                    allArgsCapture.capture());

            // since it is vargs, 6 calls with 3 parameters all accumulate to one list, 6 * 3 =18
            final List<Object> logArgs = allArgsCapture.getAllValues();
            Assert.assertNotNull(logArgs, "log messages mismatched.");
            Assert.assertEquals(logArgs.size(), 21, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(0), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(1), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(2), "a1 AUTHENTICATE PLAIN\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(3), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(4), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(5), "+", "log messages from server mismatched.");
            Assert.assertEquals(logArgs.get(6), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(7), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(8), "AUTHENTICATE PLAIN FOR USER:orange", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(9), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(10), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(11), "a1 OK AUTHENTICATE completed", "Error message mismatched.");
            Assert.assertEquals(logArgs.get(12), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(13), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(14), "a2 COMPRESS DEFLATE\r\n", "log messages from client mismatched.");
            Assert.assertEquals(logArgs.get(15), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(16), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(17), "a2 OK Success", "log messages from server mismatched.");
            Assert.assertEquals(logArgs.get(18), SESSION_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(19), USER_ID, "log messages mismatched.");
            Assert.assertEquals(logArgs.get(20).getClass(), ImapAsyncClientException.class, "log messages from server mismatched.");
            final ImapAsyncClientException ex20 = (ImapAsyncClientException) logArgs.get(20);
            Assert.assertEquals(ex20.getFailureType(), FailureType.OPERATION_PROHIBITED_ON_CLOSED_CHANNEL, "Failure type mismatched.");
        }
    }

    /**
     * Tests DebugMode enum.
     */
    @Test
    public void testDebugModeEnum() {

        final DebugMode[] enumList = DebugMode.values();
        Assert.assertEquals(enumList.length, 2, "The enum count mismatched.");
        final DebugMode value = DebugMode.valueOf("DEBUG_OFF");
        Assert.assertSame(value, DebugMode.DEBUG_OFF, "Enum does not match.");
    }
}