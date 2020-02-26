package com.yahoo.imapnio.async.data;

import java.util.Collections;
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
        final List<Long> ll = Collections.singletonList(Long.MAX_VALUE - 1);
        final SearchResult sr = new SearchResult(ll, 1L);
        final List<Long> result = sr.getMessageNumbers();
        final Long modSeq = sr.getHighestModSeq();

        Assert.assertNotNull(result, "getMessageNumbers() should not return null.");
        Assert.assertEquals(result.size(), 1, "getMessageNumbers() size mismatched.");
        Assert.assertEquals(result.get(0), Long.valueOf(Long.MAX_VALUE - 1), "getMessageNumbers() mismatched.");
        Assert.assertNotNull(modSeq, "getHighestModSeq() should not return null");
        Assert.assertEquals(modSeq, Long.valueOf(1), "getHighestModSeq() mismatched.");
    }
}
