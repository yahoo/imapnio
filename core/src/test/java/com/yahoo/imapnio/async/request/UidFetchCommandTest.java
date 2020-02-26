package com.yahoo.imapnio.async.request;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * Unit test for {@link UidFetchCommand}.
 */
public class UidFetchCommandTest {

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
        final Class<?> classUnderTest = UidFetchCommand.class;
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
     * Tests getCommandLine method using MessageNumberSet[] and data items.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineFromConstructorWithDataItems()
            throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {

        final long[] msgs = { 1L, 2L, 3L };
        final MessageNumberSet[] msgsets = MessageNumberSet.createMessageNumberSets(msgs);
        final ImapRequest cmd = new UidFetchCommand(msgsets, DATA_ITEMS);
        Assert.assertEquals(cmd.getCommandLine(), "UID FETCH 1:3 (FLAGS BODY[HEADER.FIELDS (DATE FROM)])\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
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
        final ImapRequest cmd = new UidFetchCommand(msgsets, FetchMacro.FAST);
        Assert.assertEquals(cmd.getCommandLine(), "UID FETCH 4294967293:4294967295 FAST\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method using UID string and data items.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineFromConstructorWithUidStringDataItems()
            throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {

        final ImapRequest cmd = new UidFetchCommand("*:4,5:7", DATA_ITEMS);
        Assert.assertEquals(cmd.getCommandLine(), "UID FETCH *:4,5:7 (FLAGS BODY[HEADER.FIELDS (DATE FROM)])\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method using UID string and macro.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineFromConstructorWithUidStringAndMacro()
            throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {

        final ImapRequest cmd = new UidFetchCommand("1:*", FetchMacro.FAST);
        Assert.assertEquals(cmd.getCommandLine(), "UID FETCH 1:* FAST\r\n", "Expected result mismatched.");

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
    public void testGetCommandLineFromConstructorWithChangedSince() throws ImapAsyncClientException, IllegalArgumentException,
            IllegalAccessException {

        final long[] msgs = { 1L, 2L, 3L };
        final MessageNumberSet[] msgsets = MessageNumberSet.createMessageNumberSets(msgs);
        final ImapRequest cmd = new UidFetchCommand(msgsets, DATA_ITEMS, 1L);
        Assert.assertEquals(cmd.getCommandLine(), "UID FETCH 1:3 (FLAGS BODY[HEADER.FIELDS (DATE FROM)]) (CHANGEDSINCE 1)\r\n",
                "getCommandLine() mismatched.");

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
    public void testGetCommandLineFromConstructorWithMacroChangedSince() throws ImapAsyncClientException, IllegalArgumentException,
            IllegalAccessException {

        final long[] msgs = { 4294967293L, 4294967294L, 4294967295L };
        final MessageNumberSet[] msgsets = MessageNumberSet.createMessageNumberSets(msgs);
        final ImapRequest cmd = new UidFetchCommand(msgsets, FetchMacro.FAST, 1L);
        Assert.assertEquals(cmd.getCommandLine(), "UID FETCH 4294967293:4294967295 FAST (CHANGEDSINCE 1)\r\n",
                "getCommandLine() mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method using MessageNumberSet[], data items, changed since the given modification sequence and vanished flag.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineFromConstructorWithChangedSinceVanished() throws ImapAsyncClientException, IllegalArgumentException,
            IllegalAccessException {

        final long[] msgs = { 1L, 2L, 3L };
        final MessageNumberSet[] msgsets = MessageNumberSet.createMessageNumberSets(msgs);
        final ImapRequest cmd = new UidFetchCommand(msgsets, DATA_ITEMS, 1L, true);
        Assert.assertEquals(cmd.getCommandLine(), "UID FETCH 1:3 (FLAGS BODY[HEADER.FIELDS (DATE FROM)]) (CHANGEDSINCE 1 VANISHED)\r\n",
                "getCommandLine() mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method using MessageNumberSet[], macro, changed since the given modification sequence and vanished flag.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineFromConstructorWithMacroChangedSinceVanished() throws ImapAsyncClientException, IllegalArgumentException,
            IllegalAccessException {

        final long[] msgs = { 4294967293L, 4294967294L, 4294967295L };
        final MessageNumberSet[] msgsets = MessageNumberSet.createMessageNumberSets(msgs);
        final ImapRequest cmd = new UidFetchCommand(msgsets, FetchMacro.FAST, 1L, true);
        Assert.assertEquals(cmd.getCommandLine(), "UID FETCH 4294967293:4294967295 FAST (CHANGEDSINCE 1 VANISHED)\r\n",
                "getCommandLine() mismatched.");

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
        final long[] msgs = { 1L, 2L, 3L };
        final MessageNumberSet[] msgsets = MessageNumberSet.createMessageNumberSets(msgs);
        final ImapRequest cmd = new UidFetchCommand(msgsets, DATA_ITEMS);
        Assert.assertSame(cmd.getCommandType(), ImapRFCSupportedCommandType.UID_FETCH);
    }

    /**
     * Tests FetchMacro enum.
     */
    @Test
    public void testFetchMacroEnum() {
        final FetchMacro[] enumList = FetchMacro.values();
        Assert.assertEquals(enumList.length, 3, "The enum count mismatched.");
        // values below cannot be changed
        final FetchMacro stateAll = FetchMacro.valueOf("ALL");
        Assert.assertSame(stateAll, FetchMacro.ALL, "Enum does not match.");
        // values below cannot be changed
        final FetchMacro stateFast = FetchMacro.valueOf("FAST");
        Assert.assertSame(stateFast, FetchMacro.FAST, "Enum does not match.");
        // values below cannot be changed
        final FetchMacro stateFull = FetchMacro.valueOf("FULL");
        Assert.assertSame(stateFull, FetchMacro.FULL, "Enum does not match.");
    }
}
