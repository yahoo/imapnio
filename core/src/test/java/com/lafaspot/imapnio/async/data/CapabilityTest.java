package com.lafaspot.imapnio.async.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.async.data.Capability;

/**
 * Unit test for {@code Capability}.
 */
public class CapabilityTest {

    /**
     * Tests Capability constructor and getters.
     */
    @Test
    public void testCapability() {
        final Map<String, List<String>> map = new HashMap<String, List<String>>();
        map.put("IMAP4rev1".toUpperCase(), Arrays.asList("IMAP4rev1"));
        map.put("AUTH", Arrays.asList("PLAIN", "XOAUTH2", "OAUTHBEARER"));

        final Capability capa = new Capability(map);

        Assert.assertTrue(capa.hasCapability("IMAP4rev1"), "Result mismatched.");
        Assert.assertFalse(capa.hasCapability("IMAP3rev1"), "Result mismatched.");
        final List<String> values = capa.getCapability("AUTH");
        Assert.assertNotNull(values, "Result mismatched.");
        Assert.assertEquals(values.size(), 3, "Result mismatched.");
        Assert.assertEquals(values.get(0), "PLAIN", "Result mismatched.");
        Assert.assertEquals(values.get(1), "XOAUTH2", "Result mismatched.");
        Assert.assertEquals(values.get(2), "OAUTHBEARER", "Result mismatched.");
    }
}
