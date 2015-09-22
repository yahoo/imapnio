/**
 *
 */
package com.lafaspot.imapnio.client;

import java.net.URI;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.channel.IMAPChannelFuture;
import com.lafaspot.imapnio.listener.SessionListener;
import com.lafaspot.logfast.logging.LogContext;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger;
import com.lafaspot.logfast.logging.Logger.Level;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * @author kraman
 *
 */
public class ImapClientIT {

    /**
     * Generic listener.
     *
     * @author kraman
     *
     */
    class GenericListener implements SessionListener {
        /** log prefix string. */
        private String logPrefix = "";

        /**
         * Constructs a Generic listener.
         */
        public GenericListener() {
        }

        /**
         * Constructs a Generic listener.
         *
         * @param p
         *            prefix string
         */
        public GenericListener(final String p) {
            logPrefix = p;
        }

        /**
         * Got a response.
         *
         * @param session
         *            IMAP session
         * @param tag
         *            IMAP tag
         * @param responses
         *            response
         */
        @Override
        public void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses) {
            for (final IMAPResponse r : responses) {
                log.debug(logPrefix + "got rsp " + r, null);
            }

        }

        /**
         * disconnected.
         *
         * @param session
         *            IMAP session
         */
        @Override
        public void onDisconnect(final IMAPSession session) {
            log.error(logPrefix + " error disconnected", null);
        }

        /**
         * connected
         *
         * @param session
         *            IMAP session
         */
        @Override
        public void onConnect(final IMAPSession session) {
            log.error(logPrefix + " connected", null);
        }

