package com.lafaspot.imapnio.async.request;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.async.data.Capability;
import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;
import com.lafaspot.imapnio.async.request.AuthPlainCommand;
import com.lafaspot.imapnio.async.request.ImapClientConstants;
import com.lafaspot.imapnio.async.request.ImapRequest;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * Unit test for {@code AuthPlainCommand}.
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
     */
    @Test
    public void testGetCommandLineSASLIREnabled() throws IOException, IllegalArgumentException, IllegalAccessException, ImapAsyncClientException {
        final String username = "tesla";
        final String pwd = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Arrays.asList(ImapClientConstants.SASL_IR));
        final AuthPlainCommand cmd = new AuthPlainCommand(username, pwd, new Capability(capas));

        // verify getCommandLine()
        Assert.assertEquals(cmd.getCommandLine(), "AUTHENTICATE PLAIN AHRlc2xhAHNlbGZkcml2aW5n\r\n", "Expected result mismatched.");
        Assert.assertEquals(cmd.getLogLine(), "AUTHENTICATE PLAIN tesla", "Log line mismatched.");

        // verify getNextCommandLineAfterContinuation()
        final IMAPResponse serverResponse = null; // should not cause anything if it is null
        ImapAsyncClientException ex = null;
        try {
            cmd.getNextCommandLineAfterContinuation(serverResponse);
        } catch (final ImapAsyncClientException imapAsyncEx) {
            ex = imapAsyncEx;
        }
        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        Assert.assertEquals(ex.getFaiureType(), ImapAsyncClientException.FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND,
                "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method when SASL-IR is not enabled, we send next command after server chanllenage.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineAndContinuation() throws IOException, IllegalArgumentException, IllegalAccessException, ImapAsyncClientException {
        final String username = "tesla";
        final String pwd = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        final AuthPlainCommand cmd = new AuthPlainCommand(username, pwd, new Capability(capas));
        // verify getCommandLine()
        Assert.assertEquals(cmd.getCommandLine(), "AUTHENTICATE PLAIN\r\n", "Expected result mismatched.");

        // verify getNextCommandLineAfterContinuation()
        final IMAPResponse serverResponse = null; // should not cause anything if it is null
        final String base64 = cmd.getNextCommandLineAfterContinuation(serverResponse);
        Assert.assertNotNull(base64, "Expected result mismatched.");
        Assert.assertEquals(base64, "AHRlc2xhAHNlbGZkcml2aW5n\r\n", "Expected result mismatched.");

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
        Assert.assertEquals(ex.getFaiureType(), ImapAsyncClientException.FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND,
                "Expected result mismatched.");
    }
}