package com.yahoo.imapnio.async.request;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * Unit test for {@link FetchCommand}.
 */
public class FetchCommandTest {

    /** Data items to fetch. */
    private static final String DATA_ITEMS = "FLAGS BODY[HEADER.FIELDS (DATE FROM)]";

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = FetchCommand.class;
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
     * Tests getCommandLine method using MessageNumberSet[] and macro.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineFromConstructorWithMacro()
            throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {

        final long[] msgs = { 4294967293L, 4294967294L, 4294967295L };
        final MessageNumberSet[] msgsets = MessageNumberSet.createMessageNumberSets(msgs);
        final ImapRequest cmd = new FetchCommand(msgsets, FetchMacro.FAST);
        Assert.assertEquals(cmd.getCommandLine(), "FETCH 4294967293:4294967295 FAST\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method using MessageNumberSet[] and data items.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineFromConstructorWithDataItems()
            throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {

        final int[] msgs = { 1, 2, 3 };
        final MessageNumberSet[] msgsets = MessageNumberSet.createMessageNumberSets(msgs);
        final ImapRequest cmd = new FetchCommand(msgsets, DATA_ITEMS);
        Assert.assertEquals(cmd.getCommandLine(), "FETCH 1:3 (FLAGS BODY[HEADER.FIELDS (DATE FROM)])\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
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
    public void testGetCommandLineWithStartEndConstructor()
            throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {

        final ImapRequest cmd = new FetchCommand(new MessageNumberSet[] { new MessageNumberSet(1, 10000) }, DATA_ITEMS);
        Assert.assertEquals(cmd.getCommandLine(), "FETCH 1:10000 (FLAGS BODY[HEADER.FIELDS (DATE FROM)])\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method using MessageNumberSet[], macro, and changed since the given modification sequence.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineFromConstructorWithChangedSince() throws ImapAsyncClientException, IllegalArgumentException,
            IllegalAccessException {

        final long[] msgs = { 4294967293L, 4294967294L, 4294967295L };
        final MessageNumberSet[] msgsets = MessageNumberSet.createMessageNumberSets(msgs);
        final ImapRequest cmd = new FetchCommand(msgsets, FetchMacro.FAST, 1L);
        Assert.assertEquals(cmd.getCommandLine(), "FETCH 4294967293:4294967295 FAST (CHANGEDSINCE 1)\r\n",
                "getCommandLine() mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method using MessageNumberSet[], data items, and changed since the given modification sequence.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineFromConstructorWithDataItemsChangedSince() throws ImapAsyncClientException, IllegalArgumentException,
            IllegalAccessException {

        final int[] msgs = { 1, 2, 3 };
        final MessageNumberSet[] msgsets = MessageNumberSet.createMessageNumberSets(msgs);
        final ImapRequest cmd = new FetchCommand(msgsets, DATA_ITEMS, 1L);
        Assert.assertEquals(cmd.getCommandLine(), "FETCH 1:3 (FLAGS BODY[HEADER.FIELDS (DATE FROM)]) (CHANGEDSINCE 1)\r\n",
                "getCommandLine() mismatched.");

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

        final ImapRequest cmd = new FetchCommand(new MessageNumberSet[] { new MessageNumberSet(1, 10000) }, DATA_ITEMS);
        Assert.assertNull(cmd.getStreamingResponsesQueue(), "Expected result mismatched.");
    }

    /**
     * Tests getNextCommandLineAfterContinuation method.
     */
    @Test
    public void testGetNextCommandLineAfterContinuation() {

        final ImapRequest cmd = new FetchCommand(new MessageNumberSet[] { new MessageNumberSet(1, 10000) }, DATA_ITEMS);
        final IMAPResponse serverResponse = null; // null or not null does not matter
        ImapAsyncClientException ex = null;
        try {
            cmd.getNextCommandLineAfterContinuation(serverResponse);
        } catch (final ImapAsyncClientException imapAsyncEx) {
            ex = imapAsyncEx;
        }
        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        Assert.assertEquals(ex.getFailureType(), ImapAsyncClientException.FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND,
                "Expected result mismatched.");
    }

    /**
     * Tests getTerminateCommandLine method.
     */
    @Test
    public void testGetTerminateCommandLine() {

        final ImapRequest cmd = new FetchCommand(new MessageNumberSet[] { new MessageNumberSet(1, 10000) }, DATA_ITEMS);
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
        final ImapRequest cmd = new FetchCommand(new MessageNumberSet[] { new MessageNumberSet(1, 10000) }, DATA_ITEMS);
        Assert.assertSame(cmd.getCommandType(), ImapRFCSupportedCommandType.FETCH);
    }
}