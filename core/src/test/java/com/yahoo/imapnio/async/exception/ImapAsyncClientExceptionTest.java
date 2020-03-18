package com.yahoo.imapnio.async.exception;

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@link ImapAsyncClientException}.
 */
public class ImapAsyncClientExceptionTest {

    /**
     * Tests ImapAsyncClientException.
     */
    @Test
    public void testImapAsyncClientException() {
        final ImapAsyncClientException.FailureType failureType = ImapAsyncClientException.FailureType.CHANNEL_DISCONNECTED;
        final ImapAsyncClientException resp = new ImapAsyncClientException(failureType);

        Assert.assertEquals(resp.getFailureType(), failureType, "result mismatched.");
        Assert.assertNull(resp.getCause(), "cause of exception mismatched.");
        Assert.assertEquals(resp.getMessage(), "failureType=" + failureType.name(), "result mismatched.");
    }

    /**
     * Tests ImapAsyncClientException constructor.
     */
    @Test
    public void testImapAsyncClientExceptionWithFailureTypeAndCause() {
        final ImapAsyncClientException.FailureType failureType = ImapAsyncClientException.FailureType.CHANNEL_DISCONNECTED;
        final IOException cause = new IOException("Failure in IO!");
        final ImapAsyncClientException ex = new ImapAsyncClientException(failureType, cause);

        Assert.assertEquals(ex.getFailureType(), failureType, "result mismatched.");
        Assert.assertEquals(ex.getMessage(), "failureType=CHANNEL_DISCONNECTED", "result mismatched.");
        Assert.assertEquals(ex.getCause(), cause, "cause of exception mismatched.");
    }

    /**
     * Tests ImapAsyncClientException constructor.
     */
    @Test
    public void testImapAsyncClientExceptionWithSessionIdClientContext() {
        final ImapAsyncClientException.FailureType failureType = ImapAsyncClientException.FailureType.CHANNEL_DISCONNECTED;
        final Long sessionId = new Long(5);
        final String sessCtx = "T123riceratops123@scar123y.com";
        final ImapAsyncClientException ex = new ImapAsyncClientException(failureType, sessionId, sessCtx);

        Assert.assertEquals(ex.getFailureType(), failureType, "result mismatched.");
        Assert.assertNull(ex.getCause(), "cause of exception mismatched.");
        Assert.assertEquals(ex.getMessage(), "failureType=CHANNEL_DISCONNECTED,sId=5,uId=T123riceratops123@scar123y.com", "result mismatched.");
    }

    /**
     * Tests ImapAsyncClientException when failureType is null.
     */
    @Test
    public void testFailureType() {
        final ImapAsyncClientException.FailureType failureType = ImapAsyncClientException.FailureType.valueOf("CHANNEL_DISCONNECTED");
        Assert.assertEquals(failureType, ImapAsyncClientException.FailureType.CHANNEL_DISCONNECTED, "result mismatched.");
        Assert.assertEquals(ImapAsyncClientException.FailureType.values().length, 17, "Number of enums mismatched.");
    }
}
