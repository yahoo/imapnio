package com.lafaspot.imapnio.async.request;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.mail.Flags;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;
import com.lafaspot.imapnio.async.request.AppendCommand;
import com.lafaspot.imapnio.async.request.ImapRequest;
import com.sun.mail.imap.protocol.IMAPResponse;

import io.netty.buffer.ByteBuf;

/**
 * Unit test for {@code AppendCommand}.
 */
public class AppendCommandTest {

    /** Testing message in string format. */
    @Nonnull
    private static final String TEST_MSG_STR = "Date: Mon, 28 Jan 2019 21:52:25 -0800 (PST)\nFrom: Fred Foobar <foobar@Blurdybloop.COM>\nTo: "
            + "poptestli@yahoo.com\nSubject: afternoon meeting\nMessage-Id: <B27397-0100000@Blurdybloop.COM>\nMIME-Version: 1.0\nContent-Type:"
            + " TEXT/PLAIN; CHARSET=US-ASCII\n\nHello Joe, do you think we can meet at 3:30 tomorrow?\n\n";

    /** Testing message in byte array format. */
    @Nonnull
    private static final byte[] TEST_MSG_BYTE = TEST_MSG_STR.getBytes(StandardCharsets.UTF_8);

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = AppendCommand.class;
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
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineAndContinuation() throws IOException, IllegalArgumentException, IllegalAccessException, ImapAsyncClientException {
        final Date internalDate = new Date(1552413335000L);
        final Flags expectedFlags = new Flags();
        expectedFlags.add(Flags.Flag.FLAGGED);
        expectedFlags.add(Flags.Flag.SEEN);
        final byte[] expectedMsg = TEST_MSG_BYTE;
        final int len = expectedMsg.length;

        // verify getCommandLine
        final AppendCommand cmd = new AppendCommand("Inbox", expectedFlags, internalDate, expectedMsg);
        final String expectedStart = "APPEND Inbox (\\Flagged \\Seen) \"12-Mar-2019 ";
        final int startLen = expectedStart.length();
        final String expectedEnd = " {300}\r\n";
        final int endLen = expectedEnd.length();
        final String actualCmdLine = cmd.getCommandLine();
        Assert.assertNotNull(actualCmdLine, "Command line mismatched.");
        // do not compare timezone part since it depends on which slave machine it runs
        Assert.assertEquals(actualCmdLine.substring(0, startLen), expectedStart, "Expected result mismatched.");
        Assert.assertEquals(actualCmdLine.substring(actualCmdLine.length() - endLen), expectedEnd, "Expected result mismatched.");

        // verify getNextCommandLineAfterContinuation()
        final IMAPResponse serverResponse = null; // we dont care
        final ByteBuf bytebuf = cmd.getNextCommandLineAfterContinuation(serverResponse);
        Assert.assertNotNull(bytebuf, "Expected result mismatched.");

        final byte[] actual = new byte[len];
        bytebuf.getBytes(0, actual, 0, len);
        Assert.assertEquals(actual, expectedMsg, "Expected result mismatched.");

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
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineNullFlagsNullDate() throws IOException, IllegalArgumentException, IllegalAccessException, ImapAsyncClientException {
        final Date internalDate = null;
        final Flags expectedFlags = null;
        final byte[] expectedMsg = TEST_MSG_BYTE;

        // verify getCommandLine
        final AppendCommand cmd = new AppendCommand("Inbox", expectedFlags, internalDate, expectedMsg);
        Assert.assertEquals(cmd.getCommandLine(), "APPEND Inbox {300}\r\n", "Expected result mismatched.");
    }

    /**
     * Tests getStreamingResponsesQueue method.
     */
    @Test
    public void testGetStreamingResponsesQueue() {
        final Date internalDate = new Date(1552413335000L);
        final Flags expectedFlags = new Flags();
        expectedFlags.add(Flags.Flag.FLAGGED);
        expectedFlags.add(Flags.Flag.SEEN);
        final byte[] expectedMsg = TEST_MSG_BYTE;

        final ImapRequest cmd = new AppendCommand("Inbox", expectedFlags, internalDate, expectedMsg);
        Assert.assertNull(cmd.getStreamingResponsesQueue(), "Expected result mismatched.");
    }

    /**
     * Tests getTerminateCommandLine method.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetTerminateCommandLine() throws ImapAsyncClientException {
        final Date internalDate = new Date(1552413335000L);
        final Flags expectedFlags = new Flags();
        expectedFlags.add(Flags.Flag.FLAGGED);
        expectedFlags.add(Flags.Flag.SEEN);
        final byte[] expectedMsg = TEST_MSG_BYTE;

        final ImapRequest cmd = new AppendCommand("Inbox", expectedFlags, internalDate, expectedMsg);
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