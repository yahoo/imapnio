package com.yahoo.imapnio.async.client;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@code ImapAsyncSessionClientContext}.
 */
public class ImapAsyncSessionClientContextTest {

    /**
     * Tests ImapAsyncSessionClientContext constructor and getters.
     */
    @Test
    public void testGettersSetters() {

        final ImapAsyncSessionClientContext clientCtx = new ImapAsyncSessionClientContext();
        clientCtx.setUserId("anythingCanBePossible");
        Assert.assertEquals(clientCtx.getUserId(), "anythingCanBePossible", "Result mismatched.");
    }

}
