package com.yahoo.imapnio.async.data;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@link IdResult}.
 */
public class IdResultTest {

    /**
     * Tests IdResult constructor and getters.
     */
    @Test
    public void testIdResult() {
        final Map<String, String> map = new HashMap<>();
        map.put("name", "Cyrus");
        map.put("version", "1.5");
        map.put("os", "sunos");
        map.put("os-version", "5.5");
        final String expectedVal = "mailto:cyrus-bugs+@andrew.cmu.edu";
        map.put("support-url", expectedVal);

        final IdResult idresult = new IdResult(map);

        Assert.assertTrue(idresult.hasKey("name"), "Result mismatched.");
        Assert.assertFalse(idresult.hasKey("Name"), "Result mismatched.");
        Assert.assertTrue(idresult.hasKey("support-url"), "Result mismatched.");
        Assert.assertEquals(idresult.getValue("support-url"), expectedVal, "Result mismatched.");

    }

    /**
     * Tests IdResult constructor and getters.
     */
    @Test
    public void testIdResultNullKey() {
        final Map<String, String> map = new HashMap<>();
        map.put("name", "Cyrus");
        map.put("version", "1.5");
        map.put("os", "sunos");
        map.put("os-version", "5.5");
        final String expectedVal = "mailto:cyrus-bugs+@andrew.cmu.edu";
        map.put("support-url", expectedVal);

        final IdResult idresult = new IdResult(map);
        Assert.assertFalse(idresult.hasKey(null), "Result mismatched.");
        final String nullKey = null;
        Assert.assertNull(idresult.getValue(nullKey), "Result mismatched.");
    }
}
