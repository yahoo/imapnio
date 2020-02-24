package com.yahoo.imapnio.async.client;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@link ImapAsyncSessionConfig}.
 */
public class ImapAsyncSessionConfigTest {

    /**
     * Tests ImapAsyncSessionConfig constructor and getters.
     */
    @Test
    public void testGettersSetters() {

        final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
        final int connectionTimeout = 1000;
        config.setConnectionTimeoutMillis(connectionTimeout);
        Assert.assertEquals(config.getConnectionTimeoutMillis(), connectionTimeout, "Result mismatched.");

        final int readTimeout = 2000;
        config.setReadTimeoutMillis(readTimeout);
        Assert.assertEquals(config.getReadTimeoutMillis(), readTimeout, "Result mismatched.");
    }

}
