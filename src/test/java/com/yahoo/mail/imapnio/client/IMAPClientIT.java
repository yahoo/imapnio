/**
 *
 */
package com.yahoo.mail.imapnio.client;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.SSLException;

import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.mail.imapnio.client.exception.IMAPSessionException;

/**
 * @author kraman
 *
 */
public class IMAPClientIT {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IMAPClientIT.class);
    
    
    class IdleListener implements IMAPClientListener {

    	private IMAPClientListener nextListener;
    	private final IMAPSession session;
    	public IdleListener(IMAPSession session, IMAPClientListener l) {
    		this.session = session;   
    			nextListener = l;
    	}
		public void onResponse(IMAPSession session, String tag,
				List<IMAPResponse> responses) {
			for (IMAPResponse r:responses) {
				log.info ("idle... " + r);
			}
			
		}

		public void onDisconnect(IMAPSession session) {
			log.error("idle error disconnected");
		}
    	
    }
    
    class ListenerToSendIdle implements IMAPClientListener {

    	private IMAPClientListener nextListener;
    	private final IMAPSession session;
    	public ListenerToSendIdle(final IMAPSession session, IMAPClientListener l) {
    		this.session = session;
    			nextListener = l;
    	}

		public void onResponse(final IMAPSession session, String tag,
				List<IMAPResponse> responses) {
			session.executeIdleCommand("t01-idle", nextListener);
		}

		public void onDisconnect(final IMAPSession session) {
			log.error("login error.disconnected");
		}
    	
    }
    
    class ListenerToSendLogout implements IMAPClientListener {
    	
    	private IMAPClientListener nextListener;
    	private final IMAPSession session;
    	public ListenerToSendLogout(final IMAPSession session, IMAPClientListener l) {
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
    	
    } 
    
    
    class ListenerToSendStatus implements IMAPClientListener {
    	
    	private IMAPClientListener nextListener;
    	private final IMAPSession session;
    	public ListenerToSendStatus(final IMAPSession session, IMAPClientListener l) {
    		this.session = session;
    			nextListener = l;
    	}

		public void onResponse(final IMAPSession session, String tag,
				List<IMAPResponse> responses) {
			session.executeStatusCommand("t01-status", "Inbox", new String[] {"UIDNEXT"}, nextListener);
		}

		public void onDisconnect(final IMAPSession session) {
			log.error("login error.disconnected");
		}
    	
    }
    
    class ListenerToSendSelect implements IMAPClientListener {
    	
    	private IMAPClientListener nextListener;
    	private final IMAPSession session;
    	public ListenerToSendSelect(final IMAPSession session, IMAPClientListener l) {
    		this.session = session;
    			nextListener = l;
    	}

		public void onResponse(final IMAPSession session, String tag,
				List<IMAPResponse> responses) {
			session.executeSelectCommand("t01-sel", "Inbox", nextListener);
		}

		public void onDisconnect(final IMAPSession session) {
			log.error("login error.disconnected");
		}
    	
    }

    @Test
	public void testGmailPlainLoginWithIdle() throws IMAPSessionException,
			URISyntaxException, InterruptedException {
		final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI(
				"imaps://imap.gmail.com:993"));

		ChannelFuture loginFuture = session.executeLoginCommand("t1",
				"krinteg1@gmail.com", "1Testuser", new ListenerToSendStatus(session, new ListenerToSendSelect(session, new ListenerToSendIdle(session, new IdleListener(session, null)))));
		Thread.sleep(30000);

	}
    
//    @Test
//    public void testYahooXYMLOGINWithStatus() throws SSLException, NoSuchAlgorithmException, InterruptedException, URISyntaxException {
//        final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imap://localhost:4080"), new ClientListenerStatus());
//        theSession = session;
//
//        ChannelFuture loginFuture = session.executeXYMLOGINCommand("t1", "c2VjcmV0X2tleW5hbWUCaW1hcGdhdGVfaW1hcGRfc2hhcmVkX3NlY3JldAFtYnJfcmVnX2VuY29kZV9pbnRsAnVzAXNpZ25hdHVyZQIyMzMyNUcuU2RoVHVzTVFWZ1VpTkJPVldqNHctAWludGwCdXMBc2xlZGlkAjE4MDE0NDAzOTk0MjI0MDQ4AXltcmVxaWQCOTk4NWIwNzctOTJjMC03MGQyLTAwZDItYzgwMDAwMDEwYTAyAXBlZXJOYW1lAmlQaG9uZQFtc2dTaXplTGltaXQCMjYyMTQ0MDABZnVsbGVtYWlsAmtyaW50ZWcxAXNpbG9udW0COTMyNjE0AWRpc2FibGVSYXRlTGltaXQCZmFsc2UBcXVvdGECMTA3Mzc0MTgyMgFhcHBpZAJqd3MBbGFuZwJ1cwF1c2VyAmtyaW50ZWcxAXRpbWVzdGFtcAIxNDM1Njg2MjMx");
//        loginFuture.awaitUninterruptibly();
//        Thread.sleep(30000);
//
//    }

    @Test
    public void testGmailPlainLoginWithStatus() throws IMAPSessionException, URISyntaxException, InterruptedException {
        final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imaps://imap.gmail.com:993"));
 
        ChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", new ListenerToSendStatus(session, null));
        loginFuture.awaitUninterruptibly();
        Thread.sleep(300000);

    }
    
    @Test
    public void testGmailOauth2Login() throws URISyntaxException, IMAPSessionException, InterruptedException {
        final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imaps://imap.gmail.com:993"));
         final String oauth2Tok = "dXNlcj1rcmludGVnMUBnbWFpbC5jb20BYXV0aD1CZWFyZXIgeWEyOS5vUUc4NGQ3LXBFa0EwZXVvYTFXbFQ1eThqQTJUTEVMQlM5SlQxM1hUV1p3SklzVTYzUVV0cGoxUjRIbU0yODlWQS1kNlhkTWo5eTBjdWcBAQ==";
        ChannelFuture loginFuture = session.executeOAuth2Command("t1", oauth2Tok, null);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(400000000);

    }
}
