package com.yahoo.imapnio.async.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * This class is the response for the {@link ImapAsyncClient} createSession method response.
 */
public class ImapAsyncCreateSessionResponse {
    /** An instance of ImapAsyncSession object created through createSession method in {@link ImapAsyncClient}. */
    private ImapAsyncSession session;

    /** IMAP server greeting responses upon connection established. */
    private IMAPResponse greeting;

    /**
     * Instantiates a {@link ImapAsyncCreateSessionResponse} instance with ImapAsyncSession object and server initial greeting. Based on RFC3501,
     * server initial greeting is an un-tagged response.
     * 
     * @param session ImapAsyncSession instance that is created by createSession method in {@link ImapAsyncClient}
     * @param greeting the initial server one line greeting including OK
     */
    public ImapAsyncCreateSessionResponse(@Nonnull final ImapAsyncSession session, @Nonnull final IMAPResponse greeting) {
        this.session = session;
        this.greeting = greeting;
    }

    /**
     * @return ImapAsyncSession instance
     */
    public ImapAsyncSession getSession() {
        return session;
    }

    /**
     * @return server initial greeting sent immediately after connection is established
     */
    @Nullable
    public IMAPResponse getServerGreeting() {
        return greeting;
    }

}
