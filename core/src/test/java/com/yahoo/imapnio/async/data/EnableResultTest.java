package com.yahoo.imapnio.async.data;

import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@link EnableResult}.
 */
public class EnableResultTest {

    /**
     * Tests EnableResult constructor and getters.
     */
    @Test
    public void testEnableResult() {
        final Set<String> set = new HashSet<String>();
        set.add("CONDSTORE");
        set.add("QRSYNC");

        final EnableResult enableresult = new EnableResult(set);

        final Set<String> capas = enableresult.getEnabledCapabilities();
        Assert.assertNotNull(capas, "Result mismatched.");
        Assert.assertEquals(capas.size(), 2, "Result mismatched.");
        Assert.assertTrue(capas.contains("CONDSTORE"), "Result mismatched.");
        Assert.assertTrue(capas.contains("QRSYNC"), "Result mismatched.");
    }
}
