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

import javax.net.ssl.SSLException;

import org.slf4j.LoggerFactory;

import com.lafaspot.imapnio.command.Argument;
import com.lafaspot.imapnio.command.ImapCommand;
import com.lafaspot.imapnio.exception.ImapSessionException;
import com.lafaspot.imapnio.listener.ClientListener;
import com.lafaspot.imapnio.listener.SessionListener;
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
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IMAPSession.class);

    /**
     * List of capabilities returned by the server upon login.
     */
    protected Map<String, Boolean> capabilities;

    /** state of the IMAP session. */
    private IMAPSessionState state;

    /** The IMAP channel. */
    private Channel channel;

    /** Event loop. */
    private EventLoopGroup group;

    /** Client listener for connect/disconnect callback events.*/
    private final SessionListener listener;

    /** Map to hold tag to listener. */
    private final ConcurrentHashMap<String, SessionListener> listeners;
    
    /** IMAP tag used for IDLE command. */
    private String idleTag;

    /** List of IMAPResposne object. */
    private final List<IMAPResponse> responses;
    
    /** The listener used for thos session. */
    private SessionListener clientListener;
    

    /**
     * Creates a IMAP session.
     *
     * @param uri remote IMAP server URI
     * @param bootstrap the bootstrap
     * @param group the event loop group
     * @param listener the session listener
     * @throws ImapSessionException on SSL or connect failure
     */
	public IMAPSession(final URI uri, final Bootstrap bootstrap, final EventLoopGroup group, 
			final SessionListener listener) throws ImapSessionException {
		responses = new ArrayList<IMAPResponse>();
		listeners = new ConcurrentHashMap<String, SessionListener>();
		this.listener = listener;
		this.group = group;
		// Initialize default state
		capabilities = new HashMap<String, Boolean>();

		boolean ssl = uri.getScheme().toLowerCase().equals("imaps");

		try {
			// Configure SSL.
			SslContext sslCtx;
			if (ssl) {
				sslCtx = SslContextBuilder.forClient().build();				
			} else {
				sslCtx = null;
			}

			// Save any other metadata

			setState(IMAPSessionState.ConnectRequest);

			// Open channel using the bootstrap
			bootstrap
					.handler(
							new IMAPClientInitializer(this, sslCtx, uri
									.getHost(), uri.getPort())); 
			ChannelFuture connectFuture = bootstrap.connect(uri.getHost(), uri.getPort());
			this.channel = connectFuture.sync().channel();
			connectFuture.addListener(new GenericFutureListener<ChannelFuture>() {

				public void operationComplete(final ChannelFuture future)
						throws Exception {
					setState(IMAPSessionState.ConnectRequest);
				}
			});
		} catch (SSLException e1) {
			throw new ImapSessionException("ssl exception", e1);
		} catch (InterruptedException e2) {
			throw new ImapSessionException("connect failed", e2);
		}
	}


    /**
     * Close socket connection to IMAP server. Must be closed by the client for book-keeping purposes.
     */
    protected void disconnect() {
        group.shutdownGracefully();
        channel.close();
    }


    /**
     * Execute an IDLE command against server. We do extra book-keeping for the IDLE command to keep track of our IDLEing state.
     *
     * @param tag IMAP tag to be used
     * @param listener the session listener
     * @return ChannelFuture the future object
     */
	public ChannelFuture executeIdleCommand(final String tag, final SessionListener listener) {
		ChannelFuture future = executeCommand(new ImapCommand(tag, "IDLE",
				new Argument(), new String[] { "IDLE" }), listener);
		if (null != future) {
			future.addListener(new GenericFutureListener<ChannelFuture>() {
				public void operationComplete(final ChannelFuture future)
						throws Exception {
					setState(IMAPSessionState.IDLE_REQUEST);
					idleTag = tag;
				}
			});
		}
		return future;
	}

	/**
	 * Execute the IMAP select command.
	 * @param tag IMAP tag to be used for this command
	 * @param mailbox mailbox/label to select
	 * @param listener the session listener
	 * @return the future object
	 */
    public ChannelFuture executeSelectCommand(final String tag, final String mailbox, final SessionListener listener) {
        String b64Mailbox = BASE64MailboxEncoder.encode(mailbox);
        return executeCommand(new ImapCommand(tag, "SELECT", new Argument().addString(b64Mailbox), new String[] {}), listener);
    }

    /**
     * Execute the IMAP status command.
     * @param tag IMAP tag to be used for this command
     * @param mailbox mailbox/folder to run status command on
     * @param items IMAP status elements
     * @param listener the session listener
     * @return the future object
     */
    public ChannelFuture executeStatusCommand(final String tag, final String mailbox, final String[] items, final SessionListener listener) {
        String mailboxB64 = BASE64MailboxEncoder.encode(mailbox);

        Argument args = new Argument();
        args.writeString(mailboxB64);

        Argument itemArgs = new Argument();

        for (int i = 0, len = items.length; i < len; i++) {
			itemArgs.writeAtom(items[i]);
		}
        args.writeArgument(itemArgs);

    	setState(IMAPSessionState.Connected);
        return executeCommand(new ImapCommand(tag, "STATUS", args, new String[] {}), listener);
    }

    /**
     * Execute the IMAP login command.
     * @param tag IMAP tag to be used
     * @param username login username
     * @param password login password
     * @param listener the session listener
     * @return the future object
     */
    public ChannelFuture executeLoginCommand(final String tag, final String username, final String password, final SessionListener listener) {
        return executeCommand(new ImapCommand(tag, "LOGIN", 
        		new Argument().addString(username).addString(password), new String[] { "auth=plain" }), listener);
    }

    /**
     * Initiate an AUTHENTICATE XOAUTH2 command.
     *
     * @param tag IMAP tag to be used
     * @param oauth2Tok OAUTH token
     * @param listener the session listener
     * @return the future object
     */
    public ChannelFuture executeOAuth2Command(final String tag, final String oauth2Tok, final SessionListener listener) {
        ChannelFuture future = executeCommand(new ImapCommand(tag, "AUTHENTICATE XOAUTH2", 
        		new Argument().addString(oauth2Tok), new String[] { "auth=xoauth2" }), listener);
        return future;
    }
    
    /**
     * Initiate a SASL based XOAUTH2 command.
     * @param tag IMAP tag to be used
     * @param user user id
     * @param token oauth token
     * @param listener the session listener
     * @return the future object
     */
    public ChannelFuture executeSASLXOAuth2(final String tag, final String user, final String token, final SessionListener listener) {
    	
    	StringBuffer buf = new StringBuffer();
    	buf.append("user=").append(user).append("\u0001")
    	.append("auth=Bearer ").append(token).append("\u0001").append("\u0001");
    	String encOAuthStr = Base64.getEncoder().encodeToString(buf.toString().getBytes(StandardCharsets.UTF_8));
    	log.debug("XOAUTH2 " + encOAuthStr);
    	return executeOAuth2Command(tag, encOAuthStr, listener);
    }


    /**
     * Execute the IMAP logout command.
     * @param tag IMAP tag to be used
     * @param listener the session listener
     * @return the future object
     */
    public ChannelFuture executeLogoutCommand(final String tag, final SessionListener listener) {
    	ChannelFuture future = executeCommand(new ImapCommand(tag, "LOGOUT", new Argument(), new String[]{}), listener);
    	return future;
    }

    /**
     * Execute a IMAP capability command.
     * @param tag IMAP tag to be used
     * @param listener the session listener
     * @return the future object
     */
    public ChannelFuture executeCapabilityCommand(final String tag, final SessionListener listener) {
    	setState(IMAPSessionState.Connected);
        return executeCommand(new ImapCommand(tag, "CAPABILITY", new Argument(), new String[] {}), listener);
    }

    /**
     * Execute a IMAP append command.
     * @param tag IMAP tag to be used
     * @param labelName label/folder where the message is to be saved
     * @param flags message flags
     * @param size message size
     * @param listener the session listener
     * @return the future object
     */
    public ChannelFuture executeAppendCommand(final String tag, final String labelName, final String flags, 
    		final String size, final SessionListener listener) {
    	setState(IMAPSessionState.Connected);
        return executeCommand(new ImapCommand(tag, "APPEND", new Argument().addString(labelName).addLiteral(flags).addLiteral("{" + size + "}"),
                new String[] { "IMAP4REV1" }), listener);
    }

    /**
     * Execute a IMAP raw command.
     * @param rawText the raw text to be sent to the remote IMAP server
     * @return the future object
     */
    public ChannelFuture executeRawTextCommand(final String rawText) {
        return executeCommand(new ImapCommand("", rawText, null, new String[] {}), null);
    }
    
    /**
     * Execute a IMAP NOOP command.
     * @param tag IMAP tag to be used
     * @param listener the session listener
     * @return the future object
     */
    public ChannelFuture executeNOOPCommand(final String tag, final SessionListener listener) {
    	return executeCommand(new ImapCommand(tag, "NOOP", new Argument(), new String[]{}), listener);
    }

    /**
     * Format and sends the IMAP command to remote server.
     * @param method IMAP command to be sent
     * @param listener the session listener
     * @return the future object
     */
	private ChannelFuture executeCommand(final ImapCommand method, final SessionListener listener) {


		String args = (method.getArgs() != null ? method.getArgs().toString()
				: "");
		String line = method.getTag() + (method.getTag() != "" ? " " : "")
				+ method.getCommand() + (args.length() > 0 ? " " + args : "");
		log.debug("--> " + line);

		ChannelFuture lastWriteFuture = this.channel.writeAndFlush(line
				+ "\r\n");
		if (null != listener) {
			listeners.put(method.getTag(), listener);
		}

		if (lastWriteFuture != null) {
			// lastWriteFuture.sync();
		}
		return lastWriteFuture;
	}

    /**
     * Accumulate multi-line IMAP responses before giving the client callback.
     * @param response IMAP response to be queued
     */
    protected void addResponse(final IMAPResponse response) {
        responses.add(response);
    }
    
    /**
     * Get the IMAP tag corresponding to the IDLE command.
     * @return the IDLE tag
     */
    public String getIdleTag() {
    	return idleTag;
    }

    /**
     * Return the list of IMAP response objects.
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
     * @return state
     */
    public IMAPSessionState getState() {
        return state;
    }

    /**
     * Set the session state.
     * @param state session state
     */
    protected void setState(final IMAPSessionState state) {
        this.state = state;
    }

    /**
     * Returns the client listener registered for a specific tag.
     * @param tag get listener for this tag
     * @return ClientListener registered for the tag
     */
    protected ClientListener getClientListener(final String tag) {
        return listeners.get(tag);
    }    
    
    /**
     * Returns the connect/disconnect client listener.
     * @return ClientListener
     */
    protected ClientListener getSessionListener() {
    	return listener;
    }
    
    /**
     * Remove the listener after command for that tag is processed.
     * @param tag remove listener for this tag
     * @return the removed listener
     */
    protected SessionListener removeClientListener(final String tag) {
        return listeners.remove(tag);
    }
    
    /**
     * Reset this IMAP session.
     */
    protected void resetSession() {
    	resetResponseList();
    	listeners.clear();
    }

}
