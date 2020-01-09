package com.yahoo.imapnio.async.data;

import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class QResyncSeqMatchDataTest {
    /**
     * Test the get method for Message sequence numebers.
     */
    @Test
    public void testGetMessageSeqNumbers() {
        final List<MessageNumberSet> messageSeqNumbers = Collections.singletonList(new MessageNumberSet(100, 100));
        final List<MessageNumberSet> matchUids = Collections.singletonList(new MessageNumberSet(200, 200));
        final QResyncSeqMatchData qResyncSeqMatchData = new QResyncSeqMatchData(messageSeqNumbers, matchUids);
        Assert.assertEquals(messageSeqNumbers, qResyncSeqMatchData.getMessageSeqNumbers(), "Message sequence number not matched");
    }

    /**
     * Test the get method for UIDs.
     */
    @Test
    public void testGetUids() {
        final List<MessageNumberSet> messageSeqNumbers = Collections.singletonList(new MessageNumberSet(100, 100));
        final List<MessageNumberSet> matchUids = Collections.singletonList(new MessageNumberSet(200, 200));
        final QResyncSeqMatchData qResyncSeqMatchData = new QResyncSeqMatchData(messageSeqNumbers, matchUids);
        Assert.assertEquals(matchUids, qResyncSeqMatchData.getUids(), "UIDs number not matched");
    }

    /**
     * Test the toString method.
     */
    @Test
    public void testTestToString() {
        final List<MessageNumberSet> messageSeqNumbers = Collections.singletonList(new MessageNumberSet(100, 100));
        final List<MessageNumberSet> matchUids = Collections.singletonList(new MessageNumberSet(200, 200));
        final QResyncSeqMatchData qResyncSeqMatchData = new QResyncSeqMatchData(messageSeqNumbers, matchUids);
        Assert.assertEquals(qResyncSeqMatchData.toString(), "100 200", "String mismatched");

        final QResyncSeqMatchData qResyncSeqMatchData1 = new QResyncSeqMatchData(messageSeqNumbers, null);
        Assert.assertEquals(qResyncSeqMatchData1.toString(), "100", "String mismatched");

        final QResyncSeqMatchData qResyncSeqMatchData2 = new QResyncSeqMatchData(null, matchUids);
        Assert.assertEquals(qResyncSeqMatchData2.toString(), "200", "String mismatched");

        final QResyncSeqMatchData qResyncSeqMatchData3 = new QResyncSeqMatchData(null, null);
        Assert.assertEquals(qResyncSeqMatchData3.toString(), "", "String mismatched");
    }
}
