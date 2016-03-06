/**
 *
 */
package com.lafaspot.imapnio.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.lafaspot.imapnio.listener.IMAPCommandListener;
import com.lafaspot.imapnio.listener.IMAPConnectionListener;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * Handles all responses from the server, parases it and calls the client if a listener is registered. Maintains session state.
 *
 * @author kraman
 *
 */
public class IMAPClientRespHandler extends MessageToMessageDecoder<IMAPResponse> {

    /** The IMAP client session. */
    private final IMAPSession session;

    /**
     * Creates a IMAPClient response handler. The last of the handlers in the chain before giving a callback to the client.
     *
     * @param session
     *            - IMAP session
     */
    public IMAPClientRespHandler(final IMAPSession session) {
        this.session = session;
    }

    /**
     * Decode the incoming IMAP command/response from server. Call the client listener when done.
     *
     * @param ctx - channel context
     * @param msg - incoming IMAP message
     * @param out - message to be sent to the next handler in the chain
     */
    @Override
    public void decode(final ChannelHandlerContext ctx, final IMAPResponse msg, final List<Object> out) {
        final IMAPSessionState state = session.getState().get();
        switch (state) {
        case CONNECT_SENT: {
            final IMAPConnectionListener connectionListener = session.getConnectionListener();
            if (msg.isOK()) {
                if (!session.getState().compareAndSet(IMAPSessionState.CONNECT_SENT, IMAPSessionState.CONNECTED)) {
                    session.getLogger().error("Connect success in invalid state " + session.getState().get().name(), null);
                    break;
                }
                if (null != connectionListener) {
                    connectionListener.onConnect(session);
                }
            } else {
                session.getState().set(IMAPSessionState.DISCONNECTED);
                ctx.channel().close();
                if (null != connectionListener) {
                    connectionListener.onDisconnect(session, new Throwable("Invalid response from server " + msg));
                }
            }
            break;
        }
        case IDLE_SENT: {
            if (msg.readAtomString().equals("idling")) {
                if (!session.getState().compareAndSet(IMAPSessionState.IDLE_SENT, IMAPSessionState.IDLING)) {
                    session.getLogger().error("Received idling in invalid state " + session.getState(), null);
                    return;
                }
                final IMAPConnectionListener connectionListener = session.getConnectionListener();
                // go back so the listener gets everything
                msg.reset();
                if (msg.isContinuation() && connectionListener != null) {
                    // go back so the listener gets everything
                    msg.reset();
                    connectionListener.onMessage(session, msg);
                }
            }
            break;
        }
        case IDLING: {
            final String idleTag = session.getCurrentTagRef().get();
            final IMAPCommandListener callback = session.getCommandListener(idleTag);
            if (null == callback) {
                session.getLogger().error("IDLING - no callback set for tag " + idleTag, null);
                break;
            }
            callback.onMessage(session, msg);
            break;
        }
        case DONE_SENT: {
            final String idleTag = session.getCurrentTagRef().get();
            final IMAPCommandListener callback = (idleTag != null) ? session.getCommandListener(idleTag) : null;
            if (msg.isOK()) {
                if (msg.readAtomString().equals("IDLE") && msg.readAtomString().equals("terminated")) {
                    msg.reset();
                    if (!session.getState().compareAndSet(IMAPSessionState.DONE_SENT, IMAPSessionState.CONNECTED)) {
                        session.getLogger().error("IDLE terminated in invalid state " + session.getState(), null);
                        break;
                    }
                    // reset IDLE tag
                    session.getCurrentTagRef().set(null);
                    if (null != idleTag) {
                        session.removeCommandListener(idleTag);
                    }
                    if (null == callback) {
                        session.getLogger().error("IDLE terminated - no callback for tag " + idleTag, null);
                        break;
                    }
                    callback.onResponse(session, idleTag, Arrays.asList(msg));
                    break;
                }

                // if we still got some IDLE command responses
                if (null == callback) {
                    session.getLogger().error("IDLING - no callback for tag " + idleTag, null);
                    break;
                }
                callback.onMessage(session, msg);
            }
            break;
        }
        case CONNECTED: {
            if (msg.isTagged()) {
                final String msgTag = msg.getTag();
                final String currentTag = session.getCurrentTagRef().get();
                if (null != currentTag && currentTag.equals(msgTag)) {
                    session.getCurrentTagRef().set(null);
                } else {
                    // TODO - should error out
                    session.getLogger().error("Unknown tag - (" + msgTag + "), expected (" + currentTag + "), sess " + session, null);
                }

                session.addResponse(msg);
                final IMAPCommandListener commandListener = session.removeCommandListener(msgTag);
                if (null != commandListener) {
                    commandListener.onResponse(session, msgTag, Collections.unmodifiableList(session.getResponseList()));
                }
                session.resetResponseList();
            } else {
                if (msg.isContinuation()) {
                    final String currentTag = session.getCurrentTagRef().get();
                    final IMAPCommandListener commandCallback = (currentTag != null) ? session.getCommandListener(currentTag) : null;
                    final IMAPConnectionListener connectionCallback = session.getConnectionListener();
                    if (null != commandCallback) {
                        commandCallback.onMessage(session, msg);
                    } else if (null != connectionCallback) {
                        connectionCallback.onMessage(session, msg);
                    }
                } else {
                    session.addResponse(msg);
                }
            }
            break;
        }
        default: {
            session.getLogger().error("Message in invalid state " + state, null);
        }
        }
    }

}
