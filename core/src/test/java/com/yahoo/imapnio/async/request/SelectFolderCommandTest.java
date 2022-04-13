package com.yahoo.imapnio.async.request;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.data.QResyncParameter;
import com.yahoo.imapnio.async.data.QResyncSeqMatchData;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * Unit test for {@link SelectFolderCommand}.
 */
public class SelectFolderCommandTest {
    /** Literal for SELECT. */
    private static final String SELECT = "SELECT ";

    /** Fields to check for cleanup. */
    private Set<Field> fieldsToCheck;

    /**
     * Setup reflection.
     */
    @BeforeClass
    public void setUp() {
        // Use reflection to get all declared non-primitive non-static fields (We do not care about inherited fields)
        final Class<?> classUnderTest = SelectFolderCommand.class;
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
    public void testGetCommandLine() throws ImapAsyncClientException, IllegalArgumentException, IllegalAccessException {
        final String folderName = "folderABC";
        final ImapRequest cmd = new SelectFolderCommand(folderName);
        Assert.assertEquals(cmd.getCommandLine(), SELECT + folderName + "\r\n", "Expected result mismatched.");

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
        final ImapRequest cmd = new SelectFolderCommand(folderName);
        Assert.assertEquals(cmd.getCommandLine(), SELECT + "\"" + folderName + "\"\r\n", "Expected result mismatched.");
    }

    /**
     * Tests getCommandLine method with folder name with other character set encoding.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineWithOtherCharSet() throws ImapAsyncClientException {
        final String folderName = "测试";
        final ImapRequest cmd = new SelectFolderCommand(folderName);
        Assert.assertEquals(cmd.getCommandLine(), SELECT + "&bUuL1Q-\r\n", "Expected result mismatched.");
    }

    /**
     * Tests getCommandType method.
     */
    @Test
    public void testGetCommandType() {
        final ImapRequest cmd = new SelectFolderCommand("inbox");
        Assert.assertSame(cmd.getCommandType(), ImapRFCSupportedCommandType.SELECT_FOLDER);
    }

    /**
     * Tests getCommandLine method with QRESYNC parameter.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineWithQResyncParam() throws ImapAsyncClientException {
        final String folderName = "测试";
        final long knownUidValidity = 100;
        final long knownModSeq = 4223212;
        QResyncParameter qResyncParameter = new QResyncParameter(knownUidValidity, knownModSeq, null, null);
        ImapRequest cmd = new SelectFolderCommand(folderName, qResyncParameter);
        Assert.assertEquals(cmd.getCommandLine(), SELECT + "&bUuL1Q- (QRESYNC (100 4223212))\r\n", "Expected result mismatched.");

        final MessageNumberSet[] uids = new MessageNumberSet[] { new MessageNumberSet(1, 200) };
        qResyncParameter = new QResyncParameter(knownUidValidity, knownModSeq, uids, null);
        cmd = new SelectFolderCommand(folderName, qResyncParameter);
        Assert.assertEquals(cmd.getCommandLine(), SELECT + "&bUuL1Q- (QRESYNC (100 4223212 1:200))\r\n", "Expected result mismatched.");

        final MessageNumberSet[] messageSeqNumbers = new MessageNumberSet[] { new MessageNumberSet(1, 1) };
        final MessageNumberSet[] matchUids = new MessageNumberSet[] { new MessageNumberSet(1, 10) };
        QResyncSeqMatchData seqMatchData = new QResyncSeqMatchData(messageSeqNumbers, matchUids);
        qResyncParameter = new QResyncParameter(knownUidValidity, knownModSeq, uids, seqMatchData);
        cmd = new SelectFolderCommand(folderName, qResyncParameter);
        Assert.assertEquals(cmd.getCommandLine(), SELECT + "&bUuL1Q- (QRESYNC (100 4223212 1:200 (1 1:10)))\r\n", "Expected result mismatched.");
    }

    /**
     * Tests getCommandLine method with CONDSTORE enabled.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testGetCommandLineWithCondStore() throws ImapAsyncClientException {
        final String folderName = "测试";
        ImapRequest cmd = new SelectFolderCommand(folderName, true);
        Assert.assertEquals(cmd.getCommandLine(), SELECT + "&bUuL1Q- (CONDSTORE)\r\n", "getCommandLine() mismatched.");
    }
}
