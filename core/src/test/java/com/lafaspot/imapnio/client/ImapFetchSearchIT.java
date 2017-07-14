package com.lafaspot.imapnio.client;

import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.listener.IMAPCommandListener;
import com.lafaspot.imapnio.listener.IMAPConnectionListener;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger.Level;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * Tests imap fetch and search command integration tests.
 *
 * @author kaituo
 *
 */
public class ImapFetchSearchIT {

    /** Imap client. */
    private IMAPClient theClient;
    /** Log manager. */
    private LogManager logManager;
    /** Logger. */
    private Logger log;
    /** Imap client command timeout in seconds. */
    private static final int CLIENT_COMMAND_TIMEOUT_IN_SECONDS = 30;
    /** Imap client threads. */
    private static final int CLIENT_THREADS = 5;
    /** Yahoo imap server. */
    private static final String YAHOO_IMAP_SERVER = "imaps://imap.mail.yahoo.com:993";
    /** Gmail imap server. */
    private static final String GMAIL_IMAP_SERVER = "imaps://imap.gmail.com:993";
    /** Imap client config. */
    private Properties imapConfig;

    @BeforeClass
    public void setup() {
        theClient = new IMAPClient(CLIENT_THREADS);
        logManager = new LogManager(Level.DEBUG, 5);
        logManager.setLegacy(true);
        log = LoggerFactory.getLogger(ImapFetchSearchIT.class);
        imapConfig = new Properties();
        imapConfig.setProperty(IMAPSession.CONFIG_IMAP_INACTIVITY_TIMEOUT_KEY, "60");
        imapConfig.setProperty(IMAPSession.CONFIG_CONNECTION_TIMEOUT_KEY, "60000");
        imapConfig.setProperty(IMAPSession.CONFIG_IMAP_TIMEOUT_KEY, "60000");

    }

    class TestCommandListener implements IMAPCommandListener {

        final String logPrefix;

        AtomicBoolean isDone = new AtomicBoolean(false);

        AtomicReference<IMAPResponse> tagResponse = new AtomicReference<>();

        TestCommandListener(String prefix) {
            logPrefix = prefix;
        }

        @Override
        public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
            for (final IMAPResponse r : responses) {
                if (r.getTag() != null) {
                    isDone.set(true);
                    tagResponse.set(r);
                }
                log.info(logPrefix + " " + r);
            }
        }

        @Override
        public void onMessage(IMAPSession session, IMAPResponse response) {
            log.info(logPrefix + " " + response);
        }

        public boolean isDone(int seconds) {
            final long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < seconds * 1000) {
                if (isDone.get()) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
            return isDone.get();
        }

        public IMAPResponse getTagResponse() {
            return tagResponse.get();
        }

    }

    class TestConnectionListener implements IMAPConnectionListener {

        final String logPrefix;

        TestConnectionListener(String prefix) {
            this.logPrefix = prefix;
        }

        @Override
        public void onConnect(IMAPSession session) {
            log.info(logPrefix + " got onConnect");

        }

        @Override
        public void onDisconnect(IMAPSession session, Throwable cause) {
            log.info(logPrefix + " got onDisconnect");

        }

        @Override
        public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
            log.info(logPrefix + " got onResponse");

        }

        @Override
        public void onMessage(IMAPSession session, IMAPResponse response) {
            log.info(logPrefix + " got onMessage " + response);

        }

        @Override
        public void onInactivityTimeout(IMAPSession session) {
            log.info(" inactivity detected");
        }

    }

    /**
     * Tests yahoo mail fetch command with literal response.
     *
     * @throws Exception failed data
     */
    @Test
    public void testYahooFetchLiteralResponse() throws Exception {
        final IMAPSession session = theClient.createSession(new URI(YAHOO_IMAP_SERVER), imapConfig, new TestConnectionListener("testLogin"),
                logManager);
        session.connect();
        Thread.sleep(2000);

        final TestCommandListener loginListener = new TestCommandListener("login");
        session.executeLoginCommand("a1", "testimapnio@yahoo.com", "extraginger", loginListener);
        Assert.assertTrue(loginListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(loginListener.getTagResponse().isOK());

        final TestCommandListener selectListener = new TestCommandListener("select");
        session.executeSelectCommand("a2", "Inbox", selectListener);
        Assert.assertTrue(selectListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(selectListener.getTagResponse().isOK());

        final TestCommandListener fetchListener = new TestCommandListener("fetch");
        session.executeFetchCommand("a3", false, "1", "(BODYSTRUCTURE BODY.PEEK[HEADER])", fetchListener);
        Assert.assertTrue(fetchListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(fetchListener.getTagResponse().isOK());
    }

    /**
     * Tests gmail fetch command with literal response.
     *
     * @throws Exception failed data
     */
    @Test
    public void testGmailFetchLiteralResponse() throws Exception {
        final IMAPSession session = theClient.createSession(new URI(GMAIL_IMAP_SERVER), imapConfig, new TestConnectionListener("testLogin"),
                logManager);
        session.connect();
        Thread.sleep(2000);

        final TestCommandListener loginListener = new TestCommandListener("login");
        session.executeLoginCommand("a1", "testimapnio@gmail.com", "extraginger", loginListener);
        Assert.assertTrue(loginListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(loginListener.getTagResponse().isOK());

        final TestCommandListener selectListener = new TestCommandListener("select");
        session.executeSelectCommand("a2", "Inbox", selectListener);
        Assert.assertTrue(selectListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(selectListener.getTagResponse().isOK());

        final TestCommandListener fetchListener = new TestCommandListener("fetch");
        session.executeFetchCommand("a3", false, "1", "(BODYSTRUCTURE BODY.PEEK[HEADER])", fetchListener);
        Assert.assertTrue(fetchListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(fetchListener.getTagResponse().isOK());
    }

    /**
     * Tests yahoo mail fetch command with non literal response.
     *
     * @throws Exception failed data
     */
    @Test
    public void testYahooFetchNonLiteralResponse() throws Exception {
        final IMAPSession session = theClient.createSession(new URI(YAHOO_IMAP_SERVER), imapConfig, new TestConnectionListener("testLogin"),
                logManager);
        session.connect();
        Thread.sleep(2000);

        final TestCommandListener loginListener = new TestCommandListener("login");
        session.executeLoginCommand("a1", "testimapnio@yahoo.com", "extraginger", loginListener);
        Assert.assertTrue(loginListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(loginListener.getTagResponse().isOK());

        final TestCommandListener selectListener = new TestCommandListener("select");
        session.executeSelectCommand("a2", "Inbox", selectListener);
        Assert.assertTrue(selectListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(selectListener.getTagResponse().isOK());

        final TestCommandListener fetchListener = new TestCommandListener("fetch");
        session.executeFetchCommand("a3", true, "1:*", "FLAGS", fetchListener);
        Assert.assertTrue(fetchListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(fetchListener.getTagResponse().isOK());
    }

    /**
     * Tests yahoo mail UID Search command.
     *
     * @throws Exception failed data
     */
    @Test
    public void testYahooUIDSearch() throws Exception {
        final IMAPSession session = theClient.createSession(new URI(YAHOO_IMAP_SERVER), imapConfig, new TestConnectionListener("testLogin"),
                logManager);
        session.connect();
        Thread.sleep(2000);

        final TestCommandListener loginListener = new TestCommandListener("login");
        session.executeLoginCommand("a1", "testimapnio@yahoo.com", "extraginger", loginListener);
        Assert.assertTrue(loginListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(loginListener.getTagResponse().isOK());

        final TestCommandListener selectListener = new TestCommandListener("select");
        session.executeSelectCommand("a2", "Inbox", selectListener);
        Assert.assertTrue(selectListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(selectListener.getTagResponse().isOK());

        final TestCommandListener searchListener = new TestCommandListener("search");
        session.executeSearchCommand("a3", true, "TEXT yahoo.com NOT DELETED", searchListener);
        Assert.assertTrue(searchListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(searchListener.getTagResponse().isOK());
    }

    /**
     * Tests yahoo mail Search command.
     *
     * @throws Exception failed data
     */
    @Test
    public void testYahooSearch() throws Exception {
        final IMAPSession session = theClient.createSession(new URI(YAHOO_IMAP_SERVER), imapConfig, new TestConnectionListener("testLogin"),
                logManager);
        session.connect();
        Thread.sleep(2000);

        final TestCommandListener loginListener = new TestCommandListener("login");
        session.executeLoginCommand("a1", "testimapnio@yahoo.com", "extraginger", loginListener);
        Assert.assertTrue(loginListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(loginListener.getTagResponse().isOK());

        final TestCommandListener selectListener = new TestCommandListener("select");
        session.executeSelectCommand("a2", "Inbox", selectListener);
        Assert.assertTrue(selectListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(selectListener.getTagResponse().isOK());

        final TestCommandListener searchListener = new TestCommandListener("search");
        session.executeSearchCommand("a3", false, "UNDELETED SINCE 8-Feb-2017", searchListener);
        Assert.assertTrue(searchListener.isDone(CLIENT_COMMAND_TIMEOUT_IN_SECONDS));
        Assert.assertTrue(searchListener.getTagResponse().isOK());
    }
}
