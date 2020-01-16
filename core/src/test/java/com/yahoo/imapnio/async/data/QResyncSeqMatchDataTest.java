package com.yahoo.imapnio.async.data;

import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class QResyncSeqMatchDataTest {
    /**
     * Test the get method for known Sequence set.
     */
    @Test
    public void testGetKnownSequenceSet() {
        final List<MessageNumberSet> knownSeqSet = Collections.singletonList(new MessageNumberSet(100, 100));
        final MessageNumberSet[] expectedMsgSeqNumbers = new MessageNumberSet[] { new MessageNumberSet(100, 100) };
        final List<MessageNumberSet> knownUidSet = Collections.singletonList(new MessageNumberSet(200, 200));
        final QResyncSeqMatchData qResyncSeqMatchData = new QResyncSeqMatchData(knownSeqSet, knownUidSet);
        Assert.assertEquals(qResyncSeqMatchData.getKnownSequenceSet(), expectedMsgSeqNumbers, "Message sequence number not matched");
    }

    /**
     * Test the get method for known UID set.
     */
    @Test
    public void testGetKnownUidSet() {
        final List<MessageNumberSet> knownSeqSet = Collections.singletonList(new MessageNumberSet(100, 100));
        final List<MessageNumberSet> knownUidSet = Collections.singletonList(new MessageNumberSet(200, 200));
        final MessageNumberSet[] expectedUids = new MessageNumberSet[] { new MessageNumberSet(200, 200) };
        final QResyncSeqMatchData qResyncSeqMatchData = new QResyncSeqMatchData(knownSeqSet, knownUidSet);
        Assert.assertEquals(qResyncSeqMatchData.getKnownUidSet(), expectedUids, "UIDs number not matched");
    }

    /**
     * Test the buildCommandLine method.
     */
    @Test
    public void testBuildCommandLine() {
        final List<MessageNumberSet> knownSeqSet = Collections.singletonList(new MessageNumberSet(100, 100));
        final List<MessageNumberSet> knownUidSet = Collections.singletonList(new MessageNumberSet(200, 200));
        final QResyncSeqMatchData qResyncSeqMatchData = new QResyncSeqMatchData(knownSeqSet, knownUidSet);
        Assert.assertEquals(qResyncSeqMatchData.buildCommandLine(), "100 200", "String mismatched");

        final QResyncSeqMatchData qResyncSeqMatchData1 = new QResyncSeqMatchData(knownSeqSet, null);
        Assert.assertEquals(qResyncSeqMatchData1.buildCommandLine(), "100", "String mismatched");

        final QResyncSeqMatchData qResyncSeqMatchData2 = new QResyncSeqMatchData(null, knownUidSet);
        Assert.assertEquals(qResyncSeqMatchData2.buildCommandLine(), "200", "String mismatched");

        final QResyncSeqMatchData qResyncSeqMatchData3 = new QResyncSeqMatchData(null, null);
        Assert.assertEquals(qResyncSeqMatchData3.buildCommandLine(), "", "String mismatched");
    }
}
