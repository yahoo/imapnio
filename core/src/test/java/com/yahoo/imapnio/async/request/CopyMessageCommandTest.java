package com.yahoo.imapnio.async.request;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javax.mail.search.SearchException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.mail.imap.protocol.MessageSet;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * Unit test for {@code CopyMessageCommand}.
 */
public class CopyMessageCommandTest {
    /** Literal for COPY. */
    private static final String COPY = "COPY ";

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = CopyMessageCommand.class;
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
    public void testMessageSequenceGetCommandLine()
            throws IOException, ImapAsyncClientException, SearchException, IllegalArgumentException, IllegalAccessException {
        final String folderName = "folderABC";
        final int[] msgs = { 1, 2, 3 };
        final MessageSet[] msgsets = MessageSet.createMessageSets(msgs);
        final ImapRequest cmd = new CopyMessageCommand(msgsets, folderName);
        Assert.assertEquals(cmd.getCommandLine(), "COPY 1:3 folderABC\r\n", "Expected result mismatched.");

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
     * @throws SearchException will not throw
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testMessageUidGetCommandLine()
            throws IOException, ImapAsyncClientException, SearchException, IllegalArgumentException, IllegalAccessException {
        final String folderName = "folderABC";
        final ImapRequest cmd = new CopyMessageCommand(37850, 37852, folderName);
        Assert.assertEquals(cmd.getCommandLine(), "COPY 37850:37852 folderABC\r\n", "Expected result mismatched.");

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
     * @throws SearchException will not throw
     * @throws IOException will not throw
     */
    @Test
    public void testGetCommandLineWithEscapeChar() throws IOException, ImapAsyncClientException, SearchException {
        final String folderName = "folder ABC";
        final ImapRequest cmd = new CopyMessageCommand(37850, 37852, folderName);
        Assert.assertEquals(cmd.getCommandLine(), "COPY 37850:37852 \"folder ABC\"\r\n", "Expected result mismatched.");

    }

    /**
     * Tests getCommandLine method with folder name with other character set encoding.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     * @throws IOException will not throw
     */
    @Test
    public void testGetCommandLineWithOtherCharSet() throws IOException, ImapAsyncClientException, SearchException {
        final String folderName = "测试";
        final ImapRequest cmd = new CopyMessageCommand(37850, 37852, folderName);
        Assert.assertEquals(cmd.getCommandLine(), "COPY 37850:37852 &bUuL1Q-\r\n", "Expected result mismatched.");
    }

    /**
     * Tests getCommandType method.
     */
    @Test
    public void testGetCommandType() {
        final ImapRequest cmd = new CopyMessageCommand(37850, 37852, "targetFolder");
        Assert.assertSame(cmd.getCommandType(), ImapCommandType.COPY_MESSAGE);
    }
}