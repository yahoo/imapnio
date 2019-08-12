package com.yahoo.imapnio.async.data;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@code UidSearchResult}.
 */
public class UidSearchResultTest {

    /**
     * Tests SearchResult constructor and getters.
     */
    @Test
    public void testSearchResult() {
        final List<Long> ll = new ArrayList<>();
        ll.add(Long.MAX_VALUE - 1);
        final UidSearchResult infos = new UidSearchResult(ll);
        final List<Long> result = infos.getUids();
        Assert.assertEquals(result.size(), 1, "Result mismatched.");
        Assert.assertEquals(result.get(0), Long.valueOf(Long.MAX_VALUE - 1), "Result mismatched.");
    }

    /**
     * Tests SearchResult constructor and getters when passing null list.
     */
    @Test
    public void testSearchResultNullList() {
        final UidSearchResult infos = new UidSearchResult(null);
        final List<Long> result = infos.getUids();
        Assert.assertNull(result, "Result mismatched.");
    }
}
