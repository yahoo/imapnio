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
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    /** Socket connect timeout. */
    public static final String CONFIG_CONNECTION_TIMEOUT_KEY = "mail.imap.connectiontimeout";
    /** IMAP command timeout. */
    public static final String CONFIG_IMAP_TIMEOUT_KEY = "mail.imap.timeout";

    /** Socket connect timeout value. */
    public static final String CONFIG_CONNECTION_TIMEOUT_VALUE = "60000";

    /** IMAP command timeout value. */
    public static final String CONFIG_IMAP_TIMEOUT_VALUE = "60000";

    /** config key for inactivity timeout value - in seconds. */
    public static final String CONFIG_IMAP_INACTIVITY_TIMEOUT_KEY = "mail.imap.inactivity";

    /** Configuration for this session. */
    private final Properties config;

    /** logger. */
    private final Logger log;

    /** state of the IMAP session. */
    private final AtomicReference<IMAPSessionState> state = new AtomicReference<>(IMAPSessionState.DISCONNECTED);

    /** The IMAP channel. */
    private final AtomicReference<Channel> channelRef = new AtomicReference<Channel>();

    /** Client listener for connect/disconnect callback events. */
    private final IMAPConnectionListener connectionListener;

    /** Map to hold tag to listener. */
    private final ConcurrentHashMap<String, IMAPCommandListener> commandListeners;

    /** IMAP tag used for the currently running command. */
    private final AtomicReference<String> currentTagRef = new AtomicReference<String>();

    /** List of IMAPResposne object. */
    private final List<IMAPResponse> responses = new ArrayList<IMAPResponse>();

    /** The listener used for this session. */
    // private SessionListener clientListener; -- lafa was here

    /** Server to connect to. */
    private final URI serverUri;

    /** Bootstrap. */
    private final Bootstrap bootstrap;

    /**
     * Creates a IMAP session.
     *
     * @param sessionId identifier for the session
     * @param uri remote IMAP server URI
     * @param configVal configuration for this session
     * @param bootstrap the bootstrap
     * @param group the event loop group
     * @param listener the session listener
     * @param logManager the LogManager instance
     * @throws IMAPSessionException on SSL or connect failure
     */
    public IMAPSession(@Nonnull final String sessionId, @Nonnull final URI uri, @Nonnull final Properties configVal,
            @Nonnull final Bootstrap bootstrap,
            @Nonnull final EventLoopGroup group, @Nonnull final IMAPConnectionListener listener, @Nonnull final LogManager logManager)
            throws IMAPSessionException {
        LogContext context = new SessionLogContext("IMAPSession-" + uri.toASCIIString(), sessionId);
        log = logManager.getLogger(context);
        commandListeners = new ConcurrentHashMap<String, IMAPCommandListener>();
        this.connectionListener = listener;
        this.serverUri = uri;
        this.config = configVal;
        this.bootstrap = bootstrap;
        // default values
        if (!config.containsKey(CONFIG_CONNECTION_TIMEOUT_KEY)) {
            config.put(CONFIG_CONNECTION_TIMEOUT_KEY, CONFIG_CONNECTION_TIMEOUT_VALUE);
        }
        if (!config.containsKey(CONFIG_IMAP_TIMEOUT_KEY)) {
            config.put(CONFIG_IMAP_TIMEOUT_KEY, CONFIG_IMAP_TIMEOUT_VALUE);
        }

        final boolean ssl = uri.getScheme().toLowerCase().equals("imaps");

        try {
            // Configure SSL.
            SslContext sslCtx;
            if (ssl) {
            	sslCtx = SslContextBuilder.forClient().build();
            } else {
                sslCtx = null;
            }
            state.set(IMAPSessionState.DISCONNECTED);
            // Open channel using the bootstrap
            bootstrap.handler(new IMAPClientInitializer(this, sslCtx, uri.getHost(), uri.getPort()));
        } catch (final SSLException e1) {
            throw new IMAPSessionException("ssl exception", e1);
        }
    }

    /**
     * Close socket connection to IMAP server. Must be closed by the client for book-keeping purposes.
     */
    public void disconnect() {
        final Channel channel = channelRef.get();
        if (null != channel) {
            channel.close();
        }
        channelRef.set(null);
    }

    /**
     * Connects to the remote server.
     *
     * @param localAddress the local network interface to use
     * @return the ChannelFuture object
     * @throws IMAPSessionException on connection failure
     */
    public IMAPChannelFuture connect(@Nullable final InetSocketAddress localAddress) throws IMAPSessionException {
        if (!state.compareAndSet(IMAPSessionState.DISCONNECTED, IMAPSessionState.CONNECT_SENT)) {
            throw new IMAPSessionException("Invalid state " + state.get());
        }
        final ChannelFuture connectFuture;
        if (null != localAddress) {
            final InetSocketAddress remoteAddress = new InetSocketAddress(serverUri.getHost(), serverUri.getPort());
            connectFuture = bootstrap.connect(remoteAddress, localAddress);
        } else {
            connectFuture = bootstrap.connect(serverUri.getHost(), serverUri.getPort());
        }

        //

        final IMAPSession thisSession = this;
        connectFuture.addListener(new GenericFutureListener<Future<? super Void>>() {

            @Override
            public void operationComplete(final Future<? super Void> future) throws Exception {
                if (future.isSuccess()) {
                    if (!getState().compareAndSet(IMAPSessionState.CONNECT_SENT, IMAPSessionState.CONNECTED)) {
                        getLogger().error("Connect success in invalid state " + getState().get().name(), null);
                        return;
                    }

                    if (null != getConfig().getProperty(IMAPSession.CONFIG_IMAP_INACTIVITY_TIMEOUT_KEY)) {
                        final String strVal = getConfig().getProperty(IMAPSession.CONFIG_IMAP_INACTIVITY_TIMEOUT_KEY);
                        int inactivityTimeout = Integer.parseInt(strVal);
                        final int sixtySecs = 60;
                        final int oneSec = 1;
                        // check for range and default
                        if (inactivityTimeout > sixtySecs || inactivityTimeout < oneSec) {
                            inactivityTimeout = sixtySecs;
                        }
                        connectFuture.channel().pipeline().addLast("idleStateHandler", new IdleStateHandler(0, 0, inactivityTimeout));
                        connectFuture.channel().pipeline().addLast("inactivityHandler", new IMAPClientInactivityHandler(thisSession));
                    }

                    connectFuture.channel().pipeline().addLast(new IMAPClientRespHandler(thisSession));
                    connectFuture.channel().pipeline().addLast(new IMAPChannelListener(thisSession));

                    channelRef.set(connectFuture.sync().channel());
                    if (null != connectionListener) {
                        connectionListener.onConnect(thisSession);
                    }

                } else {
                    if (null != connectionListener) {
                        connectionListener.onDisconnect(thisSession, new Throwable("connect failure"));
                    }
                }
            }
        });

        return new IMAPChannelFuture(connectFuture);
    }

    /**
     * Connects to the remote server.
     *
     * @return the ChannelFuture object
     * @throws IMAPSessionException on connection failure
     */
    public IMAPChannelFuture connect() throws IMAPSessionException {
        return connect(null);
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
        if (!state.compareAndSet(IMAPSessionState.CONNECTED, IMAPSessionState.IDLE_SENT)) {
            throw new IMAPSessionException("Sending IDLE in invalid state " + state.get());
        }
        final ChannelFuture future = executeCommand(new ImapCommand(tag, "IDLE", new Argument(), new String[] { "IDLE" }), listener);
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
        if (!state.compareAndSet(IMAPSessionState.IDLING, IMAPSessionState.DONE_SENT)) {
            throw new IMAPSessionException("Sending DONE in invalid state " + state.get().name());
        }
        final ChannelFuture future = executeCommand(new ImapCommand("", "DONE", null, new String[] {}), listener);
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
            throw new IMAPSessionException("Sending STATUS in invalid state " + state.get().name());
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
            throw new IMAPSessionException("Sending Login in invalid state " + state.get().name());
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

        final Argument args = new Argument();
        args.addLiteral(oauth2Tok);
        final ImapCommand cmd = new ImapCommand(tag, "AUTHENTICATE XOAUTH2", args, new String[] { "auth=xoauth2" });

        final ChannelFuture future = executeCommand(cmd, listener);
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
     * @param rawText the raw text to be sent to the remote IMAP server
     * @return the future object
     * @throws IMAPSessionException when channel is invalid
     */
    public IMAPChannelFuture executeRawTextCommand(final String rawText) throws IMAPSessionException {
        return new IMAPChannelFuture(executeCommand(new ImapCommand("", rawText, null, new String[] {}), null));
    }

    /**
     * Execute a IMAP raw command handling tag and listener.
     *
     * @param tag IMAP tag to be used
     * @param rawText the raw text to be sent to the remote IMAP server
     * @param listener the session listener
     * @return the future object
     * @throws IMAPSessionException when channel is invalid
     */
    public IMAPChannelFuture executeTaggedRawTextCommand(final String tag, final String rawText,
            final IMAPCommandListener listener) throws IMAPSessionException {
        if (state.get() != IMAPSessionState.CONNECTED) {
            throw new IMAPSessionException("Sending raw command in invalid state " + state.get());
        }

        return new IMAPChannelFuture(executeCommand(new ImapCommand(tag, rawText, null, new String[] {}), listener));
    }

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
     * Sends out an IMAP ID command.
     *
     * @param tag IMAP tag to be used with this command
     * @param items arguments for ID command
     * @param listener the callback
     * @return future object
     * @throws IMAPSessionException when session is not connected
     */
    public IMAPChannelFuture executeIDCommand(final String tag, final String[] items, final IMAPCommandListener listener)
            throws IMAPSessionException {
        if (state.get() != IMAPSessionState.CONNECTED) {
            throw new IMAPSessionException("Sending ID in invalid state " + state.get());
        }

        final Argument imapArgs = new Argument();
        final Argument itemArgs = new Argument();
        for (int i = 0, len = items.length; i < len; i++) {
            itemArgs.writeNString(items[i]);
        }
        imapArgs.writeArgument(itemArgs);
        return new IMAPChannelFuture(executeCommand(new ImapCommand(tag, "ID", imapArgs, new String[] {}), listener));
    }

    /**
     * Format and sends the IMAP command to remote server.
     *
     * @param method
     *            IMAP command to be sent
     * @param listener
     *            the session listener
     * @return the future object
     * @throws IMAPSessionException when channel is invalid
     */
    private ChannelFuture executeCommand(final ImapCommand method, final IMAPCommandListener listener) throws IMAPSessionException {

        final String args = (method.getArgs() != null ? method.getArgs().toString() : "");
        final String line = method.getTag() + (!method.getTag().isEmpty() ? " " : "") + method.getCommand() + (args.length() > 0 ? " " + args : "");
        if (log.isDebug()) {
            log.debug(line, null);
        }

        final Channel channel = this.channelRef.get();
        if (null != channel) {
            final ChannelFuture lastWriteFuture = channel.writeAndFlush(line + "\r\n");
            if (null != listener && !method.getTag().isEmpty()) {
                final String tag = method.getTag();
                currentTagRef.set(tag);
                commandListeners.put(tag, listener);
            }
            return lastWriteFuture;
        }
        throw new IMAPSessionException("Invalid channel closed");
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
    protected IMAPCommandListener getCommandListener(final String tag) {
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
    protected IMAPCommandListener removeCommandListener(final String tag) {
        return commandListeners.remove(tag);
    }

    /**
     * Return the reference to the current tag being used.
     *
     * @return current tag ref
     */
    AtomicReference<String> getCurrentTagRef() {
        return currentTagRef;
    }

    /**
     * This method is package protected.
     *
     * @return logger
     */
    Logger getLogger() {
        return log;
    }

    /**
     * Return IMAPSession configuration.
     *
     * @return session configuration
     */
    Properties getConfig() {
        return config;
    }
}
