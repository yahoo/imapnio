/**
 *
 */
package com.lafaspot.imapnio.channel;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import com.lafaspot.imapnio.listener.IMAPChannelFutureListener;

/**
 * Wraps the Netty ChannelFuture.
 *
 * @author kraman
 *
 */
public class IMAPChannelFuture {
    /** The ChannelFuture object. */
    private final ChannelFuture future;

    /** The client future listener. */
    private IMAPChannelFutureListener futureListener;

    /**
     * Constructs a IMAPClientChannelFuture.
     *
     * @param future the channel future object
     */
    public IMAPChannelFuture(final ChannelFuture future) {
        this.future = future;
    }

    /**
     * Adds the specified listener to this future. The specified listener is notified when this future is is done. If this future is already
     * completed, the specified listener is notified immediately.
     *
     * @param listener
     *            the future listener
     * @return the channel future object
     */
    public IMAPChannelFuture addListener(final IMAPChannelFutureListener listener) {
        this.futureListener = listener;
        final IMAPChannelFuture theFuture = this;
        future.addListener(new GenericFutureListener<Future<? super Void>>() {

            @Override
            public void operationComplete(final Future<? super Void> future) throws Exception {
                if (null != futureListener) {
                    futureListener.operationComplete(theFuture);
                }
            }
        });

        return this;
    }

    /**
     * Waits for this future to be completed without interruption. This method catches an InterruptedException and discards it silently.
     *
     * @return the channel future object
     */

    public IMAPChannelFuture awaitUninterruptibly() {
        return new IMAPChannelFuture(future.awaitUninterruptibly());
    }

    /**
     * Returns {@code true} if and only if the I/O operation was completed successfully.
     *
     * @return if the task was success
     */
    public boolean isSuccess() {
        return future.isSuccess();
    }

    /**
     * returns true if and only if the operation can be cancelled via cancel() API}.
     *
     * @return is the task cancel-able
     */
    public boolean isCancellable() {
        return future.isCancellable();
    }

    /**
     * Cancel an ongoing operation.
     *
     * @param mayInterruptIfRunning force the cancel even if operation is running
     */
    public void cancel(final boolean mayInterruptIfRunning) {
        future.cancel(mayInterruptIfRunning);
    }

    /**
     * Returns the cause of the failed I/O operation if the I/O operation has failed.
     *
     * @return the cause of the failure. {@code null} if succeeded or this future is not completed yet.
     */
    public Throwable cause() {
        return future.cause();
    }

    /**
     * Returns {@code true} if this task completed.
     *
     * Completion may be due to normal termination, an exception, or cancellation -- in all of these cases, this method will return {@code true}.
     *
     * @return {@code true} if this task completed
     */
    public boolean isDone() {
        return future.isDone();
    }

}
