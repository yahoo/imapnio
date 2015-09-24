/**
 *
 */
package com.lafaspot.imapnio.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLException;

import com.lafaspot.imapnio.channel.IMAPChannelFuture;
import com.lafaspot.imapnio.command.Argument;
import com.lafaspot.imapnio.command.ImapCommand;
import com.lafaspot.imapnio.exception.IMAPSessionException;
import com.lafaspot.imapnio.listener.IMAPCommandListener;
import com.lafaspot.imapnio.listener.IMAPConnectionListener;
import com.lafaspot.logfast.logging.LogContext;
import com.lafaspot.logfast.logging.LogDataUtil;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger;
import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * Defines one IMAP session.
 *
 * @author kraman
 *
 */
public class IMAPSession {

    /** logger. */
    private final Logger log;

    /**
     * List of capabilities returned by the server upon login.
     */
    protected Map<String, Boolean> capabilities;

    /** state of the IMAP session. */
    private final AtomicReference<IMAPSessionState> state = new AtomicReference<>(IMAPSessionState.DISCONNECTED);

    /** The IMAP channel. */
    private Channel channel;

    /** Event loop. */
    private final EventLoopGroup group;

    /** Client listener for connect/disconnect callback events. */
    private final IMAPConnectionListener connectionListener;

    /** Map to hold tag to listener. */
    private final ConcurrentHashMap<String, IMAPCommandListener> commandListeners;

    /** IMAP tag used for IDLE command. */
    private String idleTag;

    /** List of IMAPResposne object. */
    private final List<IMAPResponse> responses;

    /** The listener used for this session. */
    // private SessionListener clientListener; -- lafa was here

    /** Server to connect to. */
    private final URI serverUri;

    /** Bootstrap. */
    private final Bootstrap bootstrap;

    /**
     * Creates a IMAP session.
     *
     * @param sessionId
     *            identifier for the session
     * @param uri
     *            remote IMAP server URI
     * @param bootstrap
     *            the bootstrap
     * @param group
     *            the event loop group
     * @param listener
     *            the session listener
     * @param logManager
     *            the LogManager instance
     * @throws IMAPSessionException
     *             on SSL or connect failure
     */
    @SuppressWarnings({ "deprecation", "checkstyle:linelength" })
    public IMAPSession(@Nonnull final String sessionId, @Nonnull final URI uri, @Nonnull final Bootstrap bootstrap, @Nonnull final EventLoopGroup group,
                    @Nonnull final IMAPConnectionListener listener, @Nonnull final LogManager logManager) throws IMAPSessionException {
        LogContext context = new SessionLogContext("IMAPSession-" + uri.toASCIIString(), sessionId);
        log = logManager.getLogger(context);
        responses = new ArrayList<IMAPResponse>();
        commandListeners = new ConcurrentHashMap<String, IMAPCommandListener>();
        this.connectionListener = listener;
        this.group = group;
        this.serverUri = uri;
        this.bootstrap = bootstrap;
        // Initialize default state
        capabilities = new HashMap<String, Boolean>();

        final boolean ssl = uri.getScheme().toLowerCase().equals("imaps");

        try {
            // Configure SSL.
            SslContext sslCtx;
            if (ssl) {
            	sslCtx = SslContextBuilder.forClient().build();
                // sslCtx = SslContext.newClientContext(TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()));
            } else {
                sslCtx = null;
            }
            state.set(IMAPSessionState.DISCONNECTED);
            // Open channel using the bootstrap
            bootstrap.handler(new IMAPClientInitializer(this, sslCtx, uri.getHost(), uri.getPort()));
        } catch (final SSLException e1) {
            throw new IMAPSessionException("ssl exception", e1);
        } /*catch (final NoSuchAlgorithmException e2) {
            throw new IMAPSessionException("no such algo exception", e2);
        }*/
    }

    /**
     * Close socket connection to IMAP server. Must be closed by the client for book-keeping purposes.
     */
    protected void disconnect() {
        group.shutdownGracefully();
        channel.close();
    }

