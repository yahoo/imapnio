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
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * Unit test for {@code SearchCommand}.
 */
public class SearchCommandTest {

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = SearchCommand.class;
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
     * Tests getCommandLine method using Message sequences and not null SearchTerm.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineWithNonNullSearchTerm()
            throws IOException, IllegalArgumentException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final int[] msgNos = { 1, 2, 3, 4 };
        final MessageSet[] msgSets = MessageSet.createMessageSets(msgNos);
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        final FlagTerm messageFlagTerms = new FlagTerm(flags, true);
        final ImapRequest cmd = new SearchCommand(msgSets, messageFlagTerms);
        Assert.assertEquals(cmd.getCommandLine(), "SEARCH 1:4 DELETED SEEN\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method using Message sequences and null SearchTerm.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineWithNullSearchTerm()
            throws IOException, IllegalArgumentException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final int[] msgNos = { 1, 2, 3, 4 };
        final MessageSet[] msgSets = MessageSet.createMessageSets(msgNos);
        final FlagTerm messageFlagTerms = null;
        final ImapRequest cmd = new SearchCommand(msgSets, messageFlagTerms);
        Assert.assertEquals(cmd.getCommandLine(), "SEARCH 1:4\r\n", "Expected result mismatched.");

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

        final SubjectTerm term = new SubjectTerm("ΩΩ"); // have none-ascii characters
        final ImapRequest cmd = new SearchCommand(msgSets, term);
        Assert.assertEquals(cmd.getCommandLine(), "SEARCH CHARSET UTF-8 7:10 SUBJECT {4+}\r\nￎﾩￎﾩ\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with String constructor, that is, passing string form message sequence, search string and character set name.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testStringConstructorNotNullCharset() throws IOException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final int[] msgNos = { 1, 2, 3, 4 };
        final MessageSet[] msgSets = MessageSet.createMessageSets(msgNos);
        final ImapRequest cmd = new SearchCommand(MessageSet.toString(msgSets), "DELETED SEEN UID 4294967292,4294967294:4294967295", "UTF-8");
        Assert.assertEquals(cmd.getCommandLine(), "SEARCH CHARSET UTF-8 1:4 DELETED SEEN UID 4294967292,4294967294:4294967295\r\n",
                "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with String constructor, that is, passing string form message sequence, search string and null character set name.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testStringConstructorNullCharset() throws IOException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final int[] msgNos = { 1, 2, 3, 4 };
        final MessageSet[] msgSets = MessageSet.createMessageSets(msgNos);
        final ImapRequest cmd = new SearchCommand(MessageSet.toString(msgSets), "DELETED SEEN", null);
        Assert.assertEquals(cmd.getCommandLine(), "SEARCH 1:4 DELETED SEEN\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with String constructor, that is, passing string form message sequence, null search string and null character set
     * name.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testStringConstructorNoSearchTerm() throws IOException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final ImapRequest cmd = new SearchCommand("1:*", null, null);
        Assert.assertEquals(cmd.getCommandLine(), "SEARCH 1:*\r\n", "Expected result mismatched.");

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
        final ImapRequest cmd = new SearchCommand("1:*", null, null);
        Assert.assertSame(cmd.getCommandType(), ImapCommandType.SEARCH);
    }
}