package com.yahoo.imapnio.async.request;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.mail.imap.protocol.UIDSet;
import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * Unit test for {@link UidExpungeCommand}.
 */
public class UidExpungeCommandTest {

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = UidExpungeCommand.class;
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
    public void testGetCommandLineWithUIDSet()
            throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        {
            final UIDSet[] uidsets = { new UIDSet(43, 4294967295L) };
            final ImapRequest cmd = new UidExpungeCommand(uidsets);
            Assert.assertEquals(cmd.getCommandLine(), "UID EXPUNGE 43:4294967295\r\n", "Expected result mismatched.");

            cmd.cleanup();
            // Verify if cleanup happened correctly.
            for (final Field field : fieldsToCheck) {
                Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
            }
        }
        {
            long[] uids = { 4294967292L, 4294967291L, 4294967297L };
            final UIDSet[] uidsets = UIDSet.createUIDSets(uids);
            final ImapRequest cmd = new UidExpungeCommand(uidsets);
            Assert.assertEquals(cmd.getCommandLine(), "UID EXPUNGE 4294967292,4294967291,4294967297\r\n", "Expected result mismatched.");

            cmd.cleanup();
            // Verify if cleanup happened correctly.
            for (final Field field : fieldsToCheck) {
                Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
            }
        }
    }

    /**
     * Tests getCommandLine method with MessageNumberSet constructor.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineWithMessageNumberSet()
            throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        {
            final MessageNumberSet[] sets = { new MessageNumberSet(43, 4294967295L) };
            final ImapRequest cmd = new UidExpungeCommand(sets);
            Assert.assertEquals(cmd.getCommandLine(), "UID EXPUNGE 43:4294967295\r\n", "Expected result mismatched.");

            cmd.cleanup();
            // Verify if cleanup happened correctly.
            for (final Field field : fieldsToCheck) {
                Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
            }
        }
        {
            long[] uids = { 4294967292L, 4294967293L, 4294967294L };
            final MessageNumberSet[] sets = MessageNumberSet.createMessageNumberSets(uids);
            final ImapRequest cmd = new UidExpungeCommand(sets);
            Assert.assertEquals(cmd.getCommandLine(), "UID EXPUNGE 4294967292:4294967294\r\n", "Expected result mismatched.");

            cmd.cleanup();
            // Verify if cleanup happened correctly.
            for (final Field field : fieldsToCheck) {
                Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
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
    public void testGetCommandLine() throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        final ImapRequest cmd = new UidExpungeCommand("43:44,99");
        Assert.assertEquals(cmd.getCommandLine(), "UID EXPUNGE 43:44,99\r\n", "Expected result mismatched.");

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
        final ImapRequest cmd = new UidExpungeCommand("43:44,99");
        Assert.assertSame(cmd.getCommandType(), ImapRFCSupportedCommandType.UID_EXPUNGE);
    }
}