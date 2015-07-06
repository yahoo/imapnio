package org.apache.imapnioclient.client;

import java.util.List;

import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * Listener interface to be implemented by the IMAP client. Will call the client back: (1) When the server sends a OK response to a tag (2) On any
 * event because of IDLE
 *
 * @author kraman
 *
 */
public interface IMAPClientListener /* extends Future<IMAPResponse> */{
    public void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses);
    public void onDisconnect(final IMAPSession session);
}
