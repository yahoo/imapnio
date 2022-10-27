package com.yahoo.imapnio.async.data;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@link PartialExtensionUidFetchInfo}.
 */
public class PartialExtensionUidFetchInfoTest {

    /**
     * Tests PartialExtensionUidFetchInfo constructor and getters.
     */
    @Test
    public void testPartialExtensionUidFetchInfo() {
        PartialExtensionUidFetchInfo peufi = new PartialExtensionUidFetchInfo(1, 5);
        Assert.assertEquals(peufi.getLowestUid(), 1, "Result mismatched.");
        Assert.assertEquals(peufi.getHighestUid(), 5, "Result mismatched.");

        peufi = new PartialExtensionUidFetchInfo(-1, -5);
        Assert.assertEquals(peufi.getLowestUid(), -1, "Result mismatched.");
        Assert.assertEquals(peufi.getHighestUid(), -5, "Result mismatched.");
    }
}
