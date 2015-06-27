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
import java.util.concurrent.Executors;

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
        final IMAPClient client = new IMAPClient(Executors.newScheduledThreadPool(5));
        final IMAPSession session = client.createSession(new URI("imaps://imap.gmail.com:993"), null);
        theSession = session;

         ChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", new ClientListenerIdle());
        Thread.sleep(60000);

    }

    @Test
    public void testGmailPlainLoginWithStatus() throws SSLException, NoSuchAlgorithmException, InterruptedException, URISyntaxException {
        final IMAPClient client = new IMAPClient(Executors.newScheduledThreadPool(5));
        final IMAPSession session = client.createSession(new URI("imaps://imap.gmail.com:993"), null);
        theSession = session;

        ChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", new ClientListenerStatus());
        loginFuture.awaitUninterruptibly();
        Thread.sleep(30000);

    }

    @Test
    public void testGmailPlainLoginWithAppend() throws SSLException, NoSuchAlgorithmException, InterruptedException, URISyntaxException {
        final IMAPClient client = new IMAPClient(Executors.newScheduledThreadPool(5));
        final IMAPSession session = client.createSession(new URI("imaps://imap.gmail.com:993"), null);
        theSession = session;

        ChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", new ClientListenerAppend());
        loginFuture.awaitUninterruptibly();
        Thread.sleep(30000);

    }

    @Test
    public void testGmailOauth2Login() throws SSLException, NoSuchAlgorithmException, InterruptedException, URISyntaxException {
        final IMAPClient client = new IMAPClient(Executors.newScheduledThreadPool(5));
        final IMAPSession session = client.createSession(new URI("imaps://imap.gmail.com:993"), null);
        theSession = session;
        final String oauth2Tok = "dXNlcj1pbWFwbmlvY2xpZW50dGVzdEBnbWFpbC5jb20BYXV0aD1CZWFyZXIgeWEyOS5hQUNLblZRM1VUQmxBU0VBQUFER2hrYWM3eFVPd05wWm83X3M1MzJqTC1pNDlvYjFJZ2VlZmJ1N09NZVp2dXVlZHBYeTlUTkxoUDZZMk5FSjFJTQEB";

        ChannelFuture loginFuture = session.executeOAuth2Command("t1", oauth2Tok, new ClientListenerOauth2());
        loginFuture.awaitUninterruptibly();
        Thread.sleep(30000);

    }

    static IMAPSession theSession;

    class ClientListenerIdle implements IMAPClientListener {
        public void onResponse(IMAPResponse resp) {
            log.info("<-- " + resp);
        }

        public void onResponse(String tag, List<IMAPResponse> responses) {
            log.info(" TAG FINAL RESP " + tag);
            for (IMAPResponse r : responses) {

                log.info("<<<" + r);
            }

            if (null != tag && tag.equals("t1")) {
                ChannelFuture idleFuture;
                try {
                    idleFuture = theSession.executeSelectCommand("t2", "Inbox", new ClientListenerIdle());
                    idleFuture.awaitUninterruptibly();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (null != tag && tag.equals("t2")) {
                ChannelFuture idleFuture;
                try {
                    idleFuture = theSession.executeIdleCommand("tt3", new ClientListenerIdle());
                    idleFuture.awaitUninterruptibly();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }

        public void onIdleEvent(List<IMAPResponse> messages) {
            // TODO Auto-generated method stub

        }

        public void onOAuth2LoggedIn(List<IMAPResponse> msgs) {
            // TODO Auto-generated method stub

        }
    }

    class ClientListenerStatus implements IMAPClientListener {
        public void onResponse(IMAPResponse resp) {
            log.info("<-- " + resp);
        }

        public void onResponse(String tag, List<IMAPResponse> responses) {
            log.debug(" TAG FINAL RESP " + tag);
            for (IMAPResponse r : responses) {
                log.info("<<<" + r);
            }

            if (null != tag && tag.equals("t4")) {
                ChannelFuture idleFuture;
                try {
                    idleFuture = theSession.executeSelectCommand("t2", "Inbox", new ClientListenerStatus());
                    idleFuture.awaitUninterruptibly();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (null != tag && tag.equals("t1")) {
                ChannelFuture idleFuture;
                try {
                    idleFuture = theSession.executeStatusCommand("t3", "Inbox", new String[] { "UIDNEXT" }, new ClientListenerStatus());
                    idleFuture.awaitUninterruptibly();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }

        public void onIdleEvent(List<IMAPResponse> messages) {
            // TODO Auto-generated method stub

        }

        public void onOAuth2LoggedIn(List<IMAPResponse> msgs) {
            // TODO Auto-generated method stub

        }
    }

    class ClientListenerAppend implements IMAPClientListener {
        public void onResponse(IMAPResponse resp) {
            log.info("<-- " + resp);
        }

        public void onResponse(String tag, List<IMAPResponse> responses) {
            log.debug(" TAG FINAL RESP " + tag);
            for (IMAPResponse r : responses) {
                log.info("<<<" + r);
            }

            if (null != tag && tag.equals("t4")) {
                ChannelFuture idleFuture;
                try {
                    idleFuture = theSession.executeSelectCommand("t2", "Inbox", new ClientListenerAppend());
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
                    appendFuture = theSession.executeAppendCommand("t2", "Inbox", "(\\Seen)", String.valueOf(payload.length()), null);
                    appendFuture.addListener(new GenericFutureListener<ChannelFuture>() {

                        public void operationComplete(ChannelFuture future) throws Exception {
                            theSession.executeRawTextCommand(payload, null);
                        }

                    });
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }

        public void onIdleEvent(List<IMAPResponse> messages) {
            // TODO Auto-generated method stub

        }

        public void onOAuth2LoggedIn(List<IMAPResponse> msgs) {
            // TODO Auto-generated method stub

        }
    }

    class ClientListenerOauth2 implements IMAPClientListener {
        public void onResponse(IMAPResponse resp) {
            log.info("<-- " + resp);
        }

        public void onResponse(String tag, List<IMAPResponse> responses) {
            log.info(" TAG FINAL RESP " + tag);
            for (IMAPResponse r : responses) {
                log.info("<<<" + r);
            }

        }

        public void onIdleEvent(List<IMAPResponse> messages) {
            // TODO Auto-generated method stub

        }

        public void onOAuth2LoggedIn(List<IMAPResponse> msgs) {
            try {
                theSession.executeSelectCommand("t2", "Inbox", new ClientListenerOauth2());
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

        }
    }

}
