package com.yahoo.imapnio.async.request;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * Unit test for {@link ListStatusCommand}.
 */
public class ListStatusCommandTest {

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = ListStatusCommand.class;
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
     * Tests getCommandLine method with only one pattern.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineOnePattern1() throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        final String[] someItems = { "UIDVALIDITY", "UIDNEXT", "MESSAGES", "HIGHESTMODSEQ", "UNSEEN", "RECENT", "MAILBOXID" };
        final String[] patterns = { "*" };
        final ImapRequest cmd = new ListStatusCommand("", patterns, someItems);
        Assert.assertEquals(cmd.getCommandLine(),
                "LIST \"\" (\"*\") RETURN (STATUS (UIDVALIDITY UIDNEXT MESSAGES HIGHESTMODSEQ UNSEEN RECENT MAILBOXID))\r\n",
                "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with mailbox name containing character, "%26", that need to escape and trailing space to be preserved.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineOnePatternEscapeChars() throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        final String[] someItems = { "UIDVALIDITY", "UIDNEXT" };
        final String[] patterns = { "&iber " };
        final ImapRequest cmd = new ListStatusCommand("", patterns, someItems);
        Assert.assertEquals(cmd.getCommandLine(), "LIST \"\" (\"&-iber \") RETURN (STATUS (UIDVALIDITY UIDNEXT))\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with multiple patterns, some have mailbox name, some have pattern.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineMultiPatterns() throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        final String[] someItems = { "UIDVALIDITY", "UIDNEXT", "MESSAGES", "HIGHESTMODSEQ", "UNSEEN", "RECENT" };
        final String[] patterns = { "INBOX", "Drafts", "Sent/%" };
        final ImapRequest cmd = new ListStatusCommand("", patterns, someItems);
        Assert.assertEquals(cmd.getCommandLine(),
                "LIST \"\" (\"INBOX\" \"Drafts\" \"Sent/%\") RETURN (STATUS (UIDVALIDITY UIDNEXT MESSAGES HIGHESTMODSEQ UNSEEN RECENT))\r\n",
                "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with multiple patterns, some have mailbox name that have escape chars, some have pattern.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineMultiPatternsEscapeChars() throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        final String[] someItems = { "UIDVALIDITY", "UIDNEXT", "MESSAGES", "HIGHESTMODSEQ", "UNSEEN", "RECENT" };
        final String[] patterns = { "*", "&iber ", "Drafts", "Sent/%" };
        final ImapRequest cmd = new ListStatusCommand("", patterns, someItems);
        Assert.assertEquals(cmd.getCommandLine(),
                "LIST \"\" (\"*\" \"&-iber \" \"Drafts\" \"Sent/%\") RETURN (STATUS (UIDVALIDITY UIDNEXT MESSAGES HIGHESTMODSEQ UNSEEN RECENT))\r\n",
                "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with multiple patterns, this one has none-ascii characters, which should be resolved by base64, mailbox encoding.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     */
    @Test
    public void testGetCommandLineMultiPatternsNoneAsciiChars() throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        final String[] someItems = { "UIDVALIDITY", "UIDNEXT", "MESSAGES", "HIGHESTMODSEQ", "UNSEEN", "RECENT" };
        final String[] patterns = { "ΩΩ", "Sent/%" };
        final ImapRequest cmd = new ListStatusCommand("", patterns, someItems);
        Assert.assertEquals(cmd.getCommandLine(),
                "LIST \"\" (\"&A6kDqQ-\" \"Sent/%\") RETURN (STATUS (UIDVALIDITY UIDNEXT MESSAGES HIGHESTMODSEQ UNSEEN RECENT))\r\n",
                "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests encountering invalid input error in constructor since status items array has 0 length.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testInvalidInputInConstrutorItemsLength0() throws ImapAsyncClientException {
        final String[] length0 = new String[0];

        ImapAsyncClientException actualEx = null;
        try {
            final String[] patterns = { "&iber " };
            new ListStatusCommand("", patterns, length0);
        } catch (final ImapAsyncClientException imapAsyncEx) {
            actualEx = imapAsyncEx;
        }
        Assert.assertNotNull(actualEx, "Expect exception to be thrown.");
        Assert.assertEquals(actualEx.getFailureType(), ImapAsyncClientException.FailureType.INVALID_INPUT, "Expected result mismatched.");
    }

    /**
     * Tests encountering invalid input error in constructor since multiPatterns array has 0 length.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testInvalidInputInConstrutorMultiPatternsLength0() throws ImapAsyncClientException {
        final String[] someItems = { "MESSAGES", "HIGHESTMODSEQ", "UNSEEN", "RECENT", "MAILBOXID" };
        final String[] length0 = new String[0];

        ImapAsyncClientException actualEx = null;
        try {
            new ListStatusCommand("", length0, someItems);
        } catch (final ImapAsyncClientException imapAsyncEx) {
            actualEx = imapAsyncEx;
        }
        Assert.assertNotNull(actualEx, "Expect exception to be thrown.");
        Assert.assertEquals(actualEx.getFailureType(), ImapAsyncClientException.FailureType.INVALID_INPUT, "Expected result mismatched.");
    }

    /**
     * Tests getStreamingResponsesQueue method.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetStreamingResponsesQueue() throws ImapAsyncClientException {
        final String[] someItems = { "MESSAGES", "HIGHESTMODSEQ", "UNSEEN", "RECENT", "MAILBOXID" };
        final String[] patterns = { "abc*" };
        final ImapRequest cmd = new ListStatusCommand("", patterns, someItems);
        Assert.assertNull(cmd.getStreamingResponsesQueue(), "Expected result mismatched.");
    }

    /**
     * Tests getNextCommandLineAfterContinuation method.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetNextCommandLineAfterContinuation() throws ImapAsyncClientException {
        final String[] someItems = { "RECENT", "MAILBOXID" };
        final String[] patterns = { "abc*" };
        final ImapRequest cmd = new ListStatusCommand("", patterns, someItems);
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
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetTerminateCommandLine() throws ImapAsyncClientException {
        final String[] someItems = { "HIGHESTMODSEQ", "UNSEEN", "RECENT", "MAILBOXID" };
        final String[] patterns = { "abc*" };
        final ImapRequest cmd = new ListStatusCommand("", patterns, someItems);
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
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandType() throws ImapAsyncClientException {
        final String[] someItems = { "HIGHESTMODSEQ", "UNSEEN", "RECENT", "MAILBOXID" };
        final String[] patterns = { "abc*" };
        final ImapRequest cmd = new ListStatusCommand("", patterns, someItems);
        Assert.assertSame(cmd.getCommandType(), ImapRFCSupportedCommandType.LIST_STATUS);
    }
}
