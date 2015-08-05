/**
 *
 */
package com.lafaspot.imapnio.client;

import java.util.Arrays;
import java.util.List;

import com.lafaspot.imapnio.exception.IMAPSessionException;
import com.lafaspot.imapnio.listener.SessionListener;
import com.sun.mail.imap.protocol.IMAPResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

/**
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
     * @param ctx
     *            - channel context
     * @param msg
     *            - incoming IMAP message
     * @param out
     *            - message to be sent to the next handler in the chain
     * @throws Exception
     *             FIXME: why are we throwing exception -- lafa
     */
    @Override
    public void decode(final ChannelHandlerContext ctx, final IMAPResponse msg, final List<Object> out) throws Exception {
        // log.debug("< " + msg + " state:" + session.getState());
        if (session.getState() == IMAPSessionState.ConnectRequest) {
            if (msg.isOK()) {
                session.setState(IMAPSessionState.Connected);
                session.resetResponseList();
                if (null != session.getSessionListener()) {
                    ((SessionListener) session.getSessionListener()).onConnect(session);
                }
            } else {
                throw new IMAPSessionException("connect failed");
            }
        } else if (session.getState() == IMAPSessionState.IDLE_REQUEST) {
            if (msg.readAtomString().equals("idling")) {
                session.setState(IMAPSessionState.IDLING);
                session.resetResponseList();
                // go back and see what that is.
                msg.reset();
                if (msg.readByte() == '+' && session.getSessionListener() != null) {
                    msg.reset();
                    session.getSessionListener().onMessage(session, msg);
                }
            }
        } else if (session.getState() == IMAPSessionState.IDLING) {
            session.getClientListener(session.getIdleTag()).onResponse(session, session.getIdleTag(), Arrays.asList(msg));
        } else {
            if (null != msg.getTag()) {
                session.addResponse(msg);
                final SessionListener listener = session.removeClientListener(msg.getTag());
                if (null != listener) {
                    listener.onResponse(session, msg.getTag(), session.getResponseList());
                    session.resetResponseList();
                }
            } else {
                msg.reset();
                if (msg.readByte() == '+' && session.getSessionListener() != null) {
                    msg.reset();
                    session.getSessionListener().onMessage(session, msg);
                } else {
                    session.addResponse(msg);
                    // Pass along message without modifying it.
                    // out.add(msg);
                }
            }
        }
    }

}
