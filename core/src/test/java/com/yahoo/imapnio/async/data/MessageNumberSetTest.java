package com.yahoo.imapnio.async.data;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.imapnio.async.data.MessageNumberSet.LastMessage;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

/**
 * Unit test for {@link MessageNumberSet}.
 */
public class MessageNumberSetTest {

    /**
     * Tests constructor and converting it to string.
     */
    @Test
    public void testConstructorWithStartEnd() {
        final MessageNumberSet msgSet = new MessageNumberSet(1, 100);
        Assert.assertNotNull(msgSet, "Should not be null");
        Assert.assertEquals(MessageNumberSet.buildString(new MessageNumberSet[] { msgSet }), "1:100", "Result mismatched.");
    }

    /**
     * Tests constructor where starts with specific message and ends with last message and converting it to string.
     */
    @Test
    public void testConstructorWithStartEndWithLast() {
        final MessageNumberSet msgSet = new MessageNumberSet(1, LastMessage.LAST_MESSAGE);
        Assert.assertNotNull(msgSet, "Should not be null");
        Assert.assertEquals(MessageNumberSet.buildString(new MessageNumberSet[] { msgSet }), "1:*", "Result mismatched.");
    }

    /**
     * Tests constructor where it has only one message and converting it to string.
     */
    @Test
    public void testConstructorWithStartAndEndSame() {
        final MessageNumberSet msgSet = new MessageNumberSet(1, 1);
        Assert.assertNotNull(msgSet, "Should not be null");
        Assert.assertEquals(MessageNumberSet.buildString(new MessageNumberSet[] { msgSet }), "1", "Result mismatched.");
    }

    /**
     * Tests constructor and converting it to string.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testConstructorLastMessageOnlyTrue() throws ImapAsyncClientException {
        final MessageNumberSet msgSet = new MessageNumberSet(LastMessage.LAST_MESSAGE);
        Assert.assertNotNull(msgSet, "Should not be null");
        Assert.assertEquals(MessageNumberSet.buildString(new MessageNumberSet[] { msgSet }), "*", "Result mismatched.");
    }

    /**
     * Tests createMessageNumberSets(int[]) method.
     *
     */
    @Test
    public void testCreateMessageNumberSetsFromIntArray() {
        final int[] msgs = { 1, 2, 3, 4, 5, 7 };

        final MessageNumberSet[] sets = MessageNumberSet.createMessageNumberSets(msgs);
        Assert.assertNotNull(sets, "Should not be null");
        Assert.assertEquals(sets.length, 2, "length mismatched.");
        Assert.assertEquals(MessageNumberSet.buildString(sets), "1:5,7", "Expect result mismatched.");
    }

    /**
     * Tests createMessageNumberSets(int[]) method.
     *
     */
    @Test
    public void testCreateMessageNumberSetsFromLongArray() {
        final long[] msgs = { 1, 2, 3, 4, 5, 7, 1 };

        final MessageNumberSet[] sets = MessageNumberSet.createMessageNumberSets(msgs);
        Assert.assertNotNull(sets, "Should not be null");
        Assert.assertEquals(sets.length, 3, "length mismatched.");
        Assert.assertEquals(MessageNumberSet.buildString(sets), "1:5,7,1", "Expect result mismatched.");
    }

    /**
     * Tests createMessageNumberSets(int[]) method.
     *
     */
    @Test
    public void testRemovePointDuplicates() {
        final long[] msgs = { 1, 1, 1, 1, 1, 1, 1 };

        final MessageNumberSet[] sets = MessageNumberSet.createMessageNumberSets(msgs);
        Assert.assertNotNull(sets, "Should not be null");
        Assert.assertEquals(MessageNumberSet.buildString(sets), "1", "Expect result mismatched.");
    }

