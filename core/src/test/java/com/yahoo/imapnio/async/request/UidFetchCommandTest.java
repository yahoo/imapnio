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

import com.sun.mail.imap.protocol.UIDSet;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * Unit test for {@code UidFetchCommand}.
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
     * Tests getCommandLine method using Message sequences.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineWithMessageSequence()
            throws IOException, ImapAsyncClientException, SearchException, IllegalArgumentException, IllegalAccessException {

        final long[] msgs = { 1L, 2L, 3L };
        final UIDSet[] msgsets = UIDSet.createUIDSets(msgs);
        final ImapRequest cmd = new UidFetchCommand(msgsets, DATA_ITEMS);
        Assert.assertEquals(cmd.getCommandLine(), "UID FETCH 1:3 (FLAGS BODY[HEADER.FIELDS (DATE FROM)])\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method using UID.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineWithUID()
            throws IOException, ImapAsyncClientException, SearchException, IllegalArgumentException, IllegalAccessException {

        final long[] msgs = { 4294967293L, 4294967294L, 4294967295L };
        final UIDSet[] msgsets = UIDSet.createUIDSets(msgs);
        final ImapRequest cmd = new UidFetchCommand(msgsets, DATA_ITEMS);
        Assert.assertEquals(cmd.getCommandLine(), "UID FETCH 4294967293:4294967295 (FLAGS BODY[HEADER.FIELDS (DATE FROM)])\r\n",
                "Expected result mismatched.");

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
        final UIDSet[] msgsets = UIDSet.createUIDSets(msgs);
        final ImapRequest cmd = new UidFetchCommand(msgsets, DATA_ITEMS);
        Assert.assertSame(cmd.getCommandType(), ImapCommandType.UID_FETCH);
    }
}