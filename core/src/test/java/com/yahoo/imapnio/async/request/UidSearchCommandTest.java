package com.yahoo.imapnio.async.request;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javax.mail.Flags;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchException;
import javax.mail.search.SubjectTerm;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.mail.imap.protocol.MessageSet;
import com.sun.mail.imap.protocol.UIDSet;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * Unit test for {@code UidSearchCommand}.
 */
public class UidSearchCommandTest {

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = UidSearchCommand.class;
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
    public void testGetCommandLineWithUIDSequence()
            throws IOException, IllegalArgumentException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final int[] msgNos = { 1, 2, 3, 4 };
        final MessageSet[] msgSets = MessageSet.createMessageSets(msgNos);
        final long[] uids = { 4294967292L, 4294967294L, 4294967295L };
        final UIDSet[] uidsets = UIDSet.createUIDSets(uids);
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        final FlagTerm messageFlagTerms = new FlagTerm(flags, true);
        final ImapRequest cmd = new UidSearchCommand(msgSets, uidsets, messageFlagTerms);
        Assert.assertEquals(cmd.getCommandLine(), "UID SEARCH 1:4 DELETED SEEN UID 4294967292,4294967294:4294967295\r\n",
                "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method using none ascii search strings.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineWithNoneAscii()
            throws IOException, IllegalArgumentException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final int[] msgNos = { 7, 8, 9, 10 };
        final MessageSet[] msgSets = MessageSet.createMessageSets(msgNos);
        final long[] uids = { 1L, 2L, 3L };
        final UIDSet[] uidsets = UIDSet.createUIDSets(uids);

        final SubjectTerm term = new SubjectTerm("ΩΩ"); // have none-ascii characters
        final ImapRequest cmd = new UidSearchCommand(msgSets, uidsets, term);
        Assert.assertEquals(cmd.getCommandLine(), "UID SEARCH CHARSET UTF-8 7:10 SUBJECT {4+}\r\nￎﾩￎﾩ UID 1:3\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with UID and flags search terms.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineWithUIDAndSearchTerm() throws IOException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final int[] msgNos = { 1, 2, 3, 4 };
        final MessageSet[] msgSets = MessageSet.createMessageSets(msgNos);
        final long[] uids = { 4294967292L, 4294967294L, 4294967295L };
        final UIDSet[] uidsets = UIDSet.createUIDSets(uids);
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        final FlagTerm messageFlagTerms = new FlagTerm(flags, true);
        final ImapRequest cmd = new UidSearchCommand(MessageSet.toString(msgSets), UIDSet.toString(uidsets), messageFlagTerms);
        Assert.assertEquals(cmd.getCommandLine(), "UID SEARCH 1:4 DELETED SEEN UID 4294967292,4294967294:4294967295\r\n",
                "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with flags search term but no UIDs.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineWithSearchTermNoUid() throws IOException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final int[] msgNos = { 1, 2, 3, 4 };
        final MessageSet[] msgSets = MessageSet.createMessageSets(msgNos);
        final UIDSet[] uidsets = null;
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        final FlagTerm messageFlagTerms = new FlagTerm(flags, true);
        final ImapRequest cmd = new UidSearchCommand(MessageSet.toString(msgSets), UIDSet.toString(uidsets), messageFlagTerms);
        Assert.assertEquals(cmd.getCommandLine(), "UID SEARCH 1:4 DELETED SEEN\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method, no UID, but it has search terms.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineWithUidNoSearchTerm() throws IOException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final int[] msgNos = { 1, 2, 3, 4 };
        final MessageSet[] msgSets = MessageSet.createMessageSets(msgNos);
        final String uidString = "4294967292,4294967294";
        final FlagTerm messageFlagTerms = null;
        final ImapRequest cmd = new UidSearchCommand(MessageSet.toString(msgSets), uidString, messageFlagTerms);
        Assert.assertEquals(cmd.getCommandLine(), "UID SEARCH 1:4 UID 4294967292,4294967294\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method, no UID, no search terms.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineWithNoUIDNoSearchTerm() throws IOException, IllegalAccessException, SearchException, ImapAsyncClientException {

        final UIDSet[] uidsets = null;
        final ImapRequest cmd = new UidSearchCommand("1:*", UIDSet.toString(uidsets), null);
        Assert.assertEquals(cmd.getCommandLine(), "UID SEARCH 1:*\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }
}