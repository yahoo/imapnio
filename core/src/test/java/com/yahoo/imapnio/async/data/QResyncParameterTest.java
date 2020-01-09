package com.yahoo.imapnio.async.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class QResyncParameterTest {
    /**
     * Test the get method for known uidvalidity.
     */
    @Test
    public void testGetKnownUidValidity() {
        final QResyncParameter qResyncParameter = new QResyncParameter(100L, 200L, null, null);
        Assert.assertEquals(100L, qResyncParameter.getKnownUidValidity());
    }

    /**
     * Test the get method for known moseq.
     */
    @Test
    public void testGetKnownModSeq() {
        final QResyncParameter qResyncParameter = new QResyncParameter(100L, 200L, null, null);
        Assert.assertEquals(200L, qResyncParameter.getKnownModSeq());
    }

    /**
     * Test the get method for known UIDs.
     */
    @Test
    public void testGetKnownUids() {
        final List<MessageNumberSet> uids = new ArrayList<>();
        uids.add(new MessageNumberSet(1, 5));
        final QResyncParameter qResyncParameter = new QResyncParameter(100L, 200L, uids, null);
        Assert.assertEquals(uids, qResyncParameter.getKnownUids());
    }

    /**
     * Test the get method for message sequence set.
     */
    @Test
    public void testGetQResyncSeqMatchData() {
        final List<MessageNumberSet> uids = Collections.singletonList(new MessageNumberSet(1, 5));
        final List<MessageNumberSet> messageSeqNumbers = Collections.singletonList(new MessageNumberSet(1, 20));
        final List<MessageNumberSet> matchUids =Collections.singletonList(new MessageNumberSet(1, 10));
        final QResyncSeqMatchData qResyncSeqMatchData = new QResyncSeqMatchData(messageSeqNumbers, matchUids);
        final QResyncParameter qResyncParameter = new QResyncParameter(100L, 200L, uids, qResyncSeqMatchData);
        Assert.assertEquals(qResyncSeqMatchData, qResyncParameter.getqResyncSeqMatchData());
    }

    /**
     * Test the toString method.
     */
    @Test
    public void testTestToString() {
        final List<MessageNumberSet> uids = Collections.singletonList(new MessageNumberSet(1, 5));
        final List<MessageNumberSet> messageSeqNumbers = Collections.singletonList(new MessageNumberSet(100, 100));
        final List<MessageNumberSet> matchUids = Collections.singletonList(new MessageNumberSet(200, 200));
        final QResyncSeqMatchData qResyncSeqMatchData = new QResyncSeqMatchData(messageSeqNumbers, matchUids);
        final QResyncParameter qResyncParameter = new QResyncParameter(100L, 200L, uids, qResyncSeqMatchData);
        Assert.assertEquals(qResyncParameter.toString(), "(QRESYNC (100 200 1:5 (100 200)))");

        final QResyncParameter qResyncParameter1 = new QResyncParameter(300L, 400L, null, null);
        Assert.assertEquals(qResyncParameter1.toString(), "(QRESYNC (300 400))");

        final QResyncSeqMatchData qResyncSeqMatchData1 = new QResyncSeqMatchData(messageSeqNumbers, null);
        final QResyncParameter qResyncParameter2 = new QResyncParameter(100L, 200L, uids, qResyncSeqMatchData1);
        Assert.assertEquals(qResyncParameter2.toString(), "(QRESYNC (100 200 1:5 (100)))");

        final QResyncSeqMatchData qResyncSeqMatchData2 = new QResyncSeqMatchData(null, matchUids);
        final QResyncParameter qResyncParameter3 = new QResyncParameter(100L, 200L, uids, qResyncSeqMatchData2);
        Assert.assertEquals(qResyncParameter3.toString(), "(QRESYNC (100 200 1:5 (200)))");
    }
}
