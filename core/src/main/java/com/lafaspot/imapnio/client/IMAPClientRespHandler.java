/**
 *
 */
package com.lafaspot.imapnio.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.Arrays;
import java.util.List;

import com.lafaspot.imapnio.listener.IMAPCommandListener;
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
    	switch (session.getState().get()) {
    	case CONNECT_SENT:
            if (msg.isOK()) {
                session.getState().set(IMAPSessionState.CONNECTED);
                session.resetResponseList();
                if (null != session.getConnectionListener()) {
                    session.getConnectionListener().onConnect(session);
                }
            } else {
                session.getState().set(IMAPSessionState.DISCONNECTED);
                session.getConnectionListener().onDisconnect(session);
            }
            break;
    	case IDLE_SENT:
            if (msg.readAtomString().equals("idling")) {
                session.getState().set(IMAPSessionState.IDLING);
                session.resetResponseList();
                // go back so the listener gets everything
                msg.reset();
                if (msg.isContinuation() && session.getConnectionListener() != null) {
                    // go back so the listener gets everything
                    msg.reset();
                    session.getCommandListener(session.getIdleTag()).onMessage(session, msg);
                }
            }
            break;
    	case IDLING:
            session.getCommandListener(session.getIdleTag()).onMessage(session, msg);
            break;
        case DONE_SENT:
            boolean idleDone = false;
            if (msg.isOK()) {
                if (msg.readAtomString().equals("IDLE") && msg.readAtomString().equals("terminated")) {
                    msg.reset();
                    session.getState().set(IMAPSessionState.CONNECTED);
                    session.getCommandListener(session.getIdleTag()).onResponse(session, session.getIdleTag(), Arrays.asList(msg));
                    idleDone = true;
                }

                if (!idleDone) {
                    // assume still in idle
                    session.getCommandListener(session.getIdleTag()).onMessage(session, msg);
                } else {
                    session.removeCommandListener(session.getIdleTag());
                    session.resetIdleTag();
                }
            }
            break;
    	case CONNECTED:
        default: // TODO remove??
            if (msg.isTagged()) {
                session.addResponse(msg);
                final IMAPCommandListener commandListener = session.removeCommandListener(msg.getTag());
                if (null != commandListener) {
                    commandListener.onResponse(session, msg.getTag(), session.getResponseList());
                    session.resetResponseList();
                }
            } else {
                if (msg.isContinuation()) {
                    if (null != session.getCurrentTag() && null != session.getCommandListener(session.getCurrentTag())) {
                        session.getCommandListener(session.getCurrentTag()).onMessage(session, msg);
                    } else {
                        session.getConnectionListener().onMessage(session, msg);
                    }
                } else {
                    session.addResponse(msg);
                }
            }
    	}
    }

}
