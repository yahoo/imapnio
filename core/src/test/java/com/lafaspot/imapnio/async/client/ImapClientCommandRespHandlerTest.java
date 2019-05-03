package com.lafaspot.imapnio.async.client;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.async.client.ImapClientCommandRespHandler;
import com.lafaspot.imapnio.async.client.ImapCommandChannelEventProcessor;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * Unit test for {@code ImapClientCommandRespHandler}.
 */
public class ImapClientCommandRespHandlerTest {

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> c = ImapClientCommandRespHandler.class;
        fieldsToCheck = new HashSet<>();

        for (final Field declaredField : c.getDeclaredFields()) {
            if (!declaredField.getType().isPrimitive() && !Modifier.isStatic(declaredField.getModifiers())) {
                declaredField.setAccessible(true);
                fieldsToCheck.add(declaredField);
            }
        }
    }

    /**
     * Tests decode method.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ProtocolException will not throw
     * @throws IOException will not throw
     */
    @Test
    public void testDecode() throws IllegalArgumentException, IllegalAccessException, IOException, ProtocolException {
        final ImapCommandChannelEventProcessor processor = Mockito.mock(ImapCommandChannelEventProcessor.class);
        final ImapClientCommandRespHandler handler = new ImapClientCommandRespHandler(processor);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        final IMAPResponse resp = new IMAPResponse("a1 OK AUTHENTICATE completed");
        final List<Object> out = new ArrayList<Object>();
        handler.decode(ctx, resp, out);
        Mockito.verify(processor, Mockito.times(1)).handleChannelResponse(resp);
    }

    /**
     * Tests exceptionCaught method.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testExceptionCaught() throws IllegalArgumentException, IllegalAccessException {
        final ImapCommandChannelEventProcessor processor = Mockito.mock(ImapCommandChannelEventProcessor.class);
        final ImapClientCommandRespHandler handler = new ImapClientCommandRespHandler(processor);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        final TimeoutException ex = new TimeoutException("too late, my friend");
        handler.exceptionCaught(ctx, ex);
        Mockito.verify(processor, Mockito.times(1)).handleChannelException(ex);
    }

    /**
     * Tests userEventTriggered method and the event is IdleStateEvent, state is READ_IDLE.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testUserEventTriggeredIdleStateEventReadIdle() throws IllegalArgumentException, IllegalAccessException {
        final ImapCommandChannelEventProcessor processor = Mockito.mock(ImapCommandChannelEventProcessor.class);
        final ImapClientCommandRespHandler handler = new ImapClientCommandRespHandler(processor);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        final IdleStateEvent idleEvent = Mockito.mock(IdleStateEvent.class);
        Mockito.when(idleEvent.state()).thenReturn(IdleState.READER_IDLE);
        handler.userEventTriggered(ctx, idleEvent);
        Mockito.verify(processor, Mockito.times(1)).handleIdleEvent(idleEvent);
    }

    /**
     * Tests userEventTriggered method and the event is IdleStateEvent, state is NOT READ_IDLE.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testUserEventTriggeredIdleStateEventNotReadIdle() throws IllegalArgumentException, IllegalAccessException {
        final ImapCommandChannelEventProcessor processor = Mockito.mock(ImapCommandChannelEventProcessor.class);
        final ImapClientCommandRespHandler handler = new ImapClientCommandRespHandler(processor);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        final IdleStateEvent idleEvent = Mockito.mock(IdleStateEvent.class);
        Mockito.when(idleEvent.state()).thenReturn(IdleState.WRITER_IDLE);
        handler.userEventTriggered(ctx, idleEvent);
        Mockito.verify(processor, Mockito.times(0)).handleIdleEvent(Mockito.any(IdleStateEvent.class));
    }

    /**
     * Tests userEventTriggered method and the event is NOT IdleStateEvent.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testUserEventTriggeredNotIdleStateEvent() throws IllegalArgumentException, IllegalAccessException {
        final ImapCommandChannelEventProcessor processor = Mockito.mock(ImapCommandChannelEventProcessor.class);
        final ImapClientCommandRespHandler handler = new ImapClientCommandRespHandler(processor);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        final String otherEvent = new String("king is coming!!!");
        handler.userEventTriggered(ctx, otherEvent);
        Mockito.verify(processor, Mockito.times(0)).handleIdleEvent(Mockito.any(IdleStateEvent.class));
    }

    /**
     * Tests channelInactive method.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testChannelInactive() throws IllegalArgumentException, IllegalAccessException {
        final ImapCommandChannelEventProcessor processor = Mockito.mock(ImapCommandChannelEventProcessor.class);
        final ImapClientCommandRespHandler handler = new ImapClientCommandRespHandler(processor);

        final ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        handler.channelInactive(ctx);
        Mockito.verify(processor, Mockito.times(1)).handleChannelClosed();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(handler), "Cleanup should set " + field.getName() + " as null");
        }
    }
}