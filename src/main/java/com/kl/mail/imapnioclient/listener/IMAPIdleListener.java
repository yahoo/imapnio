package com.kl.mail.imapnioclient.listener;

import java.util.List;

import com.kl.mail.imapnioclient.client.IMAPSession;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * Listener to get messages when the session is idling.
 * @author kraman
 *
 */
public interface IMAPIdleListener extends IMAPClientListener {

	
}
