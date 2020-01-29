package com.yahoo.imapnio.async.data;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@link SearchResult}.
 */
public class SearchResultTest {

    /**
     * Tests SearchResult constructor and getters.
     */
    @Test
    public void testSearchResult() {
        final List<Long> ll = new ArrayList<>();
        ll.add(Long.MAX_VALUE - 1);
        final SearchResult infos = new SearchResult(ll, 1L);
        final List<Long> result = infos.getMessageNumbers();
        final long modSeq = infos.getModSeq();
        Assert.assertEquals(result.size(), 1, "Result mismatched.");
        Assert.assertEquals(result.get(0), Long.valueOf(Long.MAX_VALUE - 1), "Result mismatched.");
        Assert.assertEquals(modSeq, 1L, "Result mismatched.");
    }

    /**
     * Tests SearchResult constructor and getters when passing null list and not passing modification sequence.
     */
    @Test
    public void testSearchResultNullList() {
        final SearchResult infos = new SearchResult(null);
        final List<Long> result = infos.getMessageNumbers();
        Assert.assertNull(result, "Result mismatched.");
        Assert.assertNull(infos.getModSeq(), "Result mismatched.");
    }
}
