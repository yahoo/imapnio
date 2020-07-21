package com.yahoo.imapnio.async.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.Flags;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchException;
import javax.mail.search.SubjectTerm;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.mail.iap.Argument;
import com.sun.mail.iap.Literal;
import com.yahoo.imapnio.async.data.Capability;
import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.data.MessageNumberSet.LastMessage;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

import io.netty.buffer.ByteBuf;

/**
 * Unit test for {@link UidSearchCommand}.
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
     * Tests getCommandLine method with none-null message sequences set, none-null SearchTerm.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineNoneNullMessageSeqSetsNonNullSearchTerm()
            throws IOException, IllegalArgumentException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final int[] msgNos = { 1, 2, 3 };
        final MessageNumberSet[] msgsets = MessageNumberSet.createMessageNumberSets(msgNos);
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        final FlagTerm messageFlagTerms = new FlagTerm(flags, true);
        final Capability capa = null;
        final ImapRequest cmd = new UidSearchCommand(msgsets, messageFlagTerms, capa);
        Assert.assertEquals(cmd.getCommandLine(), "UID SEARCH 1:3 DELETED SEEN\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with none-null message sequences set, none-null SearchTerm.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineNoneNullMessageSeqStringNonNullSearchTerm()
            throws IOException, IllegalArgumentException, IllegalAccessException, SearchException, ImapAsyncClientException {

        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        final FlagTerm messageFlagTerms = new FlagTerm(flags, true);
        final Capability capa = null;
        final ImapRequest cmd = new UidSearchCommand("1:*", messageFlagTerms, capa);
        Assert.assertEquals(cmd.getCommandLine(), "UID SEARCH 1:* DELETED SEEN\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with none-null message sequences set, none-null SearchTerm.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineNullMessageSeqSetsNonNullSearchTerm()
            throws IOException, IllegalArgumentException, IllegalAccessException, SearchException, ImapAsyncClientException {

        final MessageNumberSet[] msgsets = null;
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        final FlagTerm messageFlagTerms = new FlagTerm(flags, true);

        final Capability capa = null;
        final ImapRequest cmd = new UidSearchCommand(msgsets, messageFlagTerms, capa);
        Assert.assertEquals(cmd.getCommandLine(), "UID SEARCH DELETED SEEN\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with none-null message sequences set, null SearchTerm.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineNoneNullMessageSeqSetsNullSearchTermNullCharset()
            throws IOException, IllegalArgumentException, IllegalAccessException, SearchException, ImapAsyncClientException {
        final int[] msgNos = { 1, 2, 3, 4 };
        final MessageNumberSet[] msgsets = MessageNumberSet.createMessageNumberSets(msgNos);
        final FlagTerm messageFlagTerms = null;

        final Capability capa = null;
        final ImapRequest cmd = new UidSearchCommand(msgsets, messageFlagTerms, capa);
        Assert.assertEquals(cmd.getCommandLine(), "UID SEARCH 1:4\r\n", "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with none-null message sequences set, null SearchTerm.
     *
     * @throws IOException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandLineNullMessageSeqSetsNullSearchTerm()
            throws IOException, IllegalArgumentException, SearchException {
        final MessageNumberSet[] msgSets = null;
        final String charset = null;
        ImapAsyncClientException actualEx = null;
        final Capability capa = null;
        try {
            new UidSearchCommand(msgSets, null, capa);
        } catch (final ImapAsyncClientException ex) {
            actualEx = ex;
        }
        Assert.assertNotNull(actualEx, "Expecting exception to be thrown");
        Assert.assertEquals(actualEx.getFailureType(), FailureType.INVALID_INPUT, "Incorrect failure type.");
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
        final int[] msgNos = { 1, 2, 3 };
        final MessageNumberSet[] msgsets = MessageNumberSet.createMessageNumberSets(msgNos);

        final SubjectTerm term = new SubjectTerm("ΩΩ"); // have none-ascii characters

        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.LITERAL_PLUS, Collections.singletonList(ImapClientConstants.LITERAL_PLUS));
        final ImapRequest cmd = new UidSearchCommand(msgsets, term, new Capability(capas));
        final String expectedStr = "UID SEARCH CHARSET UTF-8 1:3 SUBJECT {4+}\r\nΩΩ\r\n";
        Assert.assertEquals(cmd.getCommandLineBytes().toString(StandardCharsets.UTF_8), expectedStr, "Expected result mismatched.");

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with Literal+.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineWithLiteralPlusCapaEnabled()
            throws IOException, IllegalArgumentException, IllegalAccessException, ImapAsyncClientException {

        final Argument args = new Argument();

        // Chinese input: \u5929\u5c31\u7070\u7070\u7684
        final String input = "\u5929\u5C31\u7070\u7070\u7684";
        final byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        final SimpleLiteral byteLiteral = new SimpleLiteral(inputBytes);
        args.writeBytes(byteLiteral);

        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.LITERAL_PLUS, Collections.singletonList(ImapClientConstants.LITERAL_PLUS));
        final ImapRequest cmd = new UidSearchCommand(null, "UTF-8", args, new Capability(capas));
        final ByteArrayOutputStream expectedOutput = new ByteArrayOutputStream();
        final String cmdLine = "UID SEARCH CHARSET UTF-8 {15+}\r\n";
        expectedOutput.write(cmdLine.getBytes(StandardCharsets.UTF_8));
        expectedOutput.write(inputBytes);
        expectedOutput.write('\r');
        expectedOutput.write('\n');
        final byte[] expected = expectedOutput.toByteArray();
        final ByteBuf actual = cmd.getCommandLineBytes();
        Assert.assertEquals(actual.readableBytes(), expected.length, "Expected result mismatched.");
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(actual.getByte(i), expected[i], "byte mismatched in index:" + i);
        }

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method without Literal+.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineWithoutLiteralPlusCapaEnabled()
            throws IOException, IllegalArgumentException, IllegalAccessException, ImapAsyncClientException {

        final Argument args = new Argument();

        // Chinese input: \u5929\u5c31\u7070\u7070\u7684
        final String input = "\u5929\u5C31\u7070\u7070\u7684";
        final byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        final SimpleLiteral byteLiteral = new SimpleLiteral(inputBytes);
        args.writeBytes(byteLiteral);

        final Capability capa = null;
        final ImapRequest cmd = new UidSearchCommand(null, "UTF-8", args, capa);
        final ByteArrayOutputStream expectedOutput = new ByteArrayOutputStream();
        final String cmdLine = "UID SEARCH CHARSET UTF-8 {15}\r\n";
        expectedOutput.write(cmdLine.getBytes(StandardCharsets.UTF_8));
        expectedOutput.write(inputBytes);
        expectedOutput.write('\r');
        expectedOutput.write('\n');
        final byte[] expected = expectedOutput.toByteArray();
        final ByteBuf actual = cmd.getCommandLineBytes();
        Assert.assertEquals(actual.readableBytes(), expected.length, "Expected result mismatched.");
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(actual.getByte(i), expected[i], "byte mismatched in index:" + i);
        }

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with none-null message sequences set, none-null SearchTerm.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineNullArgumentNoneNullMessageSequence()
            throws IOException, IllegalArgumentException, IllegalAccessException, ImapAsyncClientException {

        final Argument args = null;
        final String msgSeq = "1:*";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.LITERAL_PLUS, Collections.singletonList(ImapClientConstants.LITERAL_PLUS));
        final ImapRequest cmd = new UidSearchCommand(msgSeq, null, args, new Capability(capas));
        final ByteArrayOutputStream expectedOutput = new ByteArrayOutputStream();
        final String cmdLine = "UID SEARCH 1:*\r\n";
        expectedOutput.write(cmdLine.getBytes(StandardCharsets.UTF_8));

        final byte[] expected = expectedOutput.toByteArray();
        final ByteBuf actual = cmd.getCommandLineBytes();
        Assert.assertEquals(actual.readableBytes(), expected.length, "Expected result mismatched.");
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(actual.getByte(i), expected[i], "byte mismatched in index:" + i);
        }

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Tests getCommandLine method with none-null message sequences set, none-null SearchTerm.
     *
     * @throws IOException will not throw
     * @throws IllegalAccessException will not throw
     * @throws IllegalArgumentException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineNoneNullMessageSequenceNoneNullArgument()
            throws IOException, IllegalArgumentException, IllegalAccessException, ImapAsyncClientException {

        final Argument args = new Argument();
        args.writeAtom("ALL");
        args.writeAtom("UID");
        final String msgSeq = "1:*";
        final Map<String, List<String>> capas = new HashMap<String, List<String>>();
        capas.put(ImapClientConstants.LITERAL_PLUS, Collections.singletonList(ImapClientConstants.LITERAL_PLUS));
        final ImapRequest cmd = new UidSearchCommand(msgSeq, null, args, new Capability(capas));
        final ByteArrayOutputStream expectedOutput = new ByteArrayOutputStream();
        final String cmdLine = "UID SEARCH 1:* ALL UID\r\n";
        expectedOutput.write(cmdLine.getBytes(StandardCharsets.UTF_8));

        final byte[] expected = expectedOutput.toByteArray();
        final ByteBuf actual = cmd.getCommandLineBytes();
        Assert.assertEquals(actual.readableBytes(), expected.length, "Expected result mismatched.");
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(actual.getByte(i), expected[i], "byte mismatched in index:" + i);
        }

        cmd.cleanup();
        // Verify if cleanup happened correctly.
        for (final Field field : fieldsToCheck) {
            Assert.assertNull(field.get(cmd), "Cleanup should set " + field.getName() + " as null");
        }
    }

    /**
     * Literal implementation.
     */
    public class SimpleLiteral implements Literal {
        /** Message Data as byte array. */
        private final byte[] msgData;

        /**
         * Creates a byte literal.
         *
         * @param data - message data in ascii
         */
        public SimpleLiteral(final byte[] data) {
            msgData = data;
        }

        @Override
        public int size() {
            return msgData.length;
        }

        @Override
        public void writeTo(final OutputStream os) throws IOException {
            os.write(msgData);
        }

    }

    /**
     * Tests getCommandType method.
     *
     * @throws ImapAsyncClientException will not throw
     * @throws IOException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testGetCommandType() throws ImapAsyncClientException, SearchException, IOException {
        final Capability capa = null;
        final ImapRequest cmd = new UidSearchCommand(new MessageNumberSet[] { new MessageNumberSet(1, LastMessage.LAST_MESSAGE) }, null, capa);
        Assert.assertSame(cmd.getCommandType(), ImapRFCSupportedCommandType.UID_SEARCH);
    }
}