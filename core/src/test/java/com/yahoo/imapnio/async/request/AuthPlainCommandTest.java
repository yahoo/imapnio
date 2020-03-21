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
 * Unit test for {@link AuthPlainCommand}.
 */
public class AuthPlainCommandTest {

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = AuthPlainCommand.class;
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
     * Tests getCommandLine method when SASL-IR is enabled.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testGetCommandLineSASLIREnabled()
            throws IOException, IllegalArgumentException, IllegalAccessException, ImapAsyncClientException, ProtocolException {
        final String username = "tesla";
        final String pwd = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Collections.singletonList(ImapClientConstants.SASL_IR));
        final AuthPlainCommand cmd = new AuthPlainCommand(username, pwd, new Capability(capas));
        Assert.assertTrue(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");

        // verify getCommandLine()
        Assert.assertEquals(cmd.getCommandLine(), "AUTHENTICATE PLAIN AHRlc2xhAHNlbGZkcml2aW5n\r\n", "Expected result mismatched.");
        Assert.assertTrue(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");
        Assert.assertEquals(cmd.getDebugData(), "AUTHENTICATE PLAIN FOR USER:tesla", "Log line mismatched.");

        final IMAPResponse serverResponse = new IMAPResponse(
                "+ eyJzdGF0dXMiOiI0MDAiLCJzY2hlbWVzIjoiQmVhcmVyIiwic2NvcGUiOiJodHRwczovL21haWwuZ29vZ2xlLmNvbS8ifQ==");
        final ByteBuf nextClientReq = cmd.getNextCommandLineAfterContinuation(serverResponse);
        Assert.assertNotNull(nextClientReq, "expected command from client mismatched.");
        Assert.assertEquals(nextClientReq.toString(StandardCharsets.US_ASCII), "*\r\n", "expected command from client mismatched.");
        Assert.assertFalse(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method when SASL-IR is enabled and auth id is passed.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testGetCommandLineAuthIdPresent()
            throws IOException, IllegalArgumentException, IllegalAccessException, ImapAsyncClientException, ProtocolException {
        final String authId = "testla";
        final String username = "modelx";
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Collections.singletonList(ImapClientConstants.SASL_IR));
        final AuthPlainCommand cmd = new AuthPlainCommand(authId, username, token, new Capability(capas));
        Assert.assertTrue(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");

        // verify getCommandLine()
        Assert.assertEquals(cmd.getCommandLine(), "AUTHENTICATE PLAIN dGVzdGxhAG1vZGVseABzZWxmZHJpdmluZw==\r\n", "Expected result mismatched.");
        Assert.assertTrue(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");
        Assert.assertEquals(cmd.getDebugData(), "AUTHENTICATE PLAIN FOR USER:modelx", "Log line mismatched.");

        // asks the next command after continuation
        final IMAPResponse serverResponse = new IMAPResponse(
                "+ eyJzdGF0dXMiOiI0MDAiLCJzY2hlbWVzIjoiQmVhcmVyIiwic2NvcGUiOiJodHRwczovL21haWwuZ29vZ2xlLmNvbS8ifQ==");
        final ByteBuf nextClientReq = cmd.getNextCommandLineAfterContinuation(serverResponse);
        Assert.assertNotNull(nextClientReq, "expected command from client mismatched.");
        Assert.assertEquals(nextClientReq.toString(StandardCharsets.US_ASCII), "*\r\n", "expected command from client mismatched.");
        Assert.assertFalse(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method when SASL-IR is not enabled, we send next command after server challenge.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineAndContinuation() throws IllegalArgumentException, IllegalAccessException, ImapAsyncClientException {
        final String username = "tesla";
        final String pwd = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        final AuthPlainCommand cmd = new AuthPlainCommand(username, pwd, new Capability(capas));
        Assert.assertTrue(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");

        // verify getCommandLine()
        Assert.assertEquals(cmd.getCommandLine(), "AUTHENTICATE PLAIN\r\n", "Expected result mismatched.");
        Assert.assertFalse(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");

        // verify getNextCommandLineAfterContinuation()
        final IMAPResponse serverResponse = null; // should not cause anything if it is null
        final ByteBuf base64 = cmd.getNextCommandLineAfterContinuation(serverResponse);
        Assert.assertNotNull(base64, "Expected result mismatched.");
        Assert.assertEquals(base64.toString(StandardCharsets.US_ASCII), "AHRlc2xhAHNlbGZkcml2aW5n\r\n", "Expected result mismatched.");
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
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        final ImapRequest cmd = new AuthPlainCommand("tesla", "selfdriving", new Capability(capas));
        Assert.assertNull(cmd.getStreamingResponsesQueue(), "Expected result mismatched.");
    }

    /**
     * Tests getTerminateCommandLine method.
     */
    @Test
    public void testGetTerminateCommandLine() {
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        final ImapRequest cmd = new AuthPlainCommand("tesla", "selfdriving", new Capability(capas));
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
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        final ImapRequest cmd = new AuthPlainCommand("tesla", "selfdriving", new Capability(capas));
        Assert.assertSame(cmd.getCommandType(), ImapRFCSupportedCommandType.AUTHENTICATE);
    }
}