package com.lafaspot.imapnio.async.request;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javax.mail.Flags;
import javax.mail.search.SearchException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;
import com.lafaspot.imapnio.async.request.ImapRequest;
import com.lafaspot.imapnio.async.request.StoreFlagsCommand;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.MessageSet;

/**
 * Unit test for {@code StoreFlagsCommand}.
 */
public class StoreFlagsCommandTest {

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = StoreFlagsCommand.class;
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
     * Tests getCommandLine method using Message sequences.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineWithMessageSequence()
            throws IOException, IllegalArgumentException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final boolean isUid = false;
        final int[] msgs = { 1, 2, 3 };
        final MessageSet[] msgsets = MessageSet.createMessageSets(msgs);
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        final boolean isSet = true;
        final ImapRequest cmd = new StoreFlagsCommand(isUid, msgsets, flags, isSet);
        Assert.assertEquals(cmd.getCommandLine(), "STORE 1:3 +FLAGS (\\Deleted \\Seen)\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method using UID.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineWithUID() throws IOException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final boolean isUid = true;
        final int[] msgs = { 32321, 32322, 32323 };
        final MessageSet[] msgsets = MessageSet.createMessageSets(msgs);
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        final boolean isSet = true;
        final ImapRequest cmd = new StoreFlagsCommand(isUid, msgsets, flags, isSet);
        Assert.assertEquals(cmd.getCommandLine(), "UID STORE 32321:32323 +FLAGS (\\Deleted \\Seen)\r\n", "Expected result mismatched.");

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
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineWithStartEndConstructorAndRemoveFlags()
            throws IOException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final boolean isUid = false;
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        final boolean isSet = false;
        final ImapRequest cmd = new StoreFlagsCommand(isUid, 1, 10000, flags, isSet);
        Assert.assertEquals(cmd.getCommandLine(), "STORE 1:10000 -FLAGS (\\Deleted \\Seen)\r\n",
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
        final boolean isUid = false;
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        final boolean isSet = true;
        final ImapRequest cmd = new StoreFlagsCommand(isUid, 1, 10000, flags, isSet);
        Assert.assertNull(cmd.getStreamingResponsesQueue(), "Expected result mismatched.");
    }

    /**
     * Tests getNextCommandLineAfterContinuation method.
     */
    @Test
    public void testGetNextCommandLineAfterContinuation() {
        final boolean isUid = false;
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        final boolean isSet = true;
        final ImapRequest cmd = new StoreFlagsCommand(isUid, 1, 10000, flags, isSet);
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
        final boolean isUid = false;
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        final boolean isSet = true;
        final ImapRequest cmd = new StoreFlagsCommand(isUid, 1, 10000, flags, isSet);
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