package com.yahoo.imapnio.async.data;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    public void testGetMessageSequenceSet() {
        final List<MessageNumberSet> uids = new ArrayList<>();
        uids.add(new MessageNumberSet(1, 5));
        final Set<Integer> messageSequenceSet = new LinkedHashSet<>();
        final QResyncParameter qResyncParameter = new QResyncParameter(100L, 200L, uids, messageSequenceSet);
        Assert.assertEquals(messageSequenceSet, qResyncParameter.getMessageSequenceSet());
    }

    /**
     * Test the toString method.
     */
    @Test
    public void testTestToString() {
        final List<MessageNumberSet> uids = new ArrayList<>();
        uids.add(new MessageNumberSet(1, 5));
        final Set<Integer> messageSequenceSet = new LinkedHashSet<>();
        messageSequenceSet.add(100);
        messageSequenceSet.add(200);
        final QResyncParameter qResyncParameter = new QResyncParameter(100L, 200L, uids, messageSequenceSet);

        Assert.assertEquals(qResyncParameter.toString(), "(QRESYNC (100 200 1:5 (100,200)))");

        final QResyncParameter qResyncParameter1 = new QResyncParameter(300L, 400L, null, null);
        Assert.assertEquals(qResyncParameter1.toString(), "(QRESYNC (300 400))");

    }
}
