package com.kl.mail.imapnioclient.client;

import java.util.List;

import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * Listener interface to be implemented by the IMAP client. Will call the client back: (1) When the server sends a OK response to a tag (2) On any
 * event because of IDLE
 *
 * @author kraman
 *
 */
public interface IMAPSessionListener{
	/**
	 * Will be called when the session/socket is connected.
	 * @param session IMAPSession
	 */
	void onConnect(final IMAPSession session);
	/**
	 * Will be called when there is a tagged response from the remote server.
	 * @param session IMAPSession
	 * @param tag IMAP tag being used
	 * @param responses IMAPResponse
	 */
    void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses);
    
    /**
     * Will be called when an untagged message is received from remote server.
     * @param session IMAPSession
     * @param responses IMAPResponse
     */
    void onMessage(final IMAPSession session, final IMAPResponse response);
    
    /**
     * Will be called when the session/socket is disconnected.
     * @param session IMAPSession
     */
    void onDisconnect(final IMAPSession session);
}
