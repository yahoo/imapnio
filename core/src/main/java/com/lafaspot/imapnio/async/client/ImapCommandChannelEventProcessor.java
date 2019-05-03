package com.lafaspot.imapnio.async.client;

import com.sun.mail.imap.protocol.IMAPResponse;

import io.netty.handler.timeout.IdleStateEvent;

/**
 * This class handles the event coming from @{code ImapClientCommandRespHandler}.
 */
public interface ImapCommandChannelEventProcessor {
    /**
     * Handles when a channel response arrives.
     * 
     * @param <T> the data type for next command after continuation.
     * @param msg the response
     */
    <T> void handleChannelResponse(IMAPResponse msg);

    /**
     * Handles when a channel has an exception.
     *
     * @param cause the exception
     */
    void handleChannelException(Throwable cause);

    /**
     * Handles when a channel receives an idle-too-long event.
     *
     * @param timeOutEvent the timeout event
     */
    void handleIdleEvent(IdleStateEvent timeOutEvent);

    /**
     * Handles the event when a channel is closed/disconnected either by server or client.
     */
    void handleChannelClosed();

}
