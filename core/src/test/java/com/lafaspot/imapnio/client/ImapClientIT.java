/**
 *
 */
package com.lafaspot.imapnio.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.channel.IMAPChannelFuture;
import com.lafaspot.imapnio.exception.IMAPSessionException;
import com.lafaspot.imapnio.listener.SessionListener;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * @author kraman
 *
 */
public class ImapClientIT {

    /** the class level logger. */
    private final org.slf4j.Logger log = LoggerFactory.getLogger(ImapClientIT.class);

    /**
     * Generic listener.
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
         * @param p prefix string
         */
        public GenericListener(final String p) {
            logPrefix = p;
        }

        /**
         * Got a response.
         * @param session IMAP session
         * @param tag IMAP tag
         * @param responses response
         */
        public void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses) {
            for (IMAPResponse r : responses) {
                log.info(logPrefix + "got rsp " + r);
            }

        }

        /**
         * disconnected.
         * @param session IMAP session
         */
        public void onDisconnect(final IMAPSession session) {
            log.error(logPrefix + " error disconnected");
        }

        /**
         * connected
         * @param session IMAP session
         */
        public void onConnect(final IMAPSession session) {
            log.error(logPrefix + " connected");
        }

        /**
         * message received.
         * @param session IMAP session
         * @param response message
         */
        public void onMessage(final IMAPSession session, final IMAPResponse response) {
            log.error(logPrefix + " got message " + response);

        }

    }


    /**
     * Listener to send CAPABILITY command.
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
         * @param session IMAP session
         * @param tag IMAP tag
         * @param responses messages
         */
        public void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses) {
            for (IMAPResponse r : responses) {
                log.info("cap... " + r);
            }

        }

        /**
         * disconnected.
         * @param session IMAP session
         */
        public void onDisconnect(final IMAPSession session) {
            log.error("cap listener: disconnected");
        }

        /**
         * connected.
         * @param session IMAP session
         */
        public void onConnect(final IMAPSession session) {
            log.error("cap listener: connected");
        }

        /**
         * message received.
         * @param session IMAP session
         * @param response message
         */
        public void onMessage(final IMAPSession session, final IMAPResponse response) {
            log.error("cap listener: onMessage " + response);
        }

    }

    /**
     * Listener sends IDLE command
     * @author kraman
     *
     */
    class ListenerToSendIdle implements SessionListener {
        /** listener to be used for idle */
        private SessionListener nextListener;

        /**
         * Constructs the listener.
         *
         * @param l next listener to use
         */
        public ListenerToSendIdle(final SessionListener l) {
            nextListener = l;
        }

        /**
         * Received a tagged message response.
         * @param session IMAP session
         * @param tag IMAP tag
         * @param responses messages
         */
        public void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses) {
            for (IMAPResponse r : responses) {
                log.info("LSI: msg " + r);
            }
            log.info("LSI: got a message sending idle " + tag);
            session.executeIdleCommand("t01-idle", nextListener);
        }

        /**
         * disconnected.
         * @param session IMAP session
         */
        public void onDisconnect(final IMAPSession session) {
            log.error("LSI: login error.disconnected");
        }

        /**
         * connected.
         * @param session IMAP session
         */
        public void onConnect(final IMAPSession session) {
            log.error("LSI: connected");
        }

        /**
         * untagged message.
         * @param session IMAP session
         * @param response message
         */
        public void onMessage(final IMAPSession session, final IMAPResponse response) {
            log.error("LSI: msg " + response);
        }

    }

    /**
     * Listener to send logout.
     * @author kraman
     *
     */
    class ListenerToSendLogout implements SessionListener {
        /** Listener for logout. */
        private SessionListener nextListener;
        /** IMAP session. */
        private final IMAPSession session;

        /**
         * Construct the listener to send logout.
         *
         * @param session IMAP session
         * @param l next listener
         */
        public ListenerToSendLogout(final IMAPSession session, final SessionListener l) {
            this.session = session;
            nextListener = l;
        }

        /**
         * recenved tagged response.
         * @param session IMAP session
         * @param tag IMAP tag
         * @param responses messages
         */
        public void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses) {
            session.executeLogoutCommand("t01-logout", nextListener);
        }

        /**
         * disconnected
         * @param session IMAP session
         */
        public void onDisconnect(final IMAPSession session) {
            log.error("ListenerToSendLogout error.disconnected");
        }

        /**
         * connected
         * @param session IMAP session
         */
        public void onConnect(final IMAPSession session) {
            log.error("connected");
        }

        /**
         * untagged message received
         * @param session IMAP session
         * @param response messge
         */
        public void onMessage(final IMAPSession session, final IMAPResponse response) {
            // TODO Auto-generated method stub

        }

    }


    /**
     * Listener to send STATUS command.
     * @author kraman
     *
     */
    class ListenerToSendStatus implements SessionListener {

        /** listener to be used in status command. */
        private SessionListener nextListener;

        /**
         * Constructs the listener.
         *
         * @param l next listener to use
         */
        public ListenerToSendStatus(final SessionListener l) {
            nextListener = l;
        }
    
        /**
         * received a tagged response.
         * @param session IMAP session
         * @param tag IMAP tag
         * @param responses messages
         */
        public void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses) {
            for (IMAPResponse r : responses) {
                log.info("SL: rsp " + r);
            }
            log.error("SL: sending status");
            session.executeStatusCommand("t01-status", "Inbox", new String[] { "UIDNEXT" }, nextListener);
        }

        /**
         * disconnected.
         * @param session IMAP session
         */
        public void onDisconnect(final IMAPSession session) {
            log.error("SL: login error.disconnected");
        }

        /**
         * connected.
         * @param session IMAP session
         */
        public void onConnect(final IMAPSession session) {
            log.error("SL: connected");
        }

        /**
         * received untagged response.
         * @param session IMAP session
         * @param response message
         */
        public void onMessage(final IMAPSession session, final IMAPResponse response) {
            log.error("SL: msg  " + response);
        }

    }

    /**
     * Listener to send SELECT command.
     * @author kraman
     *
     */
    class ListenerToSendSelect implements SessionListener {

        /** listener to be used for SELECT command. */
        private SessionListener nextListener;

        /**
         * Constructs the listener.
         *
         * @param l next listener
         */
        public ListenerToSendSelect(final SessionListener l) {
            nextListener = l;
        }

        /**
         * Received tagged message.
         * @param session IMAP session
         * @param tag IMAP tag
         * @param responses messages
         */
        public void onResponse(final IMAPSession session, final String tag, final List<IMAPResponse> responses) {
            session.executeSelectCommand("t01-sel", "Inbox", nextListener);
        }

        /**
         * disconnected.
         * @param session IMAP session
         */
        public void onDisconnect(final IMAPSession session) {
            log.error("SEL: login error.disconnected");
        }

        /**
         * connected.
         * @param session IMAP session
         */
        public void onConnect(final IMAPSession session) {
            log.error("SEL: connected");
        }

        /**
         * received message without tag.
         * @param session IMAP session
         * @param response message
         */
        public void onMessage(final IMAPSession session, final IMAPResponse response) {
            log.error("SEL: msg " + response);
        }

    }

    private IMAPClient theClient;

    /**
     * Setup.
     */
    @BeforeClass
    public void setup() {
    	final int threads = 5;
    	theClient = new IMAPClient(threads);
    }

    @Test
    public void testMultipleSessions() throws IMAPSessionException, URISyntaxException, InterruptedException {
    	
    	final String gmailServer = "imaps://imap.gmail.com:993";
    	final int maxSessions = 5;
    	final IMAPSession[] sessions = new IMAPSession[maxSessions];
    	for (int i = 0; i < maxSessions; i++) {
    	
        	// final String gmailServer = "imap://localhost:9993";
        	
    		 sessions[i] = theClient.createSession(new URI(gmailServer), new GenericListener("SESS"));
    		 sessions[i].connect();

    		 IMAPChannelFuture loginFuture = sessions[i].executeLoginCommand("t1",
    				"krinteg1@gmail.com", "1Testuser",
    				new ListenerToSendStatus(new ListenerToSendSelect(new ListenerToSendIdle(
    				        new GenericListener("IDLING ")))));

    	}
		Thread.sleep(10000);
		
		for (IMAPSession s:sessions) {
			s.disconnect();
		}
    }

    @Test
	public void testGmailPlainLoginWithIdle() throws IMAPSessionException,
			URISyntaxException, InterruptedException {
    	final String gmailServer = "imaps://imap.gmail.com:993";
    	// final String gmailServer = "imap://localhost:9993";
    	
		final IMAPSession session = theClient.createSession(new URI(gmailServer), new GenericListener("SESS"));
		session.connect();

		IMAPChannelFuture loginFuture = session.executeLoginCommand("t1",
				"krinteg1@gmail.com", "1Testuser", new ListenerToSendStatus(
				        new ListenerToSendSelect(new ListenerToSendIdle(new GenericListener("IDLING ")))));
	Thread.sleep(30000);

	}




    @Test
    public void testGamailCapability() throws IMAPSessionException, URISyntaxException, InterruptedException {
        final IMAPSession session = theClient.createSession(new URI("imaps://imap.gmail.com:993"), new CapabilityListener());
        session.connect();
        IMAPChannelFuture loginFuture = session.executeCapabilityCommand("t1-cap", new CapabilityListener());
        loginFuture.awaitUninterruptibly();
        Thread.sleep(300000);

    }

    @Test
    public void testGmailPlainLoginWithStatus() throws IMAPSessionException, URISyntaxException, InterruptedException {
    	final ListenerToSendStatus l = new ListenerToSendStatus(new GenericListener("STATUS "));
        final IMAPSession session = theClient.createSession(new URI("imaps://imap.gmail.com:993"), l);
        session.connect();
    
        IMAPChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", l);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(300000);

    }

    @Test
    public void testGmailOauth2Login() throws URISyntaxException, IMAPSessionException, InterruptedException {
        final IMAPSession session = theClient.createSession(new URI("imaps://imap.gmail.com:993"), new GenericListener());
        session.connect();
         final String oauth2Tok =
                 "dXNlcj1rcmludGVnMUBnbWFpbC5jb20BYXV0aD1CZWFyZXIgeWEyOS5zQUVTb3hfblN5QjA0eEljZHNTUF9tbFZGN096dHN6WDJsa19FMXVwLUw3UGRiSG9BR2l2WG1nSWQ4Q0x2a0RLUnFEUgEB";
         IMAPChannelFuture loginFuture = session.executeOAuth2Command("t1", oauth2Tok, new ListenerToSendIdle(new GenericListener("IDLE ")));
        loginFuture.awaitUninterruptibly();
        Thread.sleep(400000000);

    }

	@Test
	public void testGmailSASLOauth2Login() throws URISyntaxException, IMAPSessionException, InterruptedException {
	    final IMAPSession session = theClient.createSession(new URI("imaps://imap.gmail.com:993"), new GenericListener());
	    session.connect();
	     final String oauth2Tok = "ya29.sAESox_nSyB04xIcdsSP_mlVF7OztszX2lk_E1up-L7PdbHoAGivXmgId8CLvkDKRqDR";
	     IMAPChannelFuture loginFuture = session.executeSASLXOAuth2("t1", "krinteg1@gmail.com",
	            oauth2Tok, new ListenerToSendIdle(new GenericListener("IDLE ")));
	    loginFuture.awaitUninterruptibly();
	    Thread.sleep(400000000);
	
	}
}
