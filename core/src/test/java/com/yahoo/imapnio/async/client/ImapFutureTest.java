package com.yahoo.imapnio.async.client;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.response.ImapAsyncResponse;

/**
 * Unit tests of {@link ImapFuture}.
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
     * Tests to verify error callback is not called when the future is cancelled.
     */
    @Test
    public void testFutureErrorCallbackUponCancelled() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();
        imapFuture.setExceptionCallback(new Consumer<Exception>() {
            @Override
            public void accept(final Exception e) {
                called.set(true);
            }
        });
        imapFuture.cancel(true);

        Assert.assertFalse(called.get(), "Callback should not be run");
    }

    /**
     * Tests to verify error callback is not called when the future is done.
     */
    @Test
    public void testFutureErrorCallbackUponDone() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<Boolean> imapFuture = new ImapFuture<Boolean>();
        imapFuture.setExceptionCallback(new Consumer<Exception>() {
            @Override
            public void accept(final Exception e) {
                called.set(true);
            }
        });
        imapFuture.done(false);

        Assert.assertFalse(called.get(), "Callback should not be run");
    }

    /**
     * Tests to verify error callback is called when the future fails.
     */
    @Test
    public void testFutureErrorCallbackUponException() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<Boolean> imapFuture = new ImapFuture<Boolean>();
        imapFuture.setExceptionCallback(new Consumer<Exception>() {
            @Override
            public void accept(final Exception e) {
                called.set(true);
            }
        });
        imapFuture.done(new RuntimeException());

        Assert.assertTrue(called.get(), "Callback should be run");
    }

    /**
     * Tests to verify done callback is not called when the future is cancelled.
     */
    @Test
    public void testFutureDoneCallbackUponCancelled() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();
        imapFuture.setDoneCallback(new Consumer<ImapAsyncResponse>() {
            @Override
            public void accept(final ImapAsyncResponse imapAsyncResponse) {
                called.set(true);
            }
        });
        imapFuture.cancel(true);

        Assert.assertFalse(called.get(), "Callback should not be run");
    }

    /**
     * Tests to verify done callback is called when the future is done.
     */
    @Test
    public void testFutureDoneCallbackUponDone() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<Boolean> imapFuture = new ImapFuture<Boolean>();
        imapFuture.setDoneCallback(new Consumer<Boolean>() {
            @Override
            public void accept(final Boolean r) {
                called.set(true);
            }
        });
        imapFuture.done(false);

        Assert.assertTrue(called.get(), "Callback should be run");
    }

    /**
     * Tests to verify done callback is not called when the future fails.
     */
    @Test
    public void testFutureDoneCallbackUponException() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<Boolean> imapFuture = new ImapFuture<Boolean>();
        imapFuture.setDoneCallback(new Consumer<Boolean>() {
            @Override
            public void accept(final Boolean r) {
                called.set(true);
            }
        });
        imapFuture.done(new RuntimeException());

        Assert.assertFalse(called.get(), "Callback should not be run");
    }

    /**
     * Tests to verify cancel callback is called when the future is cancelled.
     */
    @Test
    public void testFutureCancelCallbackUponCancelled() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();
        imapFuture.setCanceledCallback(new Runnable() {
            @Override
            public void run() {
                called.set(true);
            }
        });
        imapFuture.cancel(true);

        Assert.assertTrue(called.get(), "Callback should be run");
    }

    /**
     * Tests to verify cancel callback is not called when the future is done.
     */
    @Test
    public void testFutureCancelCallbackUponDone() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<Boolean> imapFuture = new ImapFuture<Boolean>();
        imapFuture.setCanceledCallback(new Runnable() {
            @Override
            public void run() {
                called.set(true);
            }
        });
        imapFuture.done(false);

        Assert.assertFalse(called.get(), "Callback should not be run");
    }

    /**
     * Tests to verify cancel callback is not called when the future fails.
     */
    @Test
    public void testFutureCancelCallbackUponException() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<Boolean> imapFuture = new ImapFuture<Boolean>();
        imapFuture.setCanceledCallback(new Runnable() {
            @Override
            public void run() {
                called.set(true);
            }
        });
        imapFuture.done(new RuntimeException());

        Assert.assertFalse(called.get(), "Callback should not be run");
    }

    /**
     * Tests to verify error callback is not called when the future is cancelled.
     *
     * The future execution is already finished when the callback is registered.
     */
    @Test
    public void testFutureErrorCallbackUponCancelledWhenFinished() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();
        imapFuture.cancel(true);
        imapFuture.setExceptionCallback(new Consumer<Exception>() {
            @Override
            public void accept(final Exception e) {
                called.set(true);
            }
        });

        Assert.assertFalse(called.get(), "Callback should not be run");
    }

    /**
     * Tests to verify error callback is not called when the future is done.
     *
     * The future execution is already finished when the callback is registered.
     */
    @Test
    public void testFutureErrorCallbackUponDoneWhenFinished() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<Boolean> imapFuture = new ImapFuture<Boolean>();
        imapFuture.done(false);
        imapFuture.setExceptionCallback(new Consumer<Exception>() {
            @Override
            public void accept(final Exception e) {
                called.set(true);
            }
        });

        Assert.assertFalse(called.get(), "Callback should not be run");
    }

    /**
     * Tests to verify error callback is called when the future fails.
     *
     * The future execution is already finished when the callback is registered.
     */
    @Test
    public void testFutureErrorCallbackUponExceptionWhenFinished() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<Boolean> imapFuture = new ImapFuture<Boolean>();
        imapFuture.done(new RuntimeException());
        imapFuture.setExceptionCallback(new Consumer<Exception>() {
            @Override
            public void accept(final Exception e) {
                called.set(true);
            }
        });

        Assert.assertTrue(called.get(), "Callback should be run");
    }

    /**
     * Tests to verify done callback is not called when the future is cancelled.
     *
     * The future execution is already finished when the callback is registered.
     */
    @Test
    public void testFutureDoneCallbackUponCancelledWhenFinished() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();
        imapFuture.cancel(true);
        imapFuture.setDoneCallback(new Consumer<ImapAsyncResponse>() {
            @Override
            public void accept(final ImapAsyncResponse imapAsyncResponse) {
                called.set(true);
            }
        });

        Assert.assertFalse(called.get(), "Callback should not be run");
    }

    /**
     * Tests to verify done callback is called when the future is done.
     *
     * The future execution is already finished when the callback is registered.
     */
    @Test
    public void testFutureDoneCallbackUponDoneWhenFinished() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<Boolean> imapFuture = new ImapFuture<Boolean>();
        imapFuture.done(false);
        imapFuture.setDoneCallback(new Consumer<Boolean>() {
            @Override
            public void accept(final Boolean r) {
                called.set(true);
            }
        });

        Assert.assertTrue(called.get(), "Callback should be run");
    }

    /**
     * Tests to verify done callback is not called when the future fails.
     *
     * The future execution is already finished when the callback is registered.
     */
    @Test
    public void testFutureDoneCallbackUponExceptionWhenFinished() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<Boolean> imapFuture = new ImapFuture<Boolean>();
        imapFuture.done(new RuntimeException());
        imapFuture.setDoneCallback(new Consumer<Boolean>() {
            @Override
            public void accept(final Boolean r) {
                called.set(true);
            }
        });

        Assert.assertFalse(called.get(), "Callback should not be run");
    }

    /**
     * Tests to verify cancel callback is called when the future is cancelled.
     *
     * The future execution is already finished when the callback is registered.
     */
    @Test
    public void testFutureCancelCallbackUponCancelledWhenFinished() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<ImapAsyncResponse> imapFuture = new ImapFuture<ImapAsyncResponse>();
        imapFuture.cancel(true);
        imapFuture.setCanceledCallback(new Runnable() {
            @Override
            public void run() {
                called.set(true);
            }
        });

        Assert.assertTrue(called.get(), "Callback should be run");
    }

    /**
     * Tests to verify cancel callback is not called when the future is done.
     *
     * The future execution is already finished when the callback is registered.
     */
    @Test
    public void testFutureCancelCallbackUponDoneWhenFinished() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<Boolean> imapFuture = new ImapFuture<Boolean>();
        imapFuture.done(false);
        imapFuture.setCanceledCallback(new Runnable() {
            @Override
            public void run() {
                called.set(true);
            }
        });

        Assert.assertFalse(called.get(), "Callback should not be run");
    }

    /**
     * Tests to verify cancel callback is not called when the future fails.
     *
     * The future execution is already finished when the callback is registered.
     */
    @Test
    public void testFutureCancelCallbackUponExceptionWhenFinished() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final ImapFuture<Boolean> imapFuture = new ImapFuture<Boolean>();
        imapFuture.done(new RuntimeException());
        imapFuture.setCanceledCallback(new Runnable() {
            @Override
            public void run() {
                called.set(true);
            }
        });

        Assert.assertFalse(called.get(), "Callback should not be run");
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