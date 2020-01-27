package com.yahoo.imapnio.async.data;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UnchangedSinceTest {
    /**
     * Test the get method for moseq.
     */
    @Test
    public void testGetModSeq() {
        final UnchangedSince unchangedSince = new UnchangedSince(100L);
        Assert.assertEquals(100L, unchangedSince.getModSeq());
    }
}
