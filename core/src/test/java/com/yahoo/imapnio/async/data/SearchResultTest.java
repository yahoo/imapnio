package com.yahoo.imapnio.async.data;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@code SearchResult}.
 */
public class SearchResultTest {

    /**
     * Tests SearchResult constructor and getters.
     */
    @Test
    public void testSearchResult() {
        final List<Integer> ll = new ArrayList<>();
        ll.add(1);
        ll.add(2);
        ll.add(3);
        final SearchResult infos = new SearchResult(ll);
        final List<Integer> result = infos.getMessageSequence();
        Assert.assertEquals(result.size(), 3, "Result mismatched.");
        Assert.assertEquals(result.get(0), Integer.valueOf(1), "Result mismatched.");
        Assert.assertEquals(result.get(1), Integer.valueOf(2), "Result mismatched.");
        Assert.assertEquals(result.get(2), Integer.valueOf(3), "Result mismatched.");
    }

    /**
     * Tests SearchResult constructor and getters when passing null list.
     */
    @Test
    public void testSearchResultNullList() {
        final SearchResult infos = new SearchResult(null);
        final List<Integer> result = infos.getMessageSequence();
        Assert.assertNull(result, "Result mismatched.");
    }
}
