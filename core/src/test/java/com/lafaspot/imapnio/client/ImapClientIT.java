/**
 *
 */
package com.lafaspot.imapnio.client;

import java.net.URI;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.channel.IMAPChannelFuture;
import com.lafaspot.imapnio.exception.IMAPSessionException;
import com.lafaspot.imapnio.listener.IMAPCommandListener;
import com.lafaspot.imapnio.listener.IMAPConnectionListener;
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

    class TestCommandListener implements IMAPCommandListener {

        final String logPrefix;

        TestCommandListener(String prefix) {
            logPrefix = prefix;
        }
        @Override
        public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
            for (final IMAPResponse r : responses) {
                log.info(logPrefix + " " + r, null);
            }
        }

        @Override
        public void onMessage(IMAPSession session, IMAPResponse response) {
            log.info(logPrefix + " " + response, null);
        }

    }

    class TestConnectionListener implements IMAPConnectionListener {

        final String logPrefix;

        TestConnectionListener(String prefix) {
            this.logPrefix = prefix;
        }

        @Override
        public void onConnect(IMAPSession session) {
            log.info(logPrefix + " got onConnect", null);

        }

        @Override
        public void onDisconnect(IMAPSession session) {
            log.info(logPrefix + " got onDisconnect", null);

        }

        @Override
        public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
            log.info(logPrefix + " got onResponse", null);

        }

        @Override
        public void onMessage(IMAPSession session, IMAPResponse response) {
            log.info(logPrefix + " got onMessage " + response, null);

        }

    }
    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testSMultipleSessions() throws Exception {

        class TestMultipleSessionsSendLoginListener implements IMAPConnectionListener {
            final IMAPSession session;
            final IMAPCommandListener listener;

            TestMultipleSessionsSendLoginListener(IMAPSession session, IMAPCommandListener listener) {
                this.session = session;
                this.listener = listener;
            }

            @Override
            public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
                for (IMAPResponse r : responses) {
                    log.info("testSMultipleSessions " + r, null);
                }

            }

            @Override
            public void onMessage(IMAPSession session, IMAPResponse response) {
                log.info("testSMultipleSessions " + response, null);
            }

            @Override
            public void onDisconnect(IMAPSession session) {
                // throw new RuntimeException("testMultipleSessions onDisconnect");
            }

            @Override
            public void onConnect(IMAPSession session) {
                try {
                    session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", listener);
                } catch (IMAPSessionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        ;

        IMAPCommandListener listenerToSendIdle = new IMAPCommandListener() {
            @Override
            public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
                for (IMAPResponse r : responses) {
                    if (r.getTag() != null) {
                        Assert.assertTrue(r.isOK());

                        try {
                            session.executeIdleCommand("t1-idle", new TestCommandListener("testSMultipleSessions"));
                        } catch (SecurityException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IMAPSessionException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        log.info("testSMultipleSessions " + r, null);
                    }
                }
            }

            @Override
            public void onMessage(IMAPSession session, IMAPResponse response) {
                throw new RuntimeException("testSMultipleSessions - unknown response " + response);
            }
        };

        IMAPCommandListener listenerToSendSelect = new IMAPCommandListener() {

            @Override
            public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
                for (IMAPResponse r : responses) {

                    if (r.getTag() != null) {
                        Assert.assertTrue(r.isOK());

                        try {
                            session.executeSelectCommand("t01-sel", "Inbox", listenerToSendIdle);
                        } catch (IMAPSessionException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        log.info("testSMultipleSessions " + r, null);
                    }
                }
            }

            @Override
            public void onMessage(IMAPSession session, IMAPResponse response) {
            }
        };

        final String gmailServer = "imaps://imap.gmail.com:993";
        final int maxSessions = 2;
        final IMAPSession[] sessions = new IMAPSession[maxSessions];
        for (int i = 0; i < maxSessions; i++) {

            // final String gmailServer = "imap://localhost:9993";

            sessions[i] = theClient.createSession(new URI(gmailServer), new TestMultipleSessionsSendLoginListener(sessions[i], listenerToSendSelect),
                    logManager);
            sessions[i].connect();

        }
        Thread.sleep(5000);

        for (final IMAPSession s : sessions) {
            s.executeDoneCommand(new TestCommandListener("testSMultipleSessions"));
            Thread.sleep(2000);
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
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new TestConnectionListener("testGmailPlainLoginWithIdle"),
                logManager);
        session.connect();
        Thread.sleep(500);

        IMAPCommandListener listenerToSendIdle = new IMAPCommandListener() {
            @Override
            public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {

                for (IMAPResponse r : responses) {
                    if (r.getTag() != null) {
                        Assert.assertTrue(r.isOK());
                        try {
                            session.executeIdleCommand("t1-idle", new TestCommandListener("testGmailPlainLoginWithIdle"));
                        } catch (SecurityException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IMAPSessionException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        log.info("testGmailPlainLoginWithIdle " + r, null);
                    }
                }
            }

            @Override
            public void onMessage(IMAPSession session, IMAPResponse response) {
                log.info("onMessage " + response, null);
            }
        };

        IMAPCommandListener listenerToSendSelect = new IMAPCommandListener() {

            @Override
            public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
                try {
                    session.executeSelectCommand("t01-sel", "Inbox", listenerToSendIdle);
                } catch (IMAPSessionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(IMAPSession session, IMAPResponse response) {
                log.info("onMessage " + response, null);
            }
        };

        session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", listenerToSendSelect);
        Thread.sleep(20000);
        session.executeDoneCommand(new TestCommandListener("testGmailPlainLoginWithIdle"));
        Thread.sleep(1000);

    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testGamailCapability() throws Exception {
        final String gmailServer = "imaps://imap.gmail.com:993";
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new TestConnectionListener("testGamailCapability"), logManager);
        session.connect();
        Thread.sleep(1000);

        final IMAPChannelFuture loginFuture = session.executeCapabilityCommand("t1-cap", new TestCommandListener("testGamailCapability"));
        loginFuture.awaitUninterruptibly();
        Thread.sleep(2000);
    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testGmailPlainLoginWithStatus() throws Exception {
        final String gmailServer = "imaps://imap.gmail.com:993";
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new TestConnectionListener("testGmailPlainLoginWithStatus"),
                logManager);

        session.connect();
        Thread.sleep(1000);

        IMAPCommandListener listenerToSendStatus = new IMAPCommandListener() {

            @Override
            public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
                for (final IMAPResponse r : responses) {
                    if (r.getTag() != null) {
                        Assert.assertTrue(r.isOK());
                        log.info("testGmailPlainLoginWithStatus sending status", null);
                        try {
                            session.executeStatusCommand("t01-status", "Inbox", new String[] { "UIDNEXT" }, new TestCommandListener(
                                    "testGmailPlainLoginWithStatus"));
                        } catch (SecurityException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IMAPSessionException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        log.info("testGmailPlainLoginWithStatus " + r, null);
                    }
                }
            }

            @Override
            public void onMessage(IMAPSession session, IMAPResponse response) {
            }
        };

        final IMAPChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", listenerToSendStatus);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(2000);
    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testGmailPlainLoginWithList() throws Exception {
        final String gmailServer = "imaps://imap.gmail.com:993";
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new TestConnectionListener("testGmailPlainLoginWithList"),
                logManager);

        session.connect();
        Thread.sleep(1000);

        IMAPCommandListener listenerToSendList = new IMAPCommandListener() {

            @Override
            public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
                for (final IMAPResponse r : responses) {
                    if (r.getTag() != null) {
                        Assert.assertTrue(r.isOK());
                        log.info("testGmailPlainLoginWithList sending list", null);
                        try {
                            session.executeListCommand("t0-list", "", "*", new TestCommandListener("testGmailPlainLoginWithList"));
                        } catch (IMAPSessionException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        log.info("testGmailPlainLoginWithList " + r, null);
                    }
                }
            }

            @Override
            public void onMessage(IMAPSession session, IMAPResponse response) {
            }
        };
        final IMAPChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", listenerToSendList);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(2000);

    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testGmailOauth2Login() throws Exception {
        final String gmailServer = "imaps://imap.gmail.com:993";
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new TestConnectionListener("testGmailOauth2Login"), logManager);

        session.connect();
        Thread.sleep(1000);
        IMAPCommandListener listenerToSendIdle = new IMAPCommandListener() {
            @Override
            public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
                for (IMAPResponse r : responses) {
                    if (r.getTag() != null) {
                        Assert.assertTrue(r.isOK());
                        log.info("testGmailOauth2Login sending idle", null);
                        try {
                            session.executeIdleCommand("t1", new TestCommandListener("testGmailOauth2Login"));
                        } catch (SecurityException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IMAPSessionException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        log.info("testGmailOauth2Login " + r, null);
                    }
                }
            }

            @Override
            public void onMessage(IMAPSession session, IMAPResponse response) {
            }
        };

        IMAPCommandListener listenerToSendSelect = new IMAPCommandListener() {

            @Override
            public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
                for (IMAPResponse r : responses) {
                    if (r.getTag() != null) {
                        Assert.assertTrue(r.isOK());
                        log.info("testGmailOauth2Login sending select", null);
                        try {
                            session.executeSelectCommand("t01-sel", "Inbox", listenerToSendIdle);
                        } catch (IMAPSessionException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    } else {
                        log.info("testGmailOauth2Login " + r, null);
                    }
                }
            }


            @Override
            public void onMessage(IMAPSession session, IMAPResponse response) {
            }
        };

        @SuppressWarnings("checkstyle:linelength")
        final String oauth2Tok = "dXNlcj1rcmludGVnMUBnbWFpbC5jb20BYXV0aD1CZWFyZXIgeWEyOS5zQUVTb3hfblN5QjA0eEljZHNTUF9tbFZGN096dHN6WDJsa19FMXVwLUw3UGRiSG9BR2l2WG1nSWQ4Q0x2a0RLUnFEUgEB";
        final IMAPChannelFuture loginFuture = session.executeOAuth2Command("t1", oauth2Tok, listenerToSendSelect);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(1000);
    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testGmailSASLOauth2Login() throws Exception {

        final String gmailServer = "imaps://imap.gmail.com:993";
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new TestConnectionListener("testGmailSASLOauth2Login"), logManager);

        session.connect();
        Thread.sleep(500);

        IMAPCommandListener listenerToSendIdle = new IMAPCommandListener() {
            @Override
            public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
                for (IMAPResponse r : responses) {
                    if (r.getTag() != null) {
                        Assert.assertTrue(r.isOK());
                        try {
                            session.executeIdleCommand("t1-idle", new TestCommandListener("testGmailSASLOauth2Login"));
                        } catch (SecurityException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IMAPSessionException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        log.info("testGmailSASLOauth2Login " + r, null);
                    }
                }
            }

            @Override
            public void onMessage(IMAPSession session, IMAPResponse response) {
                throw new RuntimeException("unknown response " + response.toString());
            }
        };

        IMAPCommandListener listenerToSendSelect = new IMAPCommandListener() {

            @Override
            public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {

                for (IMAPResponse r : responses) {
                    log.info(r, null);
                    if (r.getTag() != null) {
                        Assert.assertTrue(r.isOK());
                        log.info("testGmailSASLOauth2Login sending select", null);
                        try {
                            session.executeSelectCommand("t01-sel", "Inbox", listenerToSendIdle);
                        } catch (IMAPSessionException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        log.info("testGmailSASLOauth2Login " + r, null);
                    }
                }
            }

            @Override
            public void onMessage(IMAPSession session, IMAPResponse response) {
                if (response.isContinuation()) {
                    session.executeRawTextCommand("ya29.-QHOsbpZG-1AT0b8YWEGWwl1g375kNTFKpardSF3gGyBMttOG_LJj-At3CS-B6evvRYz");
                } else {
                    throw new RuntimeException("unknown message " + response.toString());
                }
            }
        };

        final String oauth2Tok = "ya29.-QHOsbpZG-1AT0b8YWEGWwl1g375kNTFKpardSF3gGyBMttOG_LJj-At3CS-B6evvRYz";
        final IMAPChannelFuture loginFuture = session.executeSASLXOAuth2("t1", "krinteg1@gmail.com", oauth2Tok, listenerToSendSelect);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(2000);

    }
}
