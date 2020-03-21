package com.yahoo.imapnio.async.request;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.mail.imap.protocol.MessageSet;
import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * Unit test for {@link MoveMessageCommand}.
 */
public class MoveMessageCommandTest {

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = MoveMessageCommand.class;
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
    public void testMessageSequenceGetCommandLine()
            throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        final String folderName = "folderABC";
        final int[] msgs = { 1, 2, 3 };
        final MessageSet[] msgsets = MessageSet.createMessageSets(msgs);
        final ImapRequest cmd = new MoveMessageCommand(msgsets, folderName);
        Assert.assertEquals(cmd.getCommandLine(), "MOVE 1:3 folderABC\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests constructor with start and end message sequence and getCommandLine method.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testConstructorStartEndGetCommandLine()
            throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        final String folderName = "folderABC";
        final ImapRequest cmd = new MoveMessageCommand(37850, 37852, folderName);
        Assert.assertEquals(cmd.getCommandLine(), "MOVE 37850:37852 folderABC\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests the constructor with @{MessageNumberSet} and getCommandLine method.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testConstructorMessageNumberSetGetCommandLine()
            throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        final String folderName = "folderABC";
        final MessageNumberSet[] mset = MessageNumberSet.createMessageNumberSets(new long[] { 37850L, 37851L, 37852L });
        final ImapRequest cmd = new MoveMessageCommand(mset, folderName);
        Assert.assertEquals(cmd.getCommandLine(), "MOVE 37850:37852 folderABC\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with folder name containing space.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineWithEscapeChar() throws ImapAsyncClientException {
        final String folderName = "folder ABC";
        final ImapRequest cmd = new MoveMessageCommand(37850, 37852, folderName);
        Assert.assertEquals(cmd.getCommandLine(), "MOVE 37850:37852 \"folder ABC\"\r\n", "Expected result mismatched.");

    }

    /**
     * Tests getCommandLine method with folder name with other character set encoding.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineWithOtherCharSet() throws ImapAsyncClientException {
        final String folderName = "测试";
        final ImapRequest cmd = new MoveMessageCommand(37850, 37852, folderName);
        Assert.assertEquals(cmd.getCommandLine(), "MOVE 37850:37852 &bUuL1Q-\r\n", "Expected result mismatched.");
    }

    /**
     * Tests getCommandType method.
     */
    @Test
    public void testGetCommandType() {
        final ImapRequest cmd = new MoveMessageCommand(37850, 37852, "targetFolder");
        Assert.assertSame(cmd.getCommandType(), ImapRFCSupportedCommandType.MOVE_MESSAGE);
    }
}