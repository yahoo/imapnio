package com.yahoo.imapnio.async.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class QResyncParameterTest {
    /**
     * Test the get method for uidvalidity.
     */
    @Test
    public void testGetUidValidity() {
        final QResyncParameter qResyncParameter = new QResyncParameter(100L, 200L, null, null);
        Assert.assertEquals(100L, qResyncParameter.getUidValidity());
    }

    /**
     * Test the get method for moseq.
     */
    @Test
    public void testGetModSeq() {
        final QResyncParameter qResyncParameter = new QResyncParameter(100L, 200L, null, null);
        Assert.assertEquals(200L, qResyncParameter.getModSeq());
    }

    /**
     * Test the get method for known UIDs.
     */
    @Test
    public void testGetKnownUids() {
        final List<MessageNumberSet> uids = Collections.singletonList(new MessageNumberSet(1, 5));
        final MessageNumberSet[] expectedUids = new MessageNumberSet[] { new MessageNumberSet(1, 5) };
        final QResyncParameter qResyncParameter = new QResyncParameter(100L, 200L, uids, null);
        Assert.assertEquals(qResyncParameter.getKnownUids(), expectedUids, "UIDs should match");
    }

    /**
     * Test the get method for message sequence set.
     */
    @Test
    public void testGetSeqMatchData() {
        final List<MessageNumberSet> uids = Collections.singletonList(new MessageNumberSet(1, 5));
        final List<MessageNumberSet> knownSeqSet = Collections.singletonList(new MessageNumberSet(1, 20));
        final List<MessageNumberSet> knownUidSet =Collections.singletonList(new MessageNumberSet(1, 10));
        final QResyncSeqMatchData seqMatchData = new QResyncSeqMatchData(knownSeqSet, knownUidSet);
        final QResyncParameter qResyncParameter = new QResyncParameter(100L, 200L, uids, seqMatchData);
        Assert.assertEquals(seqMatchData, qResyncParameter.getSeqMatchData());
    }

    /**
     * Test the buildCommandLine method.
     */
    @Test
    public void testBuildCommandLine() {
        final List<MessageNumberSet> uids = Collections.singletonList(new MessageNumberSet(1, 5));
        final List<MessageNumberSet> knownSeqSet = Collections.singletonList(new MessageNumberSet(100, 100));
        final List<MessageNumberSet> knownUidSet = Collections.singletonList(new MessageNumberSet(200, 200));
        final QResyncSeqMatchData seqMatchData = new QResyncSeqMatchData(knownSeqSet, knownUidSet);
        final QResyncParameter qResyncParameter = new QResyncParameter(100L, 200L, uids, seqMatchData);
        Assert.assertEquals(qResyncParameter.buildCommandLine(), "(QRESYNC (100 200 1:5 (100 200)))");

        final QResyncParameter qResyncParameter1 = new QResyncParameter(300L, 400L, null, null);
        Assert.assertEquals(qResyncParameter1.buildCommandLine(), "(QRESYNC (300 400))");

        final QResyncSeqMatchData qResyncSeqMatchData1 = new QResyncSeqMatchData(knownSeqSet, null);
        final QResyncParameter qResyncParameter2 = new QResyncParameter(100L, 200L, uids, qResyncSeqMatchData1);
        Assert.assertEquals(qResyncParameter2.buildCommandLine(), "(QRESYNC (100 200 1:5 (100)))");

        final QResyncSeqMatchData qResyncSeqMatchData2 = new QResyncSeqMatchData(null, knownUidSet);
        final QResyncParameter qResyncParameter3 = new QResyncParameter(100L, 200L, uids, qResyncSeqMatchData2);
        Assert.assertEquals(qResyncParameter3.buildCommandLine(), "(QRESYNC (100 200 1:5 (200)))");
    }
}