    /**
     * Tests createMessageNumberSets(int[]) method.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testRemoveRangesAndPointsDuplicates() throws ImapAsyncClientException {

        final MessageNumberSet[] sets = { new MessageNumberSet(1, 5), new MessageNumberSet(5, 1), new MessageNumberSet(1, LastMessage.LAST_MESSAGE),
                new MessageNumberSet(1, 5), new MessageNumberSet(2, 2), new MessageNumberSet(LastMessage.LAST_MESSAGE), new MessageNumberSet(2, 2) };
        Assert.assertNotNull(sets, "Should not be null");
        Assert.assertEquals(MessageNumberSet.buildString(sets), "1:5,1:*,2,*", "Expect result mismatched.");
    }

    /**
     * Tests buildString method.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testBuildString() throws ImapAsyncClientException {
        final MessageNumberSet[] sets = new MessageNumberSet[3];

        sets[0] = new MessageNumberSet(LastMessage.LAST_MESSAGE);
        sets[1] = new MessageNumberSet(1, 5);
        sets[2] = new MessageNumberSet(3, 3);
        Assert.assertNotNull(sets, "Should not be null");
        Assert.assertEquals(MessageNumberSet.buildString(sets), "*,1:5,3", "Result mismatched.");
    }

    /**
     * Tests constructor and converting it to string.
     */
    @Test
    public void testBuildStringWithNullMessageNumberSets() {
        Assert.assertNull(MessageNumberSet.buildString(null), "Result mismatched.");
    }

    /**
     * Tests constructor and converting it to string.
     */
    @Test
    public void testBuildStringWith0LengthMessageNumberSets() {
        Assert.assertNull(MessageNumberSet.buildString(new MessageNumberSet[0]), "Result mismatched.");
    }

    /**
     * Tests constructor and converting it to string.
     *
     */
    @Test
    public void testConstructorLastMessageOnlyFalse() {
        ImapAsyncClientException actual = null;
        try {
            new MessageNumberSet(null);
        } catch (final ImapAsyncClientException e) {
            actual = e;
        }
        Assert.assertNotNull(actual, "Exception should be thrown");
        Assert.assertEquals(actual.getFailureType(), FailureType.INVALID_INPUT, "Result mismatched.");
    }

    /**
     * Tests equals with same type, start, and ending point.
     */
    @Test
    public void testEqualsTrue() {
        final MessageNumberSet msgSet1 = new MessageNumberSet(1, 100);
        final MessageNumberSet msgSet2 = new MessageNumberSet(1, 100);
        Assert.assertNotNull(msgSet1, "Should not be null");
        Assert.assertTrue(msgSet1.equals(msgSet2), "Result mismatched.");
    }

    /**
     * Tests equals with different class.
     */
    @Test
    public void testEqualsDiffClass() {
        final MessageNumberSet msgSet = new MessageNumberSet(1, 100);
        Assert.assertNotNull(msgSet, "Should not be null");
        final String u = "hello";
        Assert.assertFalse(msgSet.equals(u), "Result mismatched.");
    }

    /**
     * Tests equals with different sequence type.
     */
    @Test
    public void testEqualsDiffSeqType() {
        final MessageNumberSet msgSet1 = new MessageNumberSet(1, 100);
        final MessageNumberSet msgSet2 = new MessageNumberSet(1, LastMessage.LAST_MESSAGE);
        Assert.assertFalse(msgSet1.equals(msgSet2), "Result mismatched.");
    }

    /**
     * Tests equals with different ending point.
     */
    @Test
    public void testEqualsDiffEnd() {
        final MessageNumberSet msgSet1 = new MessageNumberSet(1, 100);
        final MessageNumberSet msgSet2 = new MessageNumberSet(1, 101);
        Assert.assertFalse(msgSet1.equals(msgSet2), "Result mismatched.");
    }

    /**
     * Tests equals with different starting point.
     */
    @Test
    public void testEqualsDiffStart() {
        final MessageNumberSet msgSet1 = new MessageNumberSet(1, 100);
        final MessageNumberSet msgSet2 = new MessageNumberSet(2, 100);
        Assert.assertFalse(msgSet1.equals(msgSet2), "Result mismatched.");
    }

    /**
     * Tests SequenceType enum.
     */
    @Test
    public void testCommandTypeEnum() {
        final LastMessage[] enumList = LastMessage.values();
        Assert.assertEquals(enumList.length, 1, "The enum count mismatched.");
        final LastMessage value = LastMessage.valueOf("LAST_MESSAGE");
        Assert.assertSame(value, LastMessage.LAST_MESSAGE, "Enum does not match.");
    }
}
