package com.yahoo.mail.imapnio.client;

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
    public void onResponse(String tag, List<IMAPResponse> responses);

    public void onIdleEvent(List<IMAPResponse> messages);

    public void onOAuth2LoggedIn(List<IMAPResponse> msgs);
}
