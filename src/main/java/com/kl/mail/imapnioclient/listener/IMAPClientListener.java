/**
 * 
 */
package com.kl.mail.imapnioclient.listener;

import java.util.List;

import com.kl.mail.imapnioclient.client.IMAPSession;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * @author kraman
 *
 */
public interface IMAPClientListener {
	
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
     * @param response IMAPResponse
     */
    void onMessage(final IMAPSession session, final IMAPResponse response);
}
