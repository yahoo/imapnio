/**
 *
 */
package com.lafaspot.imapnio.channel;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Wraps the Netty ChannelFuture.
 *
 * @author kraman
 *
 */
public class IMAPClientChannelFuture {
    /** The ChannelFuture object. */
    private final ChannelFuture future;

    /**
     * Constructs a IMAPClientChannelFuture.
     *
     * @param future
     *            the channel future object
     */
    public IMAPClientChannelFuture(final ChannelFuture future) {
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
    public ChannelFuture addListener(final GenericFutureListener<? extends Future<? super Void>> listener) {
        return future.addListener(listener);
    }

    /**
     * Waits for this future to be completed without interruption. This method catches an InterruptedException and discards it silently.
     *
     * @return the channel future object
     */

    public ChannelFuture awaitUninterruptibly() {
        return future.awaitUninterruptibly();
    }
}
