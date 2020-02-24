package com.yahoo.imapnio.async.data;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@link QResyncSeqMatchData}.
 */
public class QResyncSeqMatchDataTest {
    /**
     * Test the get method for known Sequence set.
     */
    @Test
    public void testGetKnownSequenceSet() {
        final MessageNumberSet[] knownSeqSet = new MessageNumberSet[] { new MessageNumberSet(100, 100) };
        final MessageNumberSet[] expectedMsgSeqNumbers = new MessageNumberSet[] { new MessageNumberSet(100, 100) };
        final MessageNumberSet[] knownUidSet = new MessageNumberSet[] { new MessageNumberSet(200, 200) };
        final QResyncSeqMatchData qResyncSeqMatchData = new QResyncSeqMatchData(knownSeqSet, knownUidSet);
        Assert.assertEquals(qResyncSeqMatchData.getKnownSequenceSet(), expectedMsgSeqNumbers, "Message sequence number not matched");
    }

    /**
     * Test the get method for known UID set.
     */
    @Test
    public void testGetKnownUidSet() {
        final MessageNumberSet[] knownSeqSet = new MessageNumberSet[] { new MessageNumberSet(100, 100) };
        final MessageNumberSet[] knownUidSet = new MessageNumberSet[] {  new MessageNumberSet(200, 200) };
        final MessageNumberSet[] expectedUids = new MessageNumberSet[] { new MessageNumberSet(200, 200) };
        final QResyncSeqMatchData qResyncSeqMatchData = new QResyncSeqMatchData(knownSeqSet, knownUidSet);
        Assert.assertEquals(qResyncSeqMatchData.getKnownUidSet(), expectedUids, "UIDs number not matched");
    }
}
