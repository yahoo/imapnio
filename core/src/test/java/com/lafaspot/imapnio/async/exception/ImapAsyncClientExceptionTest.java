package com.lafaspot.imapnio.async.exception;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;

/**
 * Unit test for {@code ImapAsyncClientException}.
 */
public class ImapAsyncClientExceptionTest {

    /**
     * Tests ImapAsyncClientException.
     */
    @Test
    public void testImapAsyncClientException() {
        final ImapAsyncClientException.FailureType failureType = ImapAsyncClientException.FailureType.CHANNEL_DISCONNECTED;
        final ImapAsyncClientException resp = new ImapAsyncClientException(failureType);

        Assert.assertEquals(resp.getFaiureType(), failureType, "result mismatched.");
        Assert.assertEquals(resp.getMessage(), "failureType=" + failureType.name(), "result mismatched.");
    }

    /**
     * Tests ImapAsyncClientException when failureType is null.
     */
    @Test
    public void testFailureType() {
        final ImapAsyncClientException.FailureType failureType = ImapAsyncClientException.FailureType.valueOf("CHANNEL_DISCONNECTED");
        Assert.assertEquals(failureType, ImapAsyncClientException.FailureType.CHANNEL_DISCONNECTED, "result mismatched.");
        Assert.assertEquals(ImapAsyncClientException.FailureType.values().length, 14, "Number of enums mismatched.");
    }
}
