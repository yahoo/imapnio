/**
 *
 */
package com.lafaspot.imapnio.listener;

import com.lafaspot.imapnio.channel.IMAPChannelFuture;

/**
 * @author kraman
 *
 */
public interface IMAPChannelFutureListener {
    /**
     * Invoked when the operation associated with the {@link io.netty.util.concurrent.Future} has been completed.
     *
     * @param future
     *            the source {@link IMAPChannelFuture} which called this callback
     * @throws Exception
     *             FIXME: why do we use exception - lafa
     */
    void operationComplete(IMAPChannelFuture future) throws Exception;
}
