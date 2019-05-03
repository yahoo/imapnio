package com.lafaspot.imapnio.async.client;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.async.client.ImapFuture;
import com.lafaspot.imapnio.async.response.ImapAsyncResponse;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * Unit tests of {@link ImapFuture}.
 *
 * @author pulkitg
 *
 */
public class ImapFutureTest {

    /** Timeout time. */
    private static final long TIME_OUT_MILLIS = 1000L;

    /** ImapAsyncResponse for the future. */
    private ImapAsyncResponse imapAsyncResp;

    /**
     * An initial setup to mock and set the dependencies used by the ImapFuture.
     *
     * @throws ProtocolException will not throw
     * @throws IOException will not throw
     */
    @BeforeMethod
    public void beforeMethod() throws IOException, ProtocolException {
        final Collection<IMAPResponse> imapResponses = new ArrayList<IMAPResponse>();
        final IMAPResponse oneImapResponse = new IMAPResponse("a1 OK AUTHENTICATE completed");
        imapResponses.add(oneImapResponse);
        imapAsyncResp = new ImapAsyncResponse(imapResponses);
    }

    /**
     * Tests to verify isCancelled method.
     */
    @Test
    public void testIsCancelledT() {
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();
        final boolean isCancelled = imapFuture.isCancelled();

        assertFalse(isCancelled, "isCancelled should be false");
    }

    /**
     * Tests to verify isDone method.
     */
    @Test
    public void testIsDone() {
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();
        final boolean isDone = imapFuture.isDone();

        assertFalse(isDone, "isDone should be false");
    }

    /**
     * Tests to verify cancel method when mayInterruptIfRunning is true, it will be cancelled.
     */
    @Test
    public void testCancel() {
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();
        final boolean mayInterruptIfRunning = true;
        final boolean cancelSuccess = imapFuture.cancel(mayInterruptIfRunning);

        assertTrue(cancelSuccess, "cancel operation should return true to reflect success");
        assertTrue(imapFuture.isCancelled(), "isCancelled() should be true after cancel() operation.");
    }

    /**
     * Tests to verify get method when ImapResult is received and isDone is true.
     *
     * @throws ExecutionException if thread is interrupted
     * @throws InterruptedException if thread fails
     */
    @Test
    public void testGetIsDoneTrueWithImapResult() throws InterruptedException, ExecutionException {
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();
        imapFuture.done(imapAsyncResp);
        // do done again, it should have no harm
        imapFuture.done(imapAsyncResp);

        Assert.assertTrue(imapFuture.isDone(), "Future should be done");
        final ImapAsyncResponse result = imapFuture.get();


        assertNotNull(result, "result should not be null");
        assertEquals(result, imapAsyncResp, "result mismatched");
    }

    /**
     * Tests to verify get method when Exception is received and isDone is true.
     *
     * @throws ExecutionException if thread is interrupted
     * @throws InterruptedException if thread fails
     */
    @Test(expectedExceptions = ExecutionException.class, expectedExceptionsMessageRegExp = "java.lang.Exception: test")
    public void testGetIsDoneTrueWithException() throws InterruptedException, ExecutionException {
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();

        imapFuture.done(new Exception("test"));
        // do done again, it should be no-op
        imapFuture.done(new Exception("test2"));

        Assert.assertTrue(imapFuture.isDone(), "Future should be done");
        imapFuture.get();
    }

    /**
     * Tests to verify get method when future is cancelled.
     *
     * @throws ExecutionException if thread is interrupted
     * @throws InterruptedException if thread fails
     */
    @Test
    public void testGetFutureCancelled() throws InterruptedException {
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();
        imapFuture.cancel(true);

        Assert.assertTrue(imapFuture.isDone(), "Future should be done");
        ExecutionException ex = null;
        try {
            imapFuture.get();
        } catch (final ExecutionException ee) {
            ex = ee;
        }
        Assert.assertNotNull(ex, "Expect exception to be thrown.");
        Assert.assertNotNull(ex.getCause(), "Expect cause.");
        Assert.assertEquals(ex.getCause().getClass(), CancellationException.class, "Expected result mismatched.");
    }

    /**
     * Tests to verify get with timeout method when ImapResult is received and isDone is true.
     *
     * @throws ExecutionException if thread is interrupted
     * @throws InterruptedException if thread fails
     * @throws TimeoutException if result is not found after timeout
     */
    @Test
    public void testGetResultWithTimeOutlimit() throws InterruptedException, ExecutionException, TimeoutException {
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();
        imapFuture.done(imapAsyncResp);
        final ImapAsyncResponse result = imapFuture.get(TIME_OUT_MILLIS, TimeUnit.MILLISECONDS);

        assertNotNull(result, "result should not be null");
        assertEquals(result, imapAsyncResp, "result mismatched");
    }

    /**
     * Tests to verify get with timeout method when Exception is received and isDone is true.
     *
     * @throws ExecutionException if thread is interrupted
     * @throws InterruptedException if thread fails
     * @throws TimeoutException if result is not found after timeout
     */
    @Test(expectedExceptions = ExecutionException.class, expectedExceptionsMessageRegExp = "java.lang.Exception: test")
    public void testGetExceptionWithTimeOutlimit() throws InterruptedException, ExecutionException, TimeoutException {
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();

        imapFuture.done(new Exception("test"));
        imapFuture.get(TIME_OUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Tests to verify get with timeout method when future is timed out and TimeoutException is thrown.
     *
     * @throws ExecutionException if thread is interrupted
     * @throws InterruptedException if thread fails
     * @throws TimeoutException if result is not found after timeout
     */
    @Test(expectedExceptions = TimeoutException.class, expectedExceptionsMessageRegExp = "Timeout reached.")
    public void testGetTimeoutException() throws InterruptedException, ExecutionException, TimeoutException {
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();
        final long mockTimeoutForFailure = 1L;
        imapFuture.get(mockTimeoutForFailure, TimeUnit.MILLISECONDS);
    }

}