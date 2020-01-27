package com.yahoo.imapnio.async.data;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ChangedSinceTest {
    /**
     * Test the get method for moseq.
     */
    @Test
    public void testGetModSeq() {
        final ChangedSince changedSince = new ChangedSince(100L);
        Assert.assertEquals(100L, changedSince.getModSeq());
    }
}
