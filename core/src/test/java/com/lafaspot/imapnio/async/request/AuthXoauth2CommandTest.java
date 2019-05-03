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

import javax.mail.search.SearchException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.async.data.Capability;
import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;
import com.lafaspot.imapnio.async.request.AuthXoauth2Command;
import com.lafaspot.imapnio.async.request.ImapClientConstants;
import com.lafaspot.imapnio.async.request.ImapRequest;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * Unit test for {@code AuthXoauth2Command}.
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
     * @throws SearchException will not throw
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineSaslIREnabled()
            throws IOException, ImapAsyncClientException, SearchException, IllegalArgumentException, IllegalAccessException {
        final String username = "tesla";
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Arrays.asList(ImapClientConstants.SASL_IR));
        final ImapRequest cmd = new AuthXoauth2Command(username, token, new Capability(capas));

        Assert.assertEquals(cmd.getCommandLine(), "AUTHENTICATE XOAUTH2 dXNlcj10ZXNsYQFhdXRoPUJlYXJlciBzZWxmZHJpdmluZwEB\r\n",
                "Expected result mismatched.");
        Assert.assertEquals(cmd.getLogLine(), "AUTHENTICATE XOAUTH2 tesla", "Log line mismatched.");

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
        final String username = "tesla";
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        final AuthXoauth2Command cmd = new AuthXoauth2Command(username, token, new Capability(capas));
        Assert.assertEquals(cmd.getCommandLine(), "AUTHENTICATE XOAUTH2\r\n", "Expected result mismatched.");

        final IMAPResponse serverResponse = null; // null or not null does not matter
        final String resp2 = cmd.getNextCommandLineAfterContinuation(serverResponse);
        Assert.assertEquals(resp2, "dXNlcj10ZXNsYQFhdXRoPUJlYXJlciBzZWxmZHJpdmluZwEB\r\n", "Expected result mismatched.");

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
        capas.put(ImapClientConstants.SASL_IR, Arrays.asList(ImapClientConstants.SASL_IR));
        final ImapRequest cmd = new AuthXoauth2Command(username, token, new Capability(capas));
        Assert.assertNull(cmd.getStreamingResponsesQueue(), "Expected result mismatched.");
    }

    /**
     * Tests getNextCommandLineAfterContinuation method.
     */
    @Test
    public void testGetNextCommandLineAfterContinuation() {
        final String username = "tesla";
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Arrays.asList(ImapClientConstants.SASL_IR));
        final ImapRequest cmd = new AuthXoauth2Command(username, token, new Capability(capas));
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
        final String username = "tesla";
        final String token = "selfdriving";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.SASL_IR, Arrays.asList(ImapClientConstants.SASL_IR));
        final ImapRequest cmd = new AuthXoauth2Command(username, token, new Capability(capas));
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