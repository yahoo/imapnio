/**
 *
 */
package com.lafaspot.imapnio.listener;


import com.lafaspot.imapnio.channel.IMAPChannelFuture;

import io.netty.util.concurrent.Future;

/**
 * @author kraman
 *
 */
public interface IMAPChannelFutureListener {
    /**
     * Invoked when the operation associated with the {@link Future} has been completed.
     *
     * @param future  the source {@link IMAPChannelFuture} which called this callback
     */
    void operationComplete(IMAPChannelFuture future) throws Exception;
}