    /**
     * Connects to the remote server.
     *
     * @return the ChannelFuture object
     * @throws IMAPSessionException
     *             on connection failure
     */
    public IMAPChannelFuture connect() throws IMAPSessionException {
        if (state.get() != IMAPSessionState.DISCONNECTED) {
            throw new IMAPSessionException("Invalid state " + state.get());
        }
        final ChannelFuture connectFuture = bootstrap.connect(serverUri.getHost(), serverUri.getPort());
        try {
            this.channel = connectFuture.sync().channel();
        } catch (final InterruptedException e) {
            throw new IMAPSessionException(" ", e);
        }
        connectFuture.addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
                state.set(IMAPSessionState.CONNECT_SENT);
            }
        });
        return new IMAPChannelFuture(connectFuture);
    }

    /**
     * Execute an IDLE command against server. We do extra book-keeping for the IDLE command to keep track of our IDLEing state.
     *
     * @param tag IMAP tag to be used
     * @param listener the session listener
     * @throws IMAPSessionException when session is not connected
     * @return ChannelFuture the future object
     */
    public IMAPChannelFuture executeIdleCommand(final String tag, final IMAPCommandListener listener) throws IMAPSessionException {
        if (state.get() != IMAPSessionState.CONNECTED) {
            throw new IMAPSessionException("Sending IDLE in invalid state " + state.get());
        }
        final ChannelFuture future = executeCommand(new ImapCommand(tag, "IDLE", new Argument(), new String[] { "IDLE" }), listener);
        if (null != future) {
            future.addListener(new GenericFutureListener<ChannelFuture>() {
                @Override
                public void operationComplete(final ChannelFuture future) throws Exception {
                    state.set(IMAPSessionState.IDLE_SENT);
                    idleTag = tag;
                }
            });
        }
        return new IMAPChannelFuture(future);
    }

    /**
     * Sends a DONE command to get the session out of IDLE state.
     *
     * @param listener command listener
     * @return the future object
     * @throws IMAPSessionException when session is not in IDLE state
     */
    public IMAPChannelFuture executeDoneCommand(final IMAPCommandListener listener) throws IMAPSessionException {
        if (state.get() != IMAPSessionState.IDLING) {
            throw new IMAPSessionException("Sending DONE in invalid state " + state.get());
        }
        final ChannelFuture future = executeCommand(new ImapCommand("", "DONE", null, new String[] {}), listener);
        if (null != future) {
            future.addListener(new GenericFutureListener<ChannelFuture>() {
                @Override
                public void operationComplete(final ChannelFuture future) throws Exception {
                    state.set(IMAPSessionState.DONE_SENT);
                }
            });
        }
        return new IMAPChannelFuture(future);
    }

    /**
     * Execute the IMAP select command.
     *
     * @param tag IMAP tag to be used for this command
     * @param mailbox mailbox/label to select
     * @param listener the session listener
     * @return the future object
     * @throws IMAPSessionException when session is not connected
     */
    public IMAPChannelFuture executeSelectCommand(final String tag, final String mailbox, final IMAPCommandListener listener)
            throws IMAPSessionException {
        if (state.get() != IMAPSessionState.CONNECTED) {
            throw new IMAPSessionException("Sending SELECT in invalid state " + state.get());
        }

        final String b64Mailbox = BASE64MailboxEncoder.encode(mailbox);
        return new IMAPChannelFuture(executeCommand(new ImapCommand(tag, "SELECT", new Argument().addString(b64Mailbox), new String[] {}), listener));
    }

    /**
     * Execute the IMAP status command.
     *
     * @param tag IMAP tag to be used for this command
     * @param mailbox mailbox/folder to run status command on
     * @param items IMAP status elements
     * @param listener the session listener
     * @throws IMAPSessionException when session is invalid
     * @return the future object
     */
    public IMAPChannelFuture executeStatusCommand(final String tag, final String mailbox, final String[] items, final IMAPCommandListener listener)
            throws IMAPSessionException {
        if (state.get() != IMAPSessionState.CONNECTED) {
            throw new IMAPSessionException("Sending STATUS in invalid state " + state.get());
        }

        final String mailboxB64 = BASE64MailboxEncoder.encode(mailbox);

        final Argument args = new Argument();
        args.writeString(mailboxB64);

        final Argument itemArgs = new Argument();

        for (int i = 0, len = items.length; i < len; i++) {
            itemArgs.writeAtom(items[i]);
        }
        args.writeArgument(itemArgs);
        return new IMAPChannelFuture(executeCommand(new ImapCommand(tag, "STATUS", args, new String[] {}), listener));
    }

    /**
     * Execute the IMAP login command.
     *
     * @param tag IMAP tag to be used
     * @param username login username
     * @param password login password
     * @param listener the session listener
     * @return the future object
     * @throws IMAPSessionException when session is not connected
     */
    public IMAPChannelFuture executeLoginCommand(final String tag, final String username, final String password, final IMAPCommandListener listener)
            throws IMAPSessionException {
        if (state.get() != IMAPSessionState.CONNECTED) {
            throw new IMAPSessionException("Sending Login in invalid state " + state.get());
        }

        return new IMAPChannelFuture(executeCommand(
                        new ImapCommand(tag, "LOGIN", new Argument().addString(username).addString(password), new String[] { "auth=plain" }),
                        listener));
    }

    /**
     * Initiate an AUTHENTICATE XOAUTH2 command.
     *
     * @param tag IMAP tag to be used
     * @param oauth2Tok OAUTH token
     * @param listener the session listener
     * @return the future object
     * @throws IMAPSessionException when session is not connected
     */
    public IMAPChannelFuture executeOAuth2Command(final String tag, final String oauth2Tok, final IMAPCommandListener listener)
            throws IMAPSessionException {
        if (state.get() != IMAPSessionState.CONNECTED) {
            throw new IMAPSessionException("Sending OAuth2 in invalid state " + state.get());
        }

        final ChannelFuture future = executeCommand(
                        new ImapCommand(tag, "AUTHENTICATE XOAUTH2", new Argument().addString(oauth2Tok), new String[] { "auth=xoauth2" }), listener);
        return new IMAPChannelFuture(future);
    }

    /**
     * Initiate a SASL based XOAUTH2 command.
     *
     * @param tag IMAP tag to be used
     * @param user user id
     * @param token oauth token
     * @param listener the session listener
     * @return the future object
     * @throws IMAPSessionException when session is not connected
     */
    public IMAPChannelFuture executeSASLXOAuth2(final String tag, final String user, final String token, final IMAPCommandListener listener)
            throws IMAPSessionException {
        if (state.get() != IMAPSessionState.CONNECTED) {
            throw new IMAPSessionException("Sending XOauth2 in invalid state " + state.get());
        }

        final StringBuffer buf = new StringBuffer();
        buf.append("user=").append(user).append("\u0001").append("auth=Bearer ").append(token).append("\u0001").append("\u0001");
        final String encOAuthStr = Base64.getEncoder().encodeToString(buf.toString().getBytes(StandardCharsets.UTF_8));
        if (log.isDebug()) {
            log.debug(new LogDataUtil().set(this.getClass(), "XOAUTH2", encOAuthStr), null);
        }
        return executeOAuth2Command(tag, encOAuthStr, listener);
    }

    /**
     * Execute the IMAP logout command.
     *
     * @param tag IMAP tag to be used
     * @param listener the session listener
     * @return the future object
     * @throws IMAPSessionException when session is not connected
     */
    public IMAPChannelFuture executeLogoutCommand(final String tag, final IMAPCommandListener listener) throws IMAPSessionException {
        if (state.get() != IMAPSessionState.CONNECTED) {
            throw new IMAPSessionException("Sending Logout in invalid state " + state.get());
        }

        final ChannelFuture future = executeCommand(new ImapCommand(tag, "LOGOUT", new Argument(), new String[] {}), listener);
        return new IMAPChannelFuture(future);
    }

    /**
     * Execute a IMAP capability command.
     *
     * @param tag IMAP tag to be used
     * @param listener the session listener
     * @return the future object
     * @throws IMAPSessionException when session is not connected
     */
    public IMAPChannelFuture executeCapabilityCommand(final String tag, final IMAPCommandListener listener) throws IMAPSessionException {
        if (state.get() != IMAPSessionState.CONNECTED) {
            throw new IMAPSessionException("Sending CAPABILITY in invalid state " + state.get());
        }

        return new IMAPChannelFuture(executeCommand(new ImapCommand(tag, "CAPABILITY", new Argument(), new String[] {}), listener));
    }

    /**
     * Execute a IMAP append command.
     *
     * @param tag IMAP tag to be used
     * @param labelName label/folder where the message is to be saved
     * @param flags message flags
     * @param size message size
     * @param listener the session listener
     * @return the future object
     * @throws IMAPSessionException when session is not connected
     */
    public IMAPChannelFuture executeAppendCommand(final String tag, final String labelName, final String flags, final String size,
            final IMAPCommandListener listener) throws IMAPSessionException {
        if (state.get() != IMAPSessionState.CONNECTED) {
            throw new IMAPSessionException("Sending APPEND in invalid state " + state.get());
        }

        return new IMAPChannelFuture(executeCommand(new ImapCommand(tag, "APPEND",
                        new Argument().addString(labelName).addLiteral(flags).addLiteral("{" + size + "}"), new String[] { "IMAP4REV1" }), listener));
    }

    /**
     * Execute a IMAP raw command.
     *
     * @param rawText
     *            the raw text to be sent to the remote IMAP server
     * @return the future object
     */
