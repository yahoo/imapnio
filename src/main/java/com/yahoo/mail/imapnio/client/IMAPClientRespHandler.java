/**
 *
 */
package com.yahoo.mail.imapnio.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * @author kraman
 *
 */
public class IMAPClientRespHandler extends MessageToMessageDecoder<IMAPResponse> {

    private IMAPSession session;

    /** logger. */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IMAPClientRespHandler.class);

    /**
     * Creates a IMAPClient response handler. The last of the handlers in the chain before giving a callback to the client.
     *
     * @param session
     *            - IMAP session
     */
    public IMAPClientRespHandler(IMAPSession session) {
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
     */
    @Override
    public void decode(ChannelHandlerContext ctx, IMAPResponse msg, List<Object> out) throws Exception {
        log.info("<" + msg + " state:" + session.getState());
        if (session.getState() == IMAPSessionState.IDLE_REQUEST) {
            log.info("idle request state");
            if (msg.readAtomString().equals("idling")) {
                session.setState(IMAPSessionState.IDLING);
                log.info("idling now");
            }
        } else if (session.getState() == IMAPSessionState.IDLING) {
            IMAPClientListener listener = session.getClientListener();
            if (null != listener) {
                session.addResponse(msg);
                List<IMAPResponse> responses = new ArrayList<IMAPResponse>(session.getResponseList());
                session.resetResponseList();
                listener.onResponse(session, msg.getTag(), responses);
            }
        } else {
        	
			if (null != msg.getTag()) {
				IMAPClientListener listener = session.getClientListener();
				if (null != listener) {
					session.addResponse(msg);
					List<IMAPResponse> responses = new ArrayList<IMAPResponse>(
							session.getResponseList());
					session.resetResponseList();
					if (session.getState() == IMAPSessionState.OAUTH2_INIT) {
						if (msg.isOK()) {
							session.setState(IMAPSessionState.LoggedIn);
							listener.onOAuth2LoggedIn(session, responses);
						} else {
							session.setState(IMAPSessionState.LoginFailed);
							listener.onOAuth2LoggedIn(session, responses);
						}
					} else {
						listener.onResponse(session, msg.getTag(), responses);
					}
				}
			} else {
	        	if (session.getState() == IMAPSessionState.OAUTH2_INIT) {
					IMAPClientListener listener = session.getClientListener();
					if (null != listener) {
						listener.onDisconnect(session);
					}
	        	} else {

                // Pass along message without modifying it.
                out.add(msg);
                session.addResponse(msg);
	        	}
            }

        }
    }

    /**
     * Extract an IMAPResponse capabilities response into a Map of capabilities names to bools.
     *
     * @param capabilityResponse
     * @return a map all capabilities enabled on the server
     */
    private Map<String, Boolean> parseCapability(IMAPResponse capabilityResponse) {
        Map<String, Boolean> tmpCapability = new HashMap<String, Boolean>();

        String s;
        while ((s = capabilityResponse.readString(']')) != null) {
            if (s.length() == 0) {
                if (capabilityResponse.peekByte() == (byte) ']') {
                    break;
                }
                /*
                 * Probably found something here that's not an atom. Rather than loop forever or fail completely, we'll try to skip this bogus
                 * capabilities. This is known to happen with: Netscape Messaging Server 4.03 (built Apr 27 1999) that returns: * CAPABILITY *
                 * CAPABILITY IMAP4 IMAP4rev1 ... The "*" in the middle of the capabilities list causes us to loop forever here.
                 */
                capabilityResponse.skipToken();
            } else {
                if (!"capability".equals(s.toLowerCase())) {
                    tmpCapability.put(s.toLowerCase(), true);
                }
                if (s.regionMatches(true, 0, "AUTH=", 0, 5)) {
                    // authmechs.add(s.substring(5));
                }
            }
        }

        return tmpCapability;
    }

}
