package com.yahoo.imapnio.async.request;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;

/**
 * Unit test for {@link IdleCommand}.
 */
public class IdleCommandTest {

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = IdleCommand.class;
        fieldsToCheck = new HashSet<>();
        for (Class<?> c = classUnderTest; c != null; c = c.getSuperclass()) {
            for (final Field declaredField : c.getDeclaredFields()) {
                if (!declaredField.getType().isPrimitive() && !Modifier.isStatic(declaredField.getModifiers())) {
                    declaredField.setAccessible(true);
                    fieldsToCheck.add(declaredField);
                }
            }
        }
    }

    /**
     * Tests getCommandLine method.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLine() throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        final ConcurrentLinkedQueue<IMAPResponse> serverStreamingResponses = new ConcurrentLinkedQueue<IMAPResponse>();
        final ImapRequest cmd = new IdleCommand(serverStreamingResponses);
        Assert.assertEquals(cmd.getCommandLine(), "IDLE\r\n", "Expected result mismatched.");
        Assert.assertFalse(cmd.isCommandLineDataSensitive(), "Expected result mismatched.");
        Assert.assertNull(cmd.getDebugData(), "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getStreamingResponsesQueue method.
     */
    @Test
    public void testGetStreamingResponsesQueue() {
        final ConcurrentLinkedQueue<IMAPResponse> serverStreamingResponses = new ConcurrentLinkedQueue<IMAPResponse>();
        final ImapRequest cmd = new IdleCommand(serverStreamingResponses);
        Assert.assertEquals(cmd.getStreamingResponsesQueue(), serverStreamingResponses, "Expected result mismatched.");
    }

    /**
     * Tests getNextCommandLineAfterContinuation method.
     */
    @Test
    public void testGetNextCommandLineAfterContinuation() {
        final ConcurrentLinkedQueue<IMAPResponse> serverStreamingResponses = new ConcurrentLinkedQueue<IMAPResponse>();
        final IdleCommand cmd = new IdleCommand(serverStreamingResponses);
        final IMAPResponse serverResponse = null; // null or not null does not matter
        final ByteBuf s = cmd.getNextCommandLineAfterContinuation(serverResponse);
        Assert.assertNull(s, "Expect exception to be thrown.");
    }

    /**
     * Tests getTerminateCommandLine method.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetTerminateCommandLine() throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        final ConcurrentLinkedQueue<IMAPResponse> serverStreamingResponses = new ConcurrentLinkedQueue<IMAPResponse>();
        final ImapRequest cmd = new IdleCommand(serverStreamingResponses);
        Assert.assertEquals(cmd.getTerminateCommandLine().toString(StandardCharsets.US_ASCII), "DONE\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandType method.
     */
    @Test
    public void testGetCommandType() {
        final ConcurrentLinkedQueue<IMAPResponse> serverStreamingResponses = new ConcurrentLinkedQueue<IMAPResponse>();
        final ImapRequest cmd = new IdleCommand(serverStreamingResponses);
        Assert.assertSame(cmd.getCommandType(), ImapRFCSupportedCommandType.IDLE);
    }
}