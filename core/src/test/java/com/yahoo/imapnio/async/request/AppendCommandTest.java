package com.yahoo.imapnio.async.request;

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

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

import io.netty.buffer.ByteBuf;

/**
 * Unit test for {@link AppendCommand}.
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
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineAndContinuation() throws IllegalArgumentException, IllegalAccessException, ImapAsyncClientException {
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
        Assert.assertFalse(cmd.isCommandLineDataSensitive(), "Expected result mismatched.");
        Assert.assertNull(cmd.getDebugData(), "Expected result mismatched.");

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
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testLiteralPlus() throws IllegalArgumentException, IllegalAccessException, ImapAsyncClientException {
        final Date internalDate = new Date(1552413335000L);
        final Flags expectedFlags = new Flags();
        expectedFlags.add(Flags.Flag.FLAGGED);
        expectedFlags.add(Flags.Flag.SEEN);
        final byte[] expectedMsg = TEST_MSG_BYTE;
        final int len = expectedMsg.length;

        // verify getCommandLine
        final AppendCommand cmd = new AppendCommand("Inbox", expectedFlags, internalDate, expectedMsg, LiteralSupport.ENABLE_LITERAL_PLUS);
        final String expectedStart = "APPEND Inbox (\\Flagged \\Seen) \"12-Mar-2019 ";
        final int startLen = expectedStart.length();
        final String dataLenStr = " {300+}\r\n";
        // we only have ascii in the binary
        final String actualCmdLine = cmd.getCommandLineBytes().toString(StandardCharsets.UTF_8);
        Assert.assertNotNull(actualCmdLine, "Command line mismatched.");
        // do not compare timezone part since it depends on which slave machine it runs
        Assert.assertEquals(actualCmdLine.substring(0, startLen), expectedStart, "Expected result mismatched.");
        Assert.assertTrue(actualCmdLine.contains(dataLenStr), "Expected data len mismatched.");
        Assert.assertEquals(actualCmdLine.substring(actualCmdLine.length() - len - 2), TEST_MSG_STR + "\r\n", "data mismatched.");
        Assert.assertFalse(cmd.isCommandLineDataSensitive(), "Expected result mismatched.");
        Assert.assertNull(cmd.getDebugData(), "Expected result mismatched.");

        // verify getNextCommandLineAfterContinuation()
        final IMAPResponse serverResponse = null; // we dont care
        ImapAsyncClientException actual = null;
        try {
            cmd.getNextCommandLineAfterContinuation(serverResponse);
        } catch (final ImapAsyncClientException e) {
            actual = e;
        }
        Assert.assertNotNull(actual, "Should encounter exception");
        Assert.assertEquals(actual.getFailureType(), FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND, "Should fail with this type");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method.
     *
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testLiteralMinus() throws IllegalArgumentException, IllegalAccessException, ImapAsyncClientException {
        final Date internalDate = new Date(1552413335000L);
        final Flags expectedFlags = new Flags();
        expectedFlags.add(Flags.Flag.FLAGGED);
        expectedFlags.add(Flags.Flag.SEEN);
        final byte[] expectedMsg = TEST_MSG_BYTE;
        final int len = expectedMsg.length;

        // verify getCommandLine
        final AppendCommand cmd = new AppendCommand("Inbox", expectedFlags, internalDate, expectedMsg, LiteralSupport.ENABLE_LITERAL_MINUS);
        final String expectedStart = "APPEND Inbox (\\Flagged \\Seen) \"12-Mar-2019 ";
        final int startLen = expectedStart.length();
        final String dataLenStr = " {300-}\r\n";
        // we only have ascii in the binary
        final String actualCmdLine = cmd.getCommandLineBytes().toString(StandardCharsets.UTF_8);
        Assert.assertNotNull(actualCmdLine, "Command line mismatched.");
        // do not compare timezone part since it depends on which slave machine it runs
        Assert.assertEquals(actualCmdLine.substring(0, startLen), expectedStart, "Expected result mismatched.");
        Assert.assertTrue(actualCmdLine.contains(dataLenStr), "Expected data len mismatched.");
        Assert.assertEquals(actualCmdLine.substring(actualCmdLine.length() - len - 2), TEST_MSG_STR + "\r\n", "data mismatched.");
        Assert.assertFalse(cmd.isCommandLineDataSensitive(), "Expected result mismatched.");
        Assert.assertNull(cmd.getDebugData(), "Expected result mismatched.");

        // verify getNextCommandLineAfterContinuation()
        final IMAPResponse serverResponse = null; // we dont care
        ImapAsyncClientException actual = null;
        try {
            cmd.getNextCommandLineAfterContinuation(serverResponse);
        } catch (final ImapAsyncClientException e) {
            actual = e;
        }
        Assert.assertNotNull(actual, "Should encounter exception");
        Assert.assertEquals(actual.getFailureType(), FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND, "Should fail with this type");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method.
     *
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineNullFlagsNullDate() throws IllegalArgumentException, ImapAsyncClientException {
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
     */
    @Test
    public void testGetTerminateCommandLine() {
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
        Assert.assertEquals(ex.getFailureType(), ImapAsyncClientException.FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND,
                "Expected result mismatched.");
    }

    /**
     * Tests getCommandType method.
     */
    @Test
    public void testGetCommandType() {
        final Date internalDate = new Date(1552413335000L);
        final Flags expectedFlags = new Flags();
        expectedFlags.add(Flags.Flag.FLAGGED);
        expectedFlags.add(Flags.Flag.SEEN);
        final byte[] expectedMsg = TEST_MSG_BYTE;

        final ImapRequest cmd = new AppendCommand("Inbox", expectedFlags, internalDate, expectedMsg);
        Assert.assertSame(cmd.getCommandType(), ImapRFCSupportedCommandType.APPEND_MESSAGE);
    }

    /**
     * Tests LiteralSupport enum.
     */
    @Test
    public void testLiteralSupportEnum() {
        final LiteralSupport[] enumList = LiteralSupport.values();
        Assert.assertEquals(enumList.length, 3, "The enum count mismatched.");
        final LiteralSupport plus = LiteralSupport.valueOf("ENABLE_LITERAL_PLUS");
        Assert.assertSame(plus, LiteralSupport.ENABLE_LITERAL_PLUS, "Enum does not match.");
    }
}