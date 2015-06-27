/**
 *
 */
package com.yahoo.mail.imapnio.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.LoggerFactory;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.mail.imapnio.client.command.Argument;
import com.yahoo.mail.imapnio.client.command.IMAPCommand;

/**
 * Defines one IMAP session.
 *
 * @author kraman
 *
 */
public class IMAPSession {

    /** logger. */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IMAPSession.class);

    /**
     * List of capabilities returned by the server upon login.
     */
    protected Map<String, Boolean> capabilities;

    /** state of the IMAP session. */
    private IMAPSessionState state;

    private Channel channel;

    private EventLoopGroup group;


    private final ConcurrentHashMap<String, IMAPClientListener> listeners;

    private final List<IMAPResponse> responses;

    private IMAPClientListener idleListener;

    private IMAPClientListener oauth2LoginListener;

    private String oauth2Tok;

    /**
     * Creates a IMAP session.
     *
     * @param uri
     *            - remote IMAP server URI
     * @param bootstrap
     *            - the bootstrap
     * @param executor
     *            - the executor
     * @throws SSLException
     * @throws NoSuchAlgorithmException
     * @throws InterruptedException
     */
    public IMAPSession(URI uri, Bootstrap bootstrap, EventLoopGroup group) throws SSLException,
            NoSuchAlgorithmException, InterruptedException {

        boolean ssl = uri.getScheme().toLowerCase().equals("imaps");
        // Configure SSL.
        SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContext.newClientContext(TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())); // PKIX
        } else {
            sslCtx = null;
        }

        // Save any other metadata

        // Initialize default state
        capabilities = new HashMap<String, Boolean>();
        state = IMAPSessionState.Connected;
        // setIdleState(IMAPNIOClientSessionIdleState.NotIdle);
        this.group = group;

        // Open channel using the bootstrap
        bootstrap.channel(NioSocketChannel.class).handler(new IMAPClientInitializer(this, sslCtx, uri.getHost(), uri.getPort()))
                .group(group);
        this.channel = bootstrap.connect(uri.getHost(), uri.getPort()).sync().channel();
        listeners = new ConcurrentHashMap<String, IMAPClientListener>();
        responses = new ArrayList<IMAPResponse>();
    }


    /**
     * Close socket connection to IMAP server. Must be closed by the client for book-keeping purposes.
     */
    protected void disconnect() {
        group.shutdownGracefully();
        channel.close();
    }

    public void setCapabilities (Map<String, Boolean> cap) {
        this.capabilities = cap;
    }

    /**
     * Execute an IDLE command against server. We do extra book-keeping for the IDLE command to keep track of our IDLEing state.
     *
     * @param tag
     */
    public ChannelFuture executeIdleCommand(String tag, IMAPClientListener listener) throws InterruptedException {
        idleListener = listener;
        ChannelFuture future = executeCommand(new IMAPCommand(tag, "IDLE", new Argument(), new String[] { "IDLE" }), listener);
        if (null != future) {
            future.addListener(new GenericFutureListener<ChannelFuture>() {
                public void operationComplete(ChannelFuture future) throws Exception {
                    state = IMAPSessionState.IDLE_REQUEST;
                }
            });
        }
        return future;
    }

    public ChannelFuture executeSelectCommand(String tag, String mailbox, IMAPClientListener listener) throws InterruptedException {
        mailbox = BASE64MailboxEncoder.encode(mailbox);
        return executeCommand(new IMAPCommand(tag, "SELECT", new Argument().addString(mailbox), new String[] {}), listener);
    }

    public ChannelFuture executeStatusCommand(String tag, String mailbox, String[] items, IMAPClientListener listener) throws InterruptedException {
        mailbox = BASE64MailboxEncoder.encode(mailbox);

        Argument args = new Argument();
        args.writeString(mailbox);

        Argument itemArgs = new Argument();
        // if (args == null)
        // args = Status.standardItems;

        for (int i = 0, len = items.length; i < len; i++)
            itemArgs.writeAtom(items[i]);
        args.writeArgument(itemArgs);

        return executeCommand(new IMAPCommand(tag, "STATUS", args, new String[] {}), listener);
    }

    /**
     * Execute an authentication command against server.
     *
     * @param tag
     *            An auth command to execute in the session.
     */
    public ChannelFuture executeLoginCommand(String tag, String username, String password, IMAPClientListener listener) throws InterruptedException {
        return executeCommand(new IMAPCommand(tag, "LOGIN", new Argument().addString(username).addString(password), new String[] { "auth=plain" }),
                listener);
    }

    /**
     * Initiate an AUTHENTICATE XOAUTH2 command.
     *
     * @param tag
     * @param oauth2Tok
     * @param listener
     * @return
     * @throws InterruptedException
     */
    public ChannelFuture executeOAuth2Command(final String tag, final String oauth2Tok, final IMAPClientListener listener)
            throws InterruptedException {
        this.oauth2Tok = oauth2Tok;
        this.oauth2LoginListener = listener;
        ChannelFuture future = executeCommand(new IMAPCommand(tag, "AUTHENTICATE XOAUTH2", new Argument(), new String[] { "auth=xoauth" }), null);
        future.addListener(new GenericFutureListener<ChannelFuture>() {
            public void operationComplete(ChannelFuture future) throws Exception {
                state = IMAPSessionState.OAUTH2_INIT;
            }
        });
        return future;
    }

    /**
     * Execute logout command.
     *
     * @param tag
     *            IMAP tag used for this command
     * @param listener
     *            client listener to get callback
     * @return
     * @throws InterruptedException
     */
    public ChannelFuture executeLogoutCommand(String tag, IMAPClientListener listener) throws InterruptedException {
        return executeCommand(new IMAPCommand(tag, "LOGOUT", new Argument(), new String[] {}), listener);
    }

    /**
     * Execute a CAPABILITY command.
     *
     * @param tag
     */
    public ChannelFuture executeCapabilityCommand(String tag, IMAPClientListener listener) throws InterruptedException {
        return executeCommand(new IMAPCommand(tag, "CAPABILITY", new Argument(), new String[] {}), listener);
    }

    /**
     * Send APPEND command.
     *
     * @param tag
     *            tag to be used for this command
     * @param labelName
     *            name of the label/folder where the message should be appended to
     * @param flags
     *            message flags
     * @param size
     *            message size
     * @param listener
     *            client listener to get callback
     * @return future
     * @throws InterruptedException
     */
    public ChannelFuture executeAppendCommand(String tag, String labelName, String flags, String size, IMAPClientListener listener)
            throws InterruptedException {
        return executeCommand(new IMAPCommand(tag, "APPEND", new Argument().addString(labelName).addLiteral(flags).addLiteral("{" + size + "}"),
                new String[] { "IMAP4REV1" }), listener);
    }

    /**
     * Sending raw text, for APPEND
     *
     * @param rawText
     *            raw text to be written to the channel
     * @param listener
     *            client listener to get callback
     * @return future
     * @throws InterruptedException
     */
    public ChannelFuture executeRawTextCommand(String rawText, IMAPClientListener listener) throws InterruptedException {
        return executeCommand(new IMAPCommand("", rawText, null, new String[] {}), listener);
    }

    /**
     * Execute a command against the server. To run a command you should go through specific methods like `executeLoginCommand`.
     *
     * @param method
     */
    private ChannelFuture executeCommand(IMAPCommand method, IMAPClientListener listener) throws InterruptedException {
        // First, check that capabilities match required capabilities of command
        String[] capabilitiesRequired = method.getCapabilities();
        for (String requiredCapability : capabilitiesRequired) {
            if (!this.hasCapability(requiredCapability)) {
                log.error("Do not have required capability: " + requiredCapability);
            }
        }

        // Second, construct command using a Protocol proxy
        String args = (method.getArgs() != null ? method.getArgs().toString() : "");
        String line = method.getTag() + (method.getTag() != "" ? " " : "") + method.getCommand() + (args.length() > 0 ? " " + args : "");
        log.info("--> " + line);

        // Fourth, execute command.
        // TODO: make sure that everything is written before closing channel. (in whole class, not just here)
        ChannelFuture lastWriteFuture = this.channel.writeAndFlush(line + "\r\n");
        if (null != listener) {
            listeners.put(method.getTag(), listener);
        }

        // Fifth, wait until all messages are flushed
        if (lastWriteFuture != null) {
            // lastWriteFuture.sync();
        }

        return lastWriteFuture;
    }

    /**
     * Returns true if this server broadcasts this capability. Capability is case-insensitive.
     *
     * @param capability
     * @return
     */
    public boolean hasCapability(String capability) {
        return this.capabilities.containsKey(capability.toLowerCase()) && this.capabilities.get(capability.toLowerCase()).equals(true);
    }

    /**
     * Remove and return the future listener. To be used by client after receiving the response from remote client.
     *
     * @param tag
     *            - IMAP tag used by the command
     * @return FutureTask - the listener
     */
    public IMAPClientListener removeListener(String tag) {
        if (null != tag) {
            return listeners.remove(tag);
        }
        return null;
    }

    public void addResponse(IMAPResponse response) {
        responses.add(response);
    }

    public List<IMAPResponse> getResponseList() {
        return responses;
    }

    public void resetResponseList() {
        responses.clear();
    }

    public IMAPSessionState getState() {
        return state;
    }

    public void setState(IMAPSessionState state) {
        this.state = state;
    }

    public IMAPClientListener getIdleListener() {
        return idleListener;
    }

    public void onOAuth2Response(IMAPResponse resp) {
        if (resp.isOK()) {
            try {
                executeRawTextCommand(oauth2Tok, new IMAPClientListener() {
                    public void onResponse(String tag, List<IMAPResponse> responses) {
                        log.info(" oauth2 raw response " + responses.size());
                        if (responses.size() > 0) {
                            IMAPResponse resp = responses.get(0);
                            log.info(" oauth2 raw response " + resp);
                            if (resp.isOK()) {
                                setState(IMAPSessionState.LoggedIn);
                                if (oauth2LoginListener != null) {
                                    oauth2LoginListener.onOAuth2LoggedIn(Arrays.asList(resp));
                                }
                            }
                        }

                    }

                    public void onIdleEvent(List<IMAPResponse> messages) {
                        // TODO Auto-generated method stub

                    }

                    public void onOAuth2LoggedIn(List<IMAPResponse> msgs) {
                        // TODO Auto-generated method stub

                    }
                });
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
