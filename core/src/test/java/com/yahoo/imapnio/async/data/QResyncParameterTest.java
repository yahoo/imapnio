package com.yahoo.imapnio.async.data;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@link QResyncParameter}.
 */
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
        final MessageNumberSet[] uids = new MessageNumberSet[] { new MessageNumberSet(1, 5) };
        final MessageNumberSet[] expectedUids = new MessageNumberSet[] { new MessageNumberSet(1, 5) };
        final QResyncParameter qResyncParameter = new QResyncParameter(100L, 200L, uids, null);
        Assert.assertEquals(qResyncParameter.getKnownUids(), expectedUids, "UIDs should match");
    }

    /**
     * Test the get method for message sequence set.
     */
    @Test
    public void testGetSeqMatchData() {
        final MessageNumberSet[] uids = new MessageNumberSet[] { new MessageNumberSet(1, 5) };
        final MessageNumberSet[] knownSeqSet = new MessageNumberSet[] { new MessageNumberSet(1, 20) };
        final MessageNumberSet[] knownUidSet = new MessageNumberSet[] { new MessageNumberSet(1, 10) };
        final QResyncSeqMatchData seqMatchData = new QResyncSeqMatchData(knownSeqSet, knownUidSet);
        final QResyncParameter qResyncParameter = new QResyncParameter(100L, 200L, uids, seqMatchData);
        Assert.assertEquals(seqMatchData, qResyncParameter.getSeqMatchData());
    }
}
