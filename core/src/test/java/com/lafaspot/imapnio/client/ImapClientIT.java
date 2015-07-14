/**
 *
 */
package com.lafaspot.imapnio.client;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.SSLException;

import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.client.IMAPClient;
import com.lafaspot.imapnio.client.IMAPSession;
import com.lafaspot.imapnio.exception.ImapSessionException;
import com.lafaspot.imapnio.listener.SessionListener;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * @author kraman
 *
 */
public class ImapClientIT {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ImapClientIT.class);
    
    
    class GenericListener implements SessionListener {
    	
    	String logPrefix = "";
    	public GenericListener() {
    	}
    	
    	public GenericListener(String p) {
    		logPrefix = p;
    	}
		public void onResponse(IMAPSession session, String tag,
				List<IMAPResponse> responses) {
			for (IMAPResponse r:responses) {
				log.info (logPrefix + "got rsp " + r);
			}
			
		}

		public void onDisconnect(IMAPSession session) {
			log.error(logPrefix +" error disconnected");
		}
		public void onConnect(IMAPSession session) {
			log.error(logPrefix +" connected");
		}

		public void onMessage(IMAPSession session, IMAPResponse response) {
			log.error(logPrefix +" got message " + response);
			
		}
    	
    }
    
    
    class CapabilityListener implements SessionListener {
    	public CapabilityListener() {
    	}
		public void onResponse(IMAPSession session, String tag,
				List<IMAPResponse> responses) {
			for (IMAPResponse r:responses) {
				log.info ("cap... " + r);
			}
			
		}

		public void onDisconnect(IMAPSession session) {
			log.error("cap listener: disconnected");
		}
		public void onConnect(IMAPSession session) {
			log.error("cap listener: connected");
		}
		public void onMessage(IMAPSession session, IMAPResponse response) {
			log.error("cap listener: onMessage " + response);
		}
    	
    }
    
    class ListenerToSendIdle implements SessionListener {

    	private SessionListener nextListener;
    	public ListenerToSendIdle(SessionListener l) {
    			nextListener = l;
    	}

		public void onResponse(final IMAPSession session, String tag,
				List<IMAPResponse> responses) {
			for (IMAPResponse r:responses) {
				log.info("LSI: msg " + r);
			}
			log.info("LSI: got a message sending idle " + tag);
			session.executeIdleCommand("t01-idle", nextListener);
		}

		public void onDisconnect(final IMAPSession session) {
			log.error("LSI: login error.disconnected");
		}

		public void onConnect(IMAPSession session) {
			log.error("LSI: connected");
		}

		public void onMessage(IMAPSession session, IMAPResponse response) {
			log.error("LSI: msg " + response);
		}
    	
    }
    
    class ListenerToSendLogout implements SessionListener {
    	
    	private SessionListener nextListener;
    	private final IMAPSession session;
    	public ListenerToSendLogout(final IMAPSession session, SessionListener l) {
    		this.session = session;
    			nextListener = l;
    	}

		public void onResponse(final IMAPSession session, String tag,
				List<IMAPResponse> responses) {
			session.executeLogoutCommand("t01-logout", nextListener);
		}

		public void onDisconnect(final IMAPSession session) {
			log.error("ListenerToSendLogout error.disconnected");
		}

		public void onConnect(IMAPSession session) {
			log.error("connected");
		}

		public void onMessage(IMAPSession session, IMAPResponse response) {
			// TODO Auto-generated method stub
			
		}
    	
    } 
    
    
    class ListenerToSendStatus implements SessionListener {
    	
    	private SessionListener nextListener;
    	public ListenerToSendStatus(SessionListener l) {
    			nextListener = l;
    	}

		public void onResponse(final IMAPSession session, String tag,
				List<IMAPResponse> responses) {
			for (IMAPResponse r:responses) {
				log.info("SL: rsp " + r);
			}
			log.error("SL: sending status");
			session.executeStatusCommand("t01-status", "Inbox", new String[] {"UIDNEXT"}, nextListener);
		}

		public void onDisconnect(final IMAPSession session) {
			log.error("SL: login error.disconnected");
		}

		public void onConnect(IMAPSession session) {
			log.error("SL: connected");
		}

		public void onMessage(IMAPSession session, IMAPResponse response) {
			log.error("SL: msg  " + response);
		}
    	
    }
    
    class ListenerToSendSelect implements SessionListener {
    	
    	private SessionListener nextListener;
    	public ListenerToSendSelect(SessionListener l) {
    			nextListener = l;
    	}

		public void onResponse(final IMAPSession session, String tag,
				List<IMAPResponse> responses) {
			session.executeSelectCommand("t01-sel", "Inbox", nextListener);
		}

		public void onDisconnect(final IMAPSession session) {
			log.error("SEL: login error.disconnected");
		}

		public void onConnect(IMAPSession session) {
			log.error("SEL: connected");
		}

		public void onMessage(IMAPSession session, IMAPResponse response) {
			log.error("SEL: msg " + response);
		}
    	
    }
    
    @Test
    public void testMultipleSessions() throws ImapSessionException, URISyntaxException, InterruptedException {
    	
    	final String gmailServer = "imaps://imap.gmail.com:993";
    	final int maxSessions = 5;
    	final IMAPSession sessions[] = new IMAPSession[maxSessions];
    	for (int i=0; i<maxSessions; i++) {
    	   
        	// final String gmailServer = "imap://localhost:9993";
        	
    		 sessions[i]= IMAPClient.INSTANCE.createSession(new URI(gmailServer), new GenericListener("SESS"));

    		ChannelFuture loginFuture = sessions[i].executeLoginCommand("t1",
    				"krinteg1@gmail.com", "1Testuser", new ListenerToSendStatus(new ListenerToSendSelect(new ListenerToSendIdle(new GenericListener("IDLING ")))));

    	}
		Thread.sleep(10000);
		
		for (IMAPSession s:sessions) {
			s.disconnect();
		}
    }

    @Test
	public void testGmailPlainLoginWithIdle() throws ImapSessionException,
			URISyntaxException, InterruptedException {
    	final String gmailServer = "imaps://imap.gmail.com:993";
    	// final String gmailServer = "imap://localhost:9993";
    	
		final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI(gmailServer), new GenericListener("SESS"));

		ChannelFuture loginFuture = session.executeLoginCommand("t1",
				"krinteg1@gmail.com", "1Testuser", new ListenerToSendStatus(new ListenerToSendSelect(new ListenerToSendIdle(new GenericListener("IDLING ")))));
	Thread.sleep(86400000);

	}
    

    
    
    @Test
    public void testGamailCapability () throws ImapSessionException, URISyntaxException, InterruptedException {
        final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imaps://imap.gmail.com:993"), new CapabilityListener());
 
        ChannelFuture loginFuture = session.executeCapabilityCommand("t1-cap", new CapabilityListener());
        loginFuture.awaitUninterruptibly();
        Thread.sleep(300000);

    }

    @Test
    public void testGmailPlainLoginWithStatus() throws ImapSessionException, URISyntaxException, InterruptedException {
    	final ListenerToSendStatus l = new ListenerToSendStatus(new GenericListener("STATUS "));
        final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imaps://imap.gmail.com:993"), l);
 
        ChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", l);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(300000);

    }
    
    @Test
    public void testGmailOauth2Login() throws URISyntaxException, ImapSessionException, InterruptedException {
        final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imaps://imap.gmail.com:993"), new GenericListener());
         final String oauth2Tok = "dXNlcj1rcmludGVnMUBnbWFpbC5jb20BYXV0aD1CZWFyZXIgeWEyOS5zQUVTb3hfblN5QjA0eEljZHNTUF9tbFZGN096dHN6WDJsa19FMXVwLUw3UGRiSG9BR2l2WG1nSWQ4Q0x2a0RLUnFEUgEB";
        ChannelFuture loginFuture = session.executeOAuth2Command("t1", oauth2Tok, new ListenerToSendIdle(new GenericListener("IDLE ")));
        loginFuture.awaitUninterruptibly();
        Thread.sleep(400000000);

    }

	@Test
	public void testGmailSASLOauth2Login() throws URISyntaxException, ImapSessionException, InterruptedException {
	    final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imaps://imap.gmail.com:993"), new GenericListener());
	     final String oauth2Tok = "ya29.sAESox_nSyB04xIcdsSP_mlVF7OztszX2lk_E1up-L7PdbHoAGivXmgId8CLvkDKRqDR";
	    ChannelFuture loginFuture = session.executeSASLXOAuth2("t1", "krinteg1@gmail.com", oauth2Tok, new ListenerToSendIdle(new GenericListener("IDLE ")));
	    loginFuture.awaitUninterruptibly();
	    Thread.sleep(400000000);
	
	}
}