//    public IMAPChannelFuture executeRawTextCommand(final String rawText) {
//        return new IMAPChannelFuture(executeCommand(new ImapCommand("", rawText, null, new String[] {}), connectionListener));
//    }

    /**
     * Execute a IMAP NOOP command.
     *
     * @param tag IMAP tag to be used
     * @param listener the session listener
     * @return the future object
     * @throws IMAPSessionException when session is not connected
     */
    public IMAPChannelFuture executeNOOPCommand(final String tag, final IMAPCommandListener listener) throws IMAPSessionException {
        if (state.get() != IMAPSessionState.CONNECTED) {
            throw new IMAPSessionException("Sending NOOP in invalid state " + state.get());
        }

        return new IMAPChannelFuture(executeCommand(new ImapCommand(tag, "NOOP", new Argument(), new String[] {}), listener));
    }

    /**
     * Execute a IMAP LIST command.
     *
     * @param tag IMAP tag to be used
     * @param reference name of mailbox or level of mailbox hierarchy
     * @param name mailbox name
     * @param listener the session listener
     * @return the future object
     * @throws IMAPSessionException when session is not connected
     */
    public IMAPChannelFuture executeListCommand(final String tag, final String reference, final String name, final IMAPCommandListener listener)
            throws IMAPSessionException {
        if (state.get() != IMAPSessionState.CONNECTED) {
            throw new IMAPSessionException("Sending LIST in invalid state " + state.get());
        }

        final String b64Mailbox = BASE64MailboxEncoder.encode(name);
        final Argument args = new Argument();
        args.writeString(reference);
        args.writeString(b64Mailbox);
        return new IMAPChannelFuture(executeCommand(new ImapCommand(tag, "LIST", args, new String[] {}), listener));
     }

    /**
     * Format and sends the IMAP command to remote server.
     *
     * @param method
     *            IMAP command to be sent
     * @param listener
     *            the session listener
     * @return the future object
     */
    private ChannelFuture executeCommand(final ImapCommand method, final IMAPCommandListener listener) {

        final String args = (method.getArgs() != null ? method.getArgs().toString() : "");
        final String line = method.getTag() + (!method.getTag().isEmpty() ? " " : "") + method.getCommand() + (args.length() > 0 ? " " + args : "");
        if (log.isDebug()) {
            log.debug(line, null);
        }

        final ChannelFuture lastWriteFuture = this.channel.writeAndFlush(line + "\r\n");
        if (null != listener) {
            commandListeners.put(method.getTag(), listener);
        }

        return lastWriteFuture;
    }

    /**
     * Accumulate multi-line IMAP responses before giving the client callback.
     *
     * @param response
     *            IMAP response to be queued
     */
    void addResponse(final IMAPResponse response) {
        responses.add(response);
    }

    /**
     * Get the IMAP tag corresponding to the IDLE command.
     *
     * @return the IDLE tag
     */
    public String getIdleTag() {
        return idleTag;
    }

    /**
     * Return the list of IMAP response objects.
     *
     * @return IMAPResponse list
     */
    protected List<IMAPResponse> getResponseList() {
        return responses;
    }

    /**
     * Reset the accumulated IMAP response list.
     */
    protected void resetResponseList() {
        responses.clear();
    }

    /**
     * Return the current session state.
     *
     * @return state
     */
    AtomicReference<IMAPSessionState> getState() {
        return state;
    }


    /**
     * Returns the client listener registered for a specific tag.
     *
     * @param tag
     *            get listener for this tag
     * @return ClientListener registered for the tag
     */
    protected IMAPCommandListener getClientListener(final String tag) {
        return commandListeners.get(tag);
    }

    /**
     * Returns the connect/disconnect client listener.
     *
     * @return ClientListener
     */
    protected IMAPConnectionListener getConnectionListener() {
        return connectionListener;
    }

    /**
     * Remove the listener after command for that tag is processed.
     *
     * @param tag
     *            remove listener for this tag
     * @return the removed listener
     */
    protected IMAPCommandListener removeClientListener(final String tag) {
        return commandListeners.remove(tag);
    }

    /**
     * Reset this IMAP session.
     */
    protected void resetSession() {
        resetResponseList();
        commandListeners.clear();
    }

    /**
     * This method is package protected.
     *
     * @return logger
     */
    Logger getLogger() {
        return log;
    }
}