        /**
         * message received.
         *
         * @param session
         *            IMAP session
         * @param response
         *            message
         */
        @Override
        public void onMessage(final IMAPSession session, final IMAPResponse response) {
            log.error(logPrefix + " got message " + response, null);

        }

    }

    /**
     * Listener to send CAPABILITY command.
     *
     * @author kraman
     *
     */
    class CapabilityListener implements SessionListener {
        /**
         * Constructs the listener.
         */
        public CapabilityListener() {
        }

        /**
         * tagged message received.
         *
         * @param session
         *            IMAP session
         * @param tag
         *            IMAP tag
         * @param responses
         *            messages
         */
        @Override
        public void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses) {
            for (final IMAPResponse r : responses) {
                log.info("cap... " + r, null);
            }

        }

        /**
         * disconnected.
         *
         * @param session
         *            IMAP session
         */
        @Override
        public void onDisconnect(final IMAPSession session) {
            log.error("cap listener: disconnected", null);
        }

        /**
         * connected.
         *
         * @param session
         *            IMAP session
         */
        @Override
        public void onConnect(final IMAPSession session) {
            log.error("cap listener: connected", null);
        }

        /**
         * message received.
         *
         * @param session
         *            IMAP session
         * @param response
         *            message
         */
        @Override
        public void onMessage(final IMAPSession session, final IMAPResponse response) {
            log.error("cap listener: onMessage " + response, null);
        }

    }

    /**
     * Listener sends IDLE command
     *
     * @author kraman
     *
     */
    class ListenerToSendIdle implements SessionListener {
        /** listener to be used for idle */
        private final SessionListener nextListener;

        /**
         * Constructs the listener.
         *
         * @param l
         *            next listener to use
         */
        public ListenerToSendIdle(final SessionListener l) {
            nextListener = l;
        }

        /**
         * Received a tagged message response.
         *
         * @param session
         *            IMAP session
         * @param tag
         *            IMAP tag
         * @param responses
         *            messages
         */
        @Override
        public void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses) {
            for (final IMAPResponse r : responses) {
                log.info("LSI: msg " + r, null);
            }
            log.info("LSI: got a message sending idle " + tag, null);
            session.executeIdleCommand("t01-idle", nextListener);
        }

        /**
         * disconnected.
         *
         * @param session
         *            IMAP session
         */
        @Override
        public void onDisconnect(final IMAPSession session) {
            log.error("LSI: login error.disconnected", null);
        }

        /**
         * connected.
         *
         * @param session
         *            IMAP session
         */
        @Override
        public void onConnect(final IMAPSession session) {
            log.error("LSI: connected", null);
        }

        /**
         * untagged message.
         *
         * @param session
         *            IMAP session
         * @param response
         *            message
         */
        @Override
        public void onMessage(final IMAPSession session, final IMAPResponse response) {
            log.error("LSI: msg " + response, null);
        }

    }

    /**
     * Listener to send logout.
     *
     * @author kraman
     *
     */
    class ListenerToSendLogout implements SessionListener {
        /** Listener for logout. */
        private final SessionListener nextListener;

        /**
         * Construct the listener to send logout.
         *
         * @param session
         *            IMAP session
         * @param l
         *            next listener
         */
        public ListenerToSendLogout(final IMAPSession session, final SessionListener l) {
            nextListener = l;
        }

        /**
         * recenved tagged response.
         *
         * @param session
         *            IMAP session
         * @param tag
         *            IMAP tag
         * @param responses
         *            messages
         */
        @Override
        public void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses) {
            session.executeLogoutCommand("t01-logout", nextListener);
        }

        /**
         * disconnected
         *
         * @param session
         *            IMAP session
         */
        @Override
        public void onDisconnect(final IMAPSession session) {
            log.error("ListenerToSendLogout error.disconnected", null);
        }

        /**
         * connected
         *
         * @param session
         *            IMAP session
         */
        @Override
        public void onConnect(final IMAPSession session) {
            log.error("connected", null);
        }

        /**
         * untagged message received
         *
         * @param session
         *            IMAP session
         * @param response
         *            messge
         */
        @Override
        public void onMessage(final IMAPSession session, final IMAPResponse response) {
            // TODO Auto-generated method stub

        }

    }

    /**
     * Listener to send STATUS command.
     *
     * @author kraman
     *
     */
    class ListenerToSendStatus implements SessionListener {

        /** listener to be used in status command. */
        private final SessionListener nextListener;

        /**
         * Constructs the listener.
         *
         * @param l
         *            next listener to use
         */
        public ListenerToSendStatus(final SessionListener l) {
            nextListener = l;
        }

        /**
         * received a tagged response.
         *
         * @param session
         *            IMAP session
         * @param tag
         *            IMAP tag
         * @param responses
         *            messages
         */
        @Override
        public void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses) {
            for (final IMAPResponse r : responses) {
                log.info("SL: rsp " + r, null);
            }
            log.error("SL: sending status", null);
            session.executeStatusCommand("t01-status", "Inbox", new String[] { "UIDNEXT" }, nextListener);
        }

        /**
         * disconnected.
         *
         * @param session
         *            IMAP session
         */
        @Override
        public void onDisconnect(final IMAPSession session) {
            log.error("SL: login error.disconnected", null);
        }

        /**
         * connected.
         *
         * @param session
         *            IMAP session
         */
        @Override
        public void onConnect(final IMAPSession session) {
            log.info("SL: connected", null);
        }

        /**
         * received untagged response.
         *
         * @param session
         *            IMAP session
         * @param response
         *            message
         */
        @Override
        public void onMessage(final IMAPSession session, final IMAPResponse response) {
            log.error("SL: msg  " + response, null);
        }

    }

    /**
     * Listener to send SELECT command.
     *
     * @author kraman
     *
     */
    class ListenerToSendSelect implements SessionListener {

        /** listener to be used for SELECT command. */
        private final SessionListener nextListener;

        /**
         * Constructs the listener.
         *
         * @param l
         *            next listener
         */
        public ListenerToSendSelect(final SessionListener l) {
            nextListener = l;
        }

        /**
         * Received tagged message.
         *
         * @param session
         *            IMAP session
         * @param tag
         *            IMAP tag
         * @param responses
         *            messages
         */
        @Override
        public void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses) {
            session.executeSelectCommand("t01-sel", "Inbox", nextListener);
        }

        /**
         * disconnected.
         *
         * @param session
         *            IMAP session
         */
        @Override
        public void onDisconnect(final IMAPSession session) {
            log.error("SEL: login error.disconnected", null);
        }

        /**
         * connected.
         *
         * @param session
         *            IMAP session
         */
        @Override
        public void onConnect(final IMAPSession session) {
            log.error("SEL: connected", null);
        }

        /**
         * received message without tag.
         *
         * @param session
         *            IMAP session
         * @param response
         *            message
         */
        @Override
        public void onMessage(final IMAPSession session, final IMAPResponse response) {
            log.error("SEL: msg " + response, null);
        }

    }

    /**
	 * Listener to send STATUS command.
	 *
	 * @author kraman
	 *
	 */
	class ListenerToSendList implements SessionListener {

	    /** listener to be used in status command. */
	    private final SessionListener nextListener;

	    /**
	     * Constructs the listener.
	     *
	     * @param l
	     *            next listener to use
	     */
	    public ListenerToSendList(final SessionListener l) {
	        nextListener = l;
	    }

	    /**
	     * received a tagged response.
	     *
	     * @param session
	     *            IMAP session
	     * @param tag
	     *            IMAP tag
	     * @param responses
	     *            messages
	     */
	    @Override
	    public void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses) {
	        for (final IMAPResponse r : responses) {
	            log.info("SL: rsp " + r, null);
	        }
	        log.error("SL: sending list", null);
	        session.executeListCommand("t0-list", "", "*", nextListener);
	    }

	    /**
	     * disconnected.
	     *
	     * @param session
	     *            IMAP session
	     */
	    @Override
	    public void onDisconnect(final IMAPSession session) {
	        log.error("SL: login error.disconnected", null);
	    }

	    /**
	     * connected.
	     *
	     * @param session
	     *            IMAP session
	     */
	    @Override
	    public void onConnect(final IMAPSession session) {
	        log.info("SL: connected", null);
	    }

	    /**
	     * received untagged response.
	     *
	     * @param session
	     *            IMAP session
	     * @param response
	     *            message
	     */
	    @Override
	    public void onMessage(final IMAPSession session, final IMAPResponse response) {
	        log.error("SL: msg  " + response, null);
	    }

	}

	private IMAPClient theClient;
    private LogManager logManager;
    private Logger log;

    /**
     * Setup.
     */
    @BeforeClass
    public void setup() {
        final int threads = 5;
        theClient = new IMAPClient(threads);
        logManager = new LogManager(Level.DEBUG, 5);
        logManager.setLegacy(true);
        log = logManager.getLogger(new LogContext("ImapClientIT") {
        });
    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testSMultipleSessions() throws Exception {

        final String gmailServer = "imaps://imap.gmail.com:993";
        final int maxSessions = 5;
        final IMAPSession[] sessions = new IMAPSession[maxSessions];
        for (int i = 0; i < maxSessions; i++) {

            // final String gmailServer = "imap://localhost:9993";

            sessions[i] = theClient.createSession(new URI(gmailServer), new GenericListener("SESS"), logManager);
            sessions[i].connect();

            sessions[i].executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser",
                            new ListenerToSendStatus(new ListenerToSendSelect(new ListenerToSendIdle(new GenericListener("IDLING ")))));

        }
        Thread.sleep(10000);

        for (final IMAPSession s : sessions) {
            s.disconnect();
        }
    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testGmailPlainLoginWithIdle() throws Exception {
        final String gmailServer = "imaps://imap.gmail.com:993";
        // final String gmailServer = "imap://localhost:9993";

        final IMAPSession session = theClient.createSession(new URI(gmailServer), new GenericListener("SESS"), logManager);
        session.connect();

        session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser",
                        new ListenerToSendStatus(new ListenerToSendSelect(new ListenerToSendIdle(new GenericListener("IDLING ")))));
        Thread.sleep(30000);

    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testGamailCapability() throws Exception {
        final IMAPSession session = theClient.createSession(new URI("imaps://imap.gmail.com:993"), new CapabilityListener(), logManager);
        session.connect();
        final IMAPChannelFuture loginFuture = session.executeCapabilityCommand("t1-cap", new CapabilityListener());
        loginFuture.awaitUninterruptibly();
        Thread.sleep(300000);

    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testGmailPlainLoginWithStatus() throws Exception {
        final ListenerToSendStatus l = new ListenerToSendStatus(new GenericListener("STATUS "));
        final IMAPSession session = theClient.createSession(new URI("imaps://imap.gmail.com:993"), l, logManager);
        session.connect();

        final IMAPChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", l);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(300000);

    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testGmailPlainLoginWithList() throws Exception {
        final ListenerToSendList l = new ListenerToSendList(new GenericListener("LIST "));
        final IMAPSession session = theClient.createSession(new URI("imaps://imap.gmail.com:993"), new GenericListener("LIST"), logManager);
        session.connect();

        final IMAPChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", l);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(300000);

    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testGmailOauth2Login() throws Exception {
        final IMAPSession session = theClient.createSession(new URI("imaps://imap.gmail.com:993"), new GenericListener(), logManager);
        session.connect();
        @SuppressWarnings("checkstyle:linelength")
        final String oauth2Tok = "dXNlcj1rcmludGVnMUBnbWFpbC5jb20BYXV0aD1CZWFyZXIgeWEyOS5zQUVTb3hfblN5QjA0eEljZHNTUF9tbFZGN096dHN6WDJsa19FMXVwLUw3UGRiSG9BR2l2WG1nSWQ4Q0x2a0RLUnFEUgEB";
        final IMAPChannelFuture loginFuture = session.executeOAuth2Command("t1", oauth2Tok, new ListenerToSendIdle(new GenericListener("IDLE ")));
        loginFuture.awaitUninterruptibly();
        Thread.sleep(400000000);

    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testGmailSASLOauth2Login() throws Exception {
        final IMAPSession session = theClient.createSession(new URI("imaps://imap.gmail.com:993"), new GenericListener(), logManager);
        session.connect();
        final String oauth2Tok = "ya29.sAESox_nSyB04xIcdsSP_mlVF7OztszX2lk_E1up-L7PdbHoAGivXmgId8CLvkDKRqDR";
        final IMAPChannelFuture loginFuture = session.executeSASLXOAuth2("t1", "krinteg1@gmail.com", oauth2Tok,
                        new ListenerToSendIdle(new GenericListener("IDLE ")));
        loginFuture.awaitUninterruptibly();
        Thread.sleep(400000000);

    }
}
