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

/**
 * @author kraman
 *
 */
public class IMAPClientIT {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IMAPClientIT.class);
    
    

    @Test
    public void testGmailPlainLoginWithIdle() throws SSLException, NoSuchAlgorithmException, InterruptedException, URISyntaxException {
        final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imaps://imap.gmail.com:993"), new ClientListenerIdle());
        theSession = session;

         ChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser");
        Thread.sleep(3600000);

    }
    
    @Test
    public void testYahooXYMLOGINWithStatus() throws SSLException, NoSuchAlgorithmException, InterruptedException, URISyntaxException {
        final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imap://localhost:4080"), new ClientListenerStatus());
        theSession = session;

        ChannelFuture loginFuture = session.executeXYMLOGINCommand("t1", "c2VjcmV0X2tleW5hbWUCaW1hcGdhdGVfaW1hcGRfc2hhcmVkX3NlY3JldAFtYnJfcmVnX2VuY29kZV9pbnRsAnVzAXNpZ25hdHVyZQIyMzMyNUcuU2RoVHVzTVFWZ1VpTkJPVldqNHctAWludGwCdXMBc2xlZGlkAjE4MDE0NDAzOTk0MjI0MDQ4AXltcmVxaWQCOTk4NWIwNzctOTJjMC03MGQyLTAwZDItYzgwMDAwMDEwYTAyAXBlZXJOYW1lAmlQaG9uZQFtc2dTaXplTGltaXQCMjYyMTQ0MDABZnVsbGVtYWlsAmtyaW50ZWcxAXNpbG9udW0COTMyNjE0AWRpc2FibGVSYXRlTGltaXQCZmFsc2UBcXVvdGECMTA3Mzc0MTgyMgFhcHBpZAJqd3MBbGFuZwJ1cwF1c2VyAmtyaW50ZWcxAXRpbWVzdGFtcAIxNDM1Njg2MjMx");
        loginFuture.awaitUninterruptibly();
        Thread.sleep(30000);

    }

    @Test
    public void testGmailPlainLoginWithStatus() throws SSLException, NoSuchAlgorithmException, InterruptedException, URISyntaxException {
        final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imaps://imap.gmail.com:993"), new ClientListenerStatus());
        theSession = session;

        ChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser");
        loginFuture.awaitUninterruptibly();
        Thread.sleep(30000);

    }

    @Test
    public void testGmailPlainLoginWithAppend() throws SSLException, NoSuchAlgorithmException, InterruptedException, URISyntaxException {
        final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imaps://imap.gmail.com:993"), new ClientListenerAppend());
        theSession = session;

        ChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser");
        loginFuture.awaitUninterruptibly();
        Thread.sleep(30000);

    }

    @Test
    public void testGmailOauth2Login() throws SSLException, NoSuchAlgorithmException, InterruptedException, URISyntaxException {
        final IMAPSession session = IMAPClient.INSTANCE.createSession(new URI("imaps://imap.gmail.com:993"), new ClientListenerOauth2());
        theSession = session;
        final String oauth2Tok = "dXNlcj1rcmludGVnMUBnbWFpbC5jb20BYXV0aD1CZWFyZXIgeWEyOS5vUUc4NGQ3LXBFa0EwZXVvYTFXbFQ1eThqQTJUTEVMQlM5SlQxM1hUV1p3SklzVTYzUVV0cGoxUjRIbU0yODlWQS1kNlhkTWo5eTBjdWcBAQ==";
        ChannelFuture loginFuture = session.executeOAuth2Command("t1", oauth2Tok);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(400000000);

    }

    static IMAPSession theSession;

    class ClientListenerIdle implements IMAPClientListener {

        public void onIdleEvent(List<IMAPResponse> messages) {
            // TODO Auto-generated method stub

        }

        public void onOAuth2LoggedIn(List<IMAPResponse> msgs) {
            // TODO Auto-generated method stub

        }

		public void onDisconnect(IMAPSession session) {
			log.error("disconnected... ");
			// TODO Auto-generated method stub
			log.error("disconnected");

			
		}

		public void onResponse(IMAPSession session, String tag,
				List<IMAPResponse> responses) {
            log.info(" TAG FINAL RESP " + tag);
            for (IMAPResponse r : responses) {

                log.info("<<<" + r);
            }

            if (null != tag && tag.equals("t1")) {
                ChannelFuture idleFuture;
                try {
                    idleFuture = theSession.executeSelectCommand("t2", "Inbox");
                    idleFuture.awaitUninterruptibly();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (null != tag && tag.equals("t3")) {
                ChannelFuture idleFuture;
                try {
                    idleFuture = theSession.executeIdleCommand("t2");
                    idleFuture.awaitUninterruptibly();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (null != tag && tag.equals ("t2")) {
            	try {
					theSession.executeNOOPCommand("t3");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
			
		}

		public void onIdleEvent(IMAPSession session, List<IMAPResponse> messages) {
			// TODO Auto-generated method stub
			
		}

		public void onOAuth2LoggedIn(IMAPSession session,
				List<IMAPResponse> msgs) {
			// TODO Auto-generated method stub
			
		}
    }

    class ClientListenerStatus implements IMAPClientListener {
        public void onResponse(IMAPResponse resp) {
            log.info("<-- " + resp);
        }

        public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
            log.debug(" TAG FINAL RESP " + tag);
            for (IMAPResponse r : responses) {
                log.info("<<<" + r);
            }

            if (null != tag && tag.equals("t4")) {
                ChannelFuture idleFuture;
                try {
                    idleFuture = theSession.executeSelectCommand("t2", "Inbox");
                    idleFuture.awaitUninterruptibly();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (null != tag && tag.equals("t1")) {
                ChannelFuture idleFuture;
                try {
                    idleFuture = theSession.executeStatusCommand("t3", "Inbox", new String[] { "UIDNEXT" });
                    idleFuture.awaitUninterruptibly();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (null != tag && tag.equals("t3")) {
                ChannelFuture idleFuture;
                try {
                    idleFuture = theSession.executeLogoutCommand("t4");
                    idleFuture.awaitUninterruptibly();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }            	
            }

        }

        public void onIdleEvent(IMAPSession session, List<IMAPResponse> messages) {
            // TODO Auto-generated method stub

        }

		public void onOAuth2LoggedIn(IMAPSession session,
				List<IMAPResponse> msgs) {
			ChannelFuture idleFuture;
			try {
				idleFuture = theSession.executeStatusCommand("t3", "Inbox",
						new String[] { "UIDNEXT" });
				idleFuture.awaitUninterruptibly();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public void onDisconnect(IMAPSession session) {
			// TODO Auto-generated method stub
			log.error("disconnected");

			
		}
    }

    class ClientListenerAppend implements IMAPClientListener {
        public void onResponse(IMAPResponse resp) {
            log.info("<-- " + resp);
        }

        public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
            log.debug(" TAG FINAL RESP " + tag);
            for (IMAPResponse r : responses) {
                log.info("<<<" + r);
            }

            if (null != tag && tag.equals("t4")) {
                ChannelFuture idleFuture;
                try {
                    idleFuture = theSession.executeSelectCommand("t2", "Inbox");
                    idleFuture.awaitUninterruptibly();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (null != tag && tag.equals("t1")) {

                final String payload = "Date: Mon, 7 Feb 1994 21:52:25 -0800 (PST)\n" + "From: Fred Friend <foobar@fromhere.com>\n"
                        + "Subject: Long time no see?\n" + "To: heyou@outthere.com\n"
 + "Message-Id: <B27397-0100000@loopityloop.COM>\n"
                        + "MIME-Version: 1.0\n"
                        + "Content-Type: TEXT/PLAIN; CHARSET=US-ASCII"
                        + "\n\n"
                        + "Hey, do you think we can meet at 3:30 tomorrow?"
                        + "\n\n";
                ChannelFuture appendFuture;
                try {
                    appendFuture = theSession.executeAppendCommand("t2", "Inbox", "(\\Seen)", String.valueOf(payload.length()));
                    appendFuture.addListener(new GenericFutureListener<ChannelFuture>() {

                        public void operationComplete(ChannelFuture future) throws Exception {
                            theSession.executeRawTextCommand(payload);
                        }

                    });
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }

        public void onIdleEvent(IMAPSession session, List<IMAPResponse> messages) {
            // TODO Auto-generated method stub

        }

        public void onOAuth2LoggedIn(IMAPSession session, List<IMAPResponse> msgs) {
            // TODO Auto-generated method stub

        }

		public void onDisconnect(IMAPSession session) {
			// TODO Auto-generated method stub
			log.error("disconnected");

			
		}
    }

    class ClientListenerOauth2 implements IMAPClientListener {
        public void onResponse(IMAPResponse resp) {
            log.info("<-- " + resp);
        }

        public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
            log.info(" TAG FINAL RESP " + tag);
            if (null != tag && tag.equals("t2")) {
            	try {
					session.executeIdleCommand("t3");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }

        }

        public void onIdleEvent(IMAPSession session, List<IMAPResponse> messages) {
            // TODO Auto-generated method stub

        }

        public void onOAuth2LoggedIn(IMAPSession session, List<IMAPResponse> msgs) {
            try {
                theSession.executeSelectCommand("t2", "Inbox");
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

        }

		public void onDisconnect(IMAPSession session) {
			log.error("disconnected");
			// TODO Auto-generated method stub
			
		}
    }

}
