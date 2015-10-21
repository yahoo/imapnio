/**
 *
 */
package com.lafaspot.imapnio.client;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

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
        public void onDisconnect(IMAPSession session, Throwable cause) {
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
            public void onDisconnect(IMAPSession session, Throwable cause) {
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

            sessions[i] = theClient.createSession(new URI(gmailServer), new Properties(),
                    new TestMultipleSessionsSendLoginListener(sessions[i], listenerToSendSelect), logManager);
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
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new Properties(),
                new TestConnectionListener("testGmailPlainLoginWithIdle"), logManager);
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
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new Properties(),
                new TestConnectionListener("testGamailCapability"), logManager);
        session.connect();
        Thread.sleep(1000);

        final IMAPChannelFuture loginFuture = session.executeCapabilityCommand("t1-cap", new TestCommandListener("testGamailCapability"));
        loginFuture.awaitUninterruptibly();
        Thread.sleep(2000);
    }

    /**
     * @throws Exception failed data
     */
    @Test
    public void testGmailPlainLoginWithID() throws Exception {
        final String gmailServer = "imaps://imap.gmail.com:993";
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new Properties(), new TestConnectionListener(
                "testGmailPlainLoginWithIdle"), logManager);

        final InetSocketAddress localAddress = null; // new InetSocketAddress("10.101.30.252", 0);
        session.connect(localAddress);
        Thread.sleep(500);
        // try {
            session.executeIDCommand("t01-id", new String[] { "name", "roadrunner", "version", "1.0" },null); /* new TestCommandListener(
                    "ID command"));
        } catch (IMAPSessionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        */

        IMAPCommandListener listenerToSendSelect = new IMAPCommandListener() {

            @Override
            public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
                for (final IMAPResponse r : responses) {
                    log.info(" <-- " + r, null);
                }
            }

            @Override
            public void onMessage(IMAPSession session, IMAPResponse response) {
                log.info("onMessage " + response, null);
            }
        };

        session.executeLoginCommand("t1", "krinteg1@gmail.com", "1Testuser", listenerToSendSelect);
        Thread.sleep(10000);

    }

    /**
     * @throws Exception failed data
     */
    @Test
    public void testGmailPlainLoginWithStatus() throws Exception {
        final String gmailServer = "imaps://imap.gmail.com:993";
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new Properties(),
                new TestConnectionListener("testGmailPlainLoginWithStatus"), logManager);

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
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new Properties(),
                new TestConnectionListener("testGmailPlainLoginWithList"), logManager);

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
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new Properties(),
                new TestConnectionListener("testGmailOauth2Login"), logManager);

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
        final String oauth2Tok = "ya29.BQLRvf5tGjARtiTLgWNDZvrUXOX-08YOq67TObA2wRN7iiJ-dLaBA3aCHiI3HTmQ7LSH";
        final StringBuffer buf = new StringBuffer();
        buf.append("user=").append("krinteg1@gmail.com").append("\u0001").append("auth=Bearer ").append(oauth2Tok).append("\u0001").append("\u0001");
        final String encOAuthStr = Base64.getEncoder().encodeToString(buf.toString().getBytes(StandardCharsets.UTF_8));

        final IMAPChannelFuture loginFuture = session.executeOAuth2Command("t1", encOAuthStr, listenerToSendSelect);
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
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new Properties(), new TestConnectionListener(
                "testGmailSASLOauth2Login"), logManager);

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
        final IMAPChannelFuture loginFuture = session.executeSASLXOAuth2("t1", "krinteg1@outlook.com", oauth2Tok, listenerToSendSelect);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(2000);

    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testYahooPlainLoginWithStatus() throws Exception {
        final String gmailServer = "imaps://imap.mail.yahoo.com:993";
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new Properties(),
                new TestConnectionListener("testGmailPlainLoginWithStatus"), logManager);

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
                            session.executeStatusCommand("t01-status", "Inbox",
 new String[] { "UIDNEXT", "UIDVALIDITY", "UNSEEN", "MESSAGES",
                                    "RECENT" }, new TestCommandListener(
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

        final IMAPChannelFuture loginFuture = session.executeLoginCommand("t1", "yqa_mail_14400270489891894@yahoo.com", "OBeBNBgWIeCMLHY",
                listenerToSendStatus);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(2000);
    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testOutlookPlainLoginWithList() throws Exception {
        final String gmailServer = "imaps://imap-mail.outlook.com:993";
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new Properties(),
                new TestConnectionListener("testGmailPlainLoginWithList"), logManager);

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
        final IMAPChannelFuture loginFuture = session.executeLoginCommand("t1", "krinteg1@outlook.com", "1Testuser", listenerToSendList);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(2000);

    }

    /**
     * @throws Exception
     *             failed data
     */
    @Test
    public void testOutlookSASLOauth2Login() throws Exception {

        final String gmailServer = "imaps://imap-mail.outlook.com:993";
        final IMAPSession session = theClient.createSession(new URI(gmailServer), new Properties(), new TestConnectionListener(
                "testGmailSASLOauth2Login"), logManager);

        session.connect();
        Thread.sleep(3000);

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
                    session.executeRawTextCommand("EwBwAq1DBAAUGCCXc8wU/zFu9QnLdZXy+YnElFkAAfvtBPUVvlDGqq2YGiQUl7XyuIqhjv4V+iwsdiArXRTNcATxLXKTsjRYqwU99DRl89cMAfg5STNKGC6+iCDDOSYxsmHBuHHHwqW25euHXoksbUfgYHdEMkzkSV7iyeb/I6BgLCUYRvGhks2pwNSPGeLIHmc/r6IxlV+zls1nX6/Rp6FjyniIXS8m5atEoyuwjxBBHvl2M8xfw0p3Mw865YZlvtlapcy8AYIFZ094U5Rp3TKU3DAMBQ4QFkeVOMboy/dK1AuPJnwQiuMsWMlUnaT6s0ZmJoKawE1HNkKUpwWfcck2f3lnIKlSYFZnaAN1MW+p1WmYdW700DJhbMpbOZcDZgAACDn4zVs8RjlMQAFRpUeLuEm0zGzJ+TVCxgAFqm+JR+DbEOosq1zAp5/KkaZOOhmwxNGb9zDiNjSCD+/pQlns9y83P+ZMGqUpPsXgAfulyevwLxxXANnBkj7yX9MrJebdLyxsB0FF64WnimVc2MohNQDWS9YmY2fr48qO8E03L4LmL4GKC6jG5Nrc+NQJXnNGJnTEL2xHZnR3u1Z9X1k0YFlQReD0+yef63JuRKHF//cYP01C2I/TBxIIifiMFQbhNjfGFUaP7B/5L0QhG8WG/2vVoYrqlr0PKHLuR26Fst8OeylFYBSEv78KRCoC+pYUIUx0tVvUeAus6VBUACTIouZLvVd8kuwD3diNP7/wChrJjUn4HI6ToTJhfLMarPKtr0U9+aRaUqZQXBmXZDdYR2+e/q16LPRq3+RT1uXqnI+2nlWuzYfSsvpupl8B");
                } else {
                    throw new RuntimeException("unknown message " + response.toString());
                }
            }
        };


        final String oauth2Tok = "EwBwAq1DBAAUGCCXc8wU/zFu9QnLdZXy+YnElFkAAfo4mqnJTbVydFJf/ferxtZEHmdZnpXdOkOFIAG4q1DUnu2Vl1gow+kTZzJrnxxVSZGmyrfjAZjmKcsj8/HnK5DHFHX+PqJF3jitNJ8k7JkipL41JH9R2crL9eqYiarmhzzz1hQBED4it/P3HnjWN46jAOKC8uMpEnG+N9XtMb/g9NBnGtL/QQetBJ5eifV0Te24HoccJJYiNxj/Rs8KxMlMvfrg9oy5kulzmFLFBccRFJQADw3jyIl/q17ZFbfjO6RfKcNrVCw8Tt2D92UfPf0ZG+3381JHfWo3jiqQX630aIZG09W5SRJ6Fp/T318mGOgg00wcOhmAo3PSpxsTao4DZgAACHi5inzeHelHQAGxJh+JTw/5sWu1f15iLdp4W6yEJXZLfIsNsidjQclCUyb/O18PMnqWFKexTJxvzitEYg9R4oZqzYKWZVnBCTry6FhVhewXBph3uunSlGO3Q72OMzdTb7qTi4jAZNwm9yDfybZEcwYn7rJC7sGaZhKM4JFyLgjj2AedDyKa1zOYLZREw9kpaS4V4x5xW4wOeExoJCcCWqs0dqr4Sq50aY0RLGjDztoHKpD9UadlxGVXvLzKL5Tuj94AxJRTvEk2U1TmnOCByGsSf5p2dsKt2W/o3ItW5aSzeAtjD8tHtNAh50E6rfVbptjXrCjhF0ytGBzSPF4m+TaBI4SZh77uV4WlpNWCSZKNYBDPgBLuUQ8ZQPoj/X6zdxRNWpIKW7B1ERlUA5VTZI3CGmnT4W2aBHR3ji2Tq1H+tu9E1lKpQ5xu/18B";
        final StringBuffer buf = new StringBuffer();
        buf.append("user=").append("krinteg1@outlook.com").append("\u0001").append("auth=Bearer ").append(oauth2Tok).append("\u0001")
                .append("\u0001");
        final String encOAuthStr = Base64.getEncoder().encodeToString(buf.toString().getBytes(StandardCharsets.UTF_8));

        // final IMAPChannelFuture loginFuture = session.executeSASLXOAuth2("t1", "krinteg1@outlook.com", oauth2Tok, listenerToSendSelect);

        final IMAPChannelFuture loginFuture = session.executeOAuth2Command("t22", encOAuthStr, listenerToSendSelect);
        loginFuture.awaitUninterruptibly();
        Thread.sleep(2000);

    }
}
