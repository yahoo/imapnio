package com.yahoo.imapnio.async.request;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.search.SearchException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.data.Capability;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;

/**
 * Unit test for {@code AuthOauthBearerCommand}.
 */
public class AuthOauthBearerCommandTest {

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = AuthOauthBearerCommand.class;
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
     * @throws SearchException will not throw
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineSaslIREnabled()
            throws IOException, ImapAsyncClientException, SearchException, IllegalArgumentException, IllegalAccessException {
        final String emailId = "user@example.com";
        final String hostname = "server.example.com";
        final int port = 993;
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Arrays.asList(ImapClientConstants.SASL_IR));
        final ImapRequest cmd = new AuthOauthBearerCommand(emailId, hostname, port, token, new Capability(capas));

        Assert.assertEquals(cmd.getCommandLine(),
                "AUTHENTICATE OAUTHBEARER bixhPXVzZXJAZXhhbXBsZS5jb20sAWhvc3Q9c2VydmVyLmV4YW1wbGUuY29tAXBvcnQ9OTkzAWF1dGg9QmVhcmVyIHNlbGZkcml2aW5nAQE=\r\n",
                "Expected result mismatched.");
        Assert.assertTrue(cmd.isCommandLineDataSensitive(), "isCommandLineDataSensitive() result mismatched.");
        Assert.assertEquals(cmd.getDebugData(), "AUTHENTICATE OAUTHBEARER FOR USER:user@example.com", "Log line mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineSaslIRDisabled() throws IOException, IllegalArgumentException, IllegalAccessException, ImapAsyncClientException {
        final String emailId = "user@example.com";
        final String hostname = "server.example.com";
        final int port = 993;
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        final AuthOauthBearerCommand cmd = new AuthOauthBearerCommand(emailId, hostname, port, token, new Capability(capas));
        Assert.assertEquals(cmd.getCommandLine(), "AUTHENTICATE OAUTHBEARER\r\n", "Expected result mismatched.");

        final IMAPResponse serverResponse = null; // null or not null does not matter
        final ByteBuf resp2 = cmd.getNextCommandLineAfterContinuation(serverResponse);
        Assert.assertEquals(resp2.toString(StandardCharsets.US_ASCII),
                "bixhPXVzZXJAZXhhbXBsZS5jb20sAWhvc3Q9c2VydmVyLmV4YW1wbGUuY29tAXBvcnQ9OTkzAWF1dGg9QmVhcmVyIHNlbGZkcml2aW5nAQE=\r\n",
                "Expected result mismatched.");

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
        final String emailId = "user@example.com";
        final String hostname = "server.example.com";
        final int port = 993;
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Arrays.asList(ImapClientConstants.SASL_IR));
        final ImapRequest cmd = new AuthOauthBearerCommand(emailId, hostname, port, token, new Capability(capas));
        Assert.assertNull(cmd.getStreamingResponsesQueue(), "Expected result mismatched.");
    }

    /**
     * Tests getNextCommandLineAfterContinuation method.
     */
    @Test
    public void testGetNextCommandLineAfterContinuation() {
        final String emailId = "user@example.com";
        final String hostname = "server.example.com";
        final int port = 993;
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Arrays.asList(ImapClientConstants.SASL_IR));
        final ImapRequest cmd = new AuthOauthBearerCommand(emailId, hostname, port, token, new Capability(capas));
        final IMAPResponse serverResponse = null; // null or not null does not matter
        ImapAsyncClientException ex = null;
        try {
            cmd.getNextCommandLineAfterContinuation(serverResponse);
        } catch (final ImapAsyncClientException imapAsyncEx) {
            ex = imapAsyncEx;
        }
        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        Assert.assertEquals(ex.getFaiureType(), ImapAsyncClientException.FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND,
                "Expected result mismatched.");
    }

    /**
     * Tests getTerminateCommandLine method.
     */
    @Test
    public void testGetTerminateCommandLine() {
        final String emailId = "user@example.com";
        final String hostname = "server.example.com";
        final int port = 993;
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Arrays.asList(ImapClientConstants.SASL_IR));
        final ImapRequest cmd = new AuthOauthBearerCommand(emailId, hostname, port, token, new Capability(capas));
        ImapAsyncClientException ex = null;
        try {
            cmd.getTerminateCommandLine();
        } catch (final ImapAsyncClientException imapAsyncEx) {
            ex = imapAsyncEx;
        }
        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        Assert.assertEquals(ex.getFaiureType(), ImapAsyncClientException.FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND,
                "Expected result mismatched.");
    }

    /**
     * Tests getCommandType method.
     */
    @Test
    public void testGetCommandType() {
        final String emailId = "user@example.com";
        final String hostname = "server.example.com";
        final int port = 993;
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Arrays.asList(ImapClientConstants.SASL_IR));
        final ImapRequest cmd = new AuthOauthBearerCommand(emailId, hostname, port, token, new Capability(capas));
        Assert.assertSame(cmd.getCommandType(), ImapCommandType.AUTHENTICATE);
    }
}