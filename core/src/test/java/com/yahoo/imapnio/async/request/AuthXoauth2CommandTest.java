package com.yahoo.imapnio.async.request;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.data.Capability;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;

/**
 * Unit test for {@link AuthXoauth2Command}.
 */
public class AuthXoauth2CommandTest {

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = AuthXoauth2Command.class;
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
    public void testGetCommandLineSaslIREnabled()
            throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        final String username = "tesla";
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Collections.singletonList(ImapClientConstants.SASL_IR));
        final ImapRequest cmd = new AuthXoauth2Command(username, token, new Capability(capas));
        Assert.assertTrue(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");

        Assert.assertEquals(cmd.getCommandLine(), "AUTHENTICATE XOAUTH2 dXNlcj10ZXNsYQFhdXRoPUJlYXJlciBzZWxmZHJpdmluZwEB\r\n",
                "Expected result mismatched.");
        Assert.assertTrue(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");
        Assert.assertEquals(cmd.getDebugData(), "AUTHENTICATE XOAUTH2 FOR USER:tesla", "Log line mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineSaslIRDisabled() throws IllegalArgumentException, IllegalAccessException, ImapAsyncClientException {
        final String username = "tesla";
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        final AuthXoauth2Command cmd = new AuthXoauth2Command(username, token, new Capability(capas));
        Assert.assertTrue(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");

        Assert.assertEquals(cmd.getCommandLine(), "AUTHENTICATE XOAUTH2\r\n", "Expected result mismatched.");
        Assert.assertFalse(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");

        // asks the next command after continuation
        final IMAPResponse serverResponse = null; // null or not null does not matter
        final ByteBuf resp2 = cmd.getNextCommandLineAfterContinuation(serverResponse);
        Assert.assertEquals(resp2.toString(StandardCharsets.US_ASCII), "dXNlcj10ZXNsYQFhdXRoPUJlYXJlciBzZWxmZHJpdmluZwEB\r\n",
                "Expected result mismatched.");
        Assert.assertTrue(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");

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
        final String username = "tesla";
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Collections.singletonList(ImapClientConstants.SASL_IR));
        final ImapRequest cmd = new AuthXoauth2Command(username, token, new Capability(capas));
        Assert.assertNull(cmd.getStreamingResponsesQueue(), "Expected result mismatched.");
    }

    /**
     * Tests getNextCommandLineAfterContinuation method.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     * @throws IOException will not throw
     */
    @Test
    public void testGetNextCommandLineAfterContinuation() throws ImapAsyncClientException, IOException, ProtocolException {
        final String username = "tesla";
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Collections.singletonList(ImapClientConstants.SASL_IR));
        final ImapRequest cmd = new AuthXoauth2Command(username, token, new Capability(capas));
        Assert.assertTrue(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");

        // asks for the first command, should contain client response since it is SASL_IR enabled
        Assert.assertEquals(cmd.getCommandLine(), "AUTHENTICATE XOAUTH2 dXNlcj10ZXNsYQFhdXRoPUJlYXJlciBzZWxmZHJpdmluZwEB\r\n",
                "Expected result mismatched.");
        Assert.assertTrue(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");

        // asks the next command after continuation with server error response encoded in base64
        final IMAPResponse serverResponse = new IMAPResponse(
                "+ eyJzdGF0dXMiOiI0MDAiLCJzY2hlbWVzIjoiQmVhcmVyIiwic2NvcGUiOiJodHRwczovL21haWwuZ29vZ2xlLmNvbS8ifQ==");
        final ByteBuf nextClientReq = cmd.getNextCommandLineAfterContinuation(serverResponse);
        Assert.assertNotNull(nextClientReq, "expected command from client mismatched.");
        Assert.assertEquals(nextClientReq.toString(StandardCharsets.US_ASCII), "*\r\n", "expected command from client mismatched.");
        Assert.assertFalse(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");
    }

    /**
     * Tests getTerminateCommandLine method.
     */
    @Test
    public void testGetTerminateCommandLine() {
        final String username = "tesla";
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Collections.singletonList(ImapClientConstants.SASL_IR));
        final ImapRequest cmd = new AuthXoauth2Command(username, token, new Capability(capas));
        ImapAsyncClientException ex = null;
        try {
            cmd.getTerminateCommandLine();
        } catch (final ImapAsyncClientException imapAsyncEx) {
            ex = imapAsyncEx;
        }
        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        Assert.assertEquals(ex.getFailureType(), ImapAsyncClientException.FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND,
                "Expected result mismatched.");
    }

    /**
     * Tests getCommandType method.
     */
    @Test
    public void testGetCommandType() {
        final String username = "tesla";
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Collections.singletonList(ImapClientConstants.SASL_IR));
        final ImapRequest cmd = new AuthXoauth2Command(username, token, new Capability(capas));
        Assert.assertSame(cmd.getCommandType(), ImapRFCSupportedCommandType.AUTHENTICATE);
    }
}