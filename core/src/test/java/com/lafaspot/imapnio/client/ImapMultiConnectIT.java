/**
 *
 */
package com.lafaspot.imapnio.client;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.avro.Schema;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.exception.IMAPSessionException;
import com.lafaspot.imapnio.listener.IMAPCommandListener;
import com.lafaspot.imapnio.listener.IMAPConnectionListener;
import com.lafaspot.logfast.logging.LogContext;
import com.lafaspot.logfast.logging.LogDataUtil;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger;
import com.lafaspot.logfast.logging.Logger.Level;
import com.lafaspot.logfast.logging.internal.LogPage;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * @author kraman
 *
 */
public class ImapMultiConnectIT {

    private IMAPClient theClient;
    private LogManager logManager;
    private Logger log;
    private Exception e = null; // new Exception("dummy exception");

    /**
     * @throws Exception failed data
     */
    @Test
    public void testConnectWithMultipleThreads() throws Exception {

        final int NTHREADS = 5;
        final ExecutorService executor = Executors.newFixedThreadPool(NTHREADS);
        final ImapClientWorker workers[] = new ImapClientWorker[NTHREADS];
        final String gmailServer = "imaps://imap.gmail.com:993";

        org.slf4j.Logger logbackLogger = LoggerFactory.getLogger(ImapMultiConnectIT.class);
        logbackLogger.debug("test ");

        for (int i = 0; i < NTHREADS; i++) {

            final IMAPSession session = theClient.createSession(new URI(gmailServer), new Properties(), new ConnectionListener(i),
                    logManager); //
            // logManager.getLogger(new LogContext("com.lafaspot.imapnio.client.ImapMultiConnectIT") {
            // }));
            log.debug("session created " + session, e);
            workers[i] = new ImapClientWorker(i, session, logManager.getLogger(new LogContext(
                    "com.lafaspot.imapnio.client.ImapMultiConnectIT") {
            }));
            executor.submit(workers[i]);
        }

        IMAPCommandListener listenerToSendSelect = new IMAPCommandListener() {

            @Override
            public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
                for (final IMAPResponse r : responses) {
                    log.info(" <-- " + r, e);
                }
            }

            @Override
            public void onMessage(IMAPSession session, IMAPResponse response) {
                log.info("onMessage " + response, e);
            }
        };

        Thread.sleep(5000);

        if (false) {
            final byte[] bytes = logManager.getBytes();
            final Schema schema = new Schema.Parser().parse(LogPage.SCHEMA_STR);
            final String json = binaryToJson(bytes, "--no-pretty", schema.toString());
            System.out.println("log " + json);
        }

    }

    private String binaryToJson(final byte[] avro, final String... options) throws UnsupportedEncodingException, Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream p = new PrintStream(new BufferedOutputStream(baos));

        // System.out.println(" +++ " + new String(avro));
        final List<String> args = new ArrayList<String>();
        args.addAll(Arrays.asList(options));
        args.add("-");

        //
        // new BinaryFragmentToJsonTool().run(new ByteArrayInputStream(avro), // stdin
        // p, // stdout
        // null, // stderr
        // args);
        return baos.toString("utf-8").replace("\r", "");

    }

    /**
     * Setup.
     */
    @BeforeClass
    public void setup() {
        final int threads = 5;
        theClient = new IMAPClient(threads);
        logManager = new LogManager(Level.DEBUG, 5);
        logManager.setLegacy(true);

        final LogDataUtil data = new LogDataUtil();
        final LogContext context = new LogContext("com.lafaspot.imapnio.client.ImapMultiConnectIT") {
        };

        log = logManager.getLogger(context);
        log.debug("setup done", e);

    }

    class ImapClientWorker implements Runnable {

        private final IMAPSession session;
        private final Logger log;
        private final int id;

        ImapClientWorker(int id, IMAPSession session, Logger log) {
            this.id = id;
            this.session = session;
            this.log = log;

            log.debug("worker got session" + session, null);
        }

        @Override
        public void run() {
            try {

                log.debug("trying to connect " + id + ", session" + session, e);
                session.connect();

            } catch (IMAPSessionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    class ConnectionListener implements IMAPConnectionListener {
        final int id;

        ConnectionListener(int id) {
            this.id = id;
        }

        @Override
        public void onConnect(IMAPSession session) {
            System.out.println("on connect " + id);
            log.debug("on connect " + id, e);
            log.debug("sending ID command " + id, e);
            try {
                session.executeIDCommand("t01-" + id, new String[] { "name", "roadrunner", "version", "1.0" }, new IMAPCommandListener() {

                    @Override
                    public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
                        log.debug(" recv resp " + id + ", " + tag, null);

                    }

                    @Override
                    public void onMessage(IMAPSession session, IMAPResponse response) {
                        log.debug(" recv msg " + id + ", " + response, null);

                    }
                });
            } catch (IMAPSessionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnect(IMAPSession session, Throwable cause) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onResponse(IMAPSession session, String tag, List<IMAPResponse> responses) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onMessage(IMAPSession session, IMAPResponse response) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onInactivityTimeout(IMAPSession session) {
            // TODO Auto-generated method stub

        }

    }

}
