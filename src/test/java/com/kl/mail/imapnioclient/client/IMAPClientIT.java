/**
 *
 */
package com.kl.mail.imapnioclient.client;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.SSLException;

import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.kl.mail.imapnioclient.client.IMAPClient;
import com.kl.mail.imapnioclient.client.IMAPSessionListener;
import com.kl.mail.imapnioclient.client.IMAPSession;
import com.kl.mail.imapnioclient.exception.IMAPSessionException;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * @author kraman
 *
 */
public class IMAPClientIT {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IMAPClientIT.class);
    
    
    class GenericListener implements IMAPSessionListener {
    	
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
    
    
    class CapabilityListener implements IMAPSessionListener {
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
    
    class ListenerToSendIdle implements IMAPSessionListener {

    	private IMAPSessionListener nextListener;
    	public ListenerToSendIdle(IMAPSessionListener l) {
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
    
    class ListenerToSendLogout implements IMAPSessionListener {
    	
    	private IMAPSessionListener nextListener;
    	private final IMAPSession session;
    	public ListenerToSendLogout(final IMAPSession session, IMAPSessionListener l) {
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
    
    
    class ListenerToSendStatus implements IMAPSessionListener {
    	
    	private IMAPSessionListener nextListener;
    	public ListenerToSendStatus(IMAPSessionListener l) {
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
    
    class ListenerToSendSelect implements IMAPSessionListener {
    	
    	private IMAPSessionListener nextListener;
    	public ListenerToSendSelect(IMAPSessionListener l) {
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
    public void testMultipleSessions() throws IMAPSessionException, URISyntaxException, InterruptedException {
    	for (int i=0; i<5; i++) {
    		testGmailPlainLoginWithIdle();
    	}
		Thread.sleep(86400000);
    }

    @Test
	public void testGmailPlainLoginWithIdle() throws IMAPSessionException,
			URISyntaxException, InterruptedException {
    	final String gmailServer = "imaps://imap.gmail.com:993";
    	// final String gmailServer = "imap://localhost:9993";
    	
		final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI(gmailServer), new GenericListener("SESS"));

		ChannelFuture loginFuture = session.executeLoginCommand("t1",
				"krinteg1@gmail.com", "1Testuser", new ListenerToSendStatus(new ListenerToSendSelect(new ListenerToSendIdle(new GenericListener("IDLING ")))));
	Thread.sleep(86400000);

	}
    

    
    
    @Test
    public void testGamailCapability () throws IMAPSessionException, URISyntaxException, InterruptedException {
        final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imaps://imap.gmail.com:993"), new CapabilityListener());
 
        ChannelFuture loginFuture = session.executeCapabilityCommand("t1-cap", new CapabilityListener());
        loginFuture.awaitUninterruptibly();
        Thread.sleep(300000);

    }

    @Test
    public void testGmailPlainLoginWithStatus() throws IMAPSessionException, URISyntaxException, InterruptedException {
    	final ListenerToSendStatus l = new ListenerToSendStatus(new GenericListener("STATUS "));
        final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imaps://imap.gmail.com:993"), l);
 
        ChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", l);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(300000);

    }
    
    @Test
    public void testGmailOauth2Login() throws URISyntaxException, IMAPSessionException, InterruptedException {
        final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imaps://imap.gmail.com:993"), new GenericListener());
         final String oauth2Tok = "dXNlcj1rcmludGVnMUBnbWFpbC5jb20BYXV0aD1CZWFyZXIgeWEyOS5xZ0VyWVM0TDBTbnFuTTlYSnVxX1ZwN19kWGVFTXg2dnRzcGNZMGpjeERHWlN1bDRLc2JoRmRwdTJmVlhua2l1Z1dmdjN5aTNqdmU4UEEBAQ==+";
        ChannelFuture loginFuture = session.executeOAuth2Command("t1", oauth2Tok, new ListenerToSendIdle(new GenericListener("IDLE ")));
        loginFuture.awaitUninterruptibly();
        Thread.sleep(400000000);

    }
}
