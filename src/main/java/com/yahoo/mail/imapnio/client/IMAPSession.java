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
import com.yahoo.mail.imapnio.client.exception.IMAPSessionException;

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
    
    private String idleTag;

    private final List<IMAPResponse> responses;
    
    private IMAPClientListener clientListener;
    

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
	public IMAPSession(URI uri, Bootstrap bootstrap, EventLoopGroup group) throws IMAPSessionException {
		responses = new ArrayList<IMAPResponse>();
		listeners = new ConcurrentHashMap<String, IMAPClientListener>();
		this.group = group;
		// Initialize default state
		capabilities = new HashMap<String, Boolean>();

		boolean ssl = uri.getScheme().toLowerCase().equals("imaps");

		try {
			// Configure SSL.
			SslContext sslCtx;
			if (ssl) {
				sslCtx = SslContext
						.newClientContext(TrustManagerFactory
								.getInstance(TrustManagerFactory
										.getDefaultAlgorithm())); // PKIX
			} else {
				sslCtx = null;
			}

			// Save any other metadata

			setState(IMAPSessionState.ConnectRequest);
			// setIdleState(IMAPNIOClientSessionIdleState.NotIdle);

			// Open channel using the bootstrap
			bootstrap
					.channel(NioSocketChannel.class)
					.handler(
							new IMAPClientInitializer(this, sslCtx, uri
									.getHost(), uri.getPort())).group(group);
			ChannelFuture f = bootstrap.connect(uri.getHost(), uri.getPort());
			
			this.channel = f.sync().channel();

			this.channel.closeFuture().addListener(
					new SessionDisconnectListener(this));
		} catch (SSLException e1) {
			throw new IMAPSessionException("ssl exception", e1);
		} catch (InterruptedException e2) {
			throw new IMAPSessionException("connect failed", e2);
		} catch (NoSuchAlgorithmException e3) {
			throw new IMAPSessionException("unknown ssl algo", e3);
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
     * @param tag
     */
	public ChannelFuture executeIdleCommand(final String tag,
			final IMAPClientListener listener) {
		ChannelFuture future = executeCommand(new IMAPCommand(tag, "IDLE",
				new Argument(), new String[] { "IDLE" }), listener);
		if (null != future) {
			future.addListener(new GenericFutureListener<ChannelFuture>() {
				public void operationComplete(ChannelFuture future)
						throws Exception {
					setState(IMAPSessionState.IDLE_REQUEST);
					idleTag = tag;
				}
			});
		}
		return future;
	}

    public ChannelFuture executeSelectCommand(final String tag, final String mailbox, final IMAPClientListener listener) {
        String b64Mailbox = BASE64MailboxEncoder.encode(mailbox);
        return executeCommand(new IMAPCommand(tag, "SELECT", new Argument().addString(b64Mailbox), new String[] {}), listener);
    }

    public ChannelFuture executeStatusCommand(String tag, String mailbox, String[] items, final IMAPClientListener listener) {
        mailbox = BASE64MailboxEncoder.encode(mailbox);

        Argument args = new Argument();
        args.writeString(mailbox);

        Argument itemArgs = new Argument();

        for (int i = 0, len = items.length; i < len; i++)
            itemArgs.writeAtom(items[i]);
        args.writeArgument(itemArgs);

    	setState(IMAPSessionState.Connected);
        return executeCommand(new IMAPCommand(tag, "STATUS", args, new String[] {}), listener);
    }

    /**
     * Execute an authentication command against server.
     *
     * @param tag
     *            An auth command to execute in the session.
     */
    public ChannelFuture executeLoginCommand(String tag, String username, String password, final IMAPClientListener listener) {
        return executeCommand(new IMAPCommand(tag, "LOGIN", new Argument().addString(username).addString(password), new String[] { "auth=plain" }), listener);
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
    public ChannelFuture executeOAuth2Command(final String tag, final String oauth2Tok, final IMAPClientListener listener) {
        ChannelFuture future = executeCommand(new IMAPCommand(tag, "AUTHENTICATE XOAUTH2", new Argument().addString(oauth2Tok), new String[] { "auth=xoauth2" }), listener);
        return future;
    }
    
//    /**
//     * Send Yahoo! specific XYMLOGIN command.
//     * @param tag tag to be used for this command
//     * @param xymloginTok the login token
//     * @return ChannelFuture
//     * @throws InterruptedException
//     */
//    public ChannelFuture executeXYMLOGINCommand(final String tag, String xymloginTok) {
//    	ChannelFuture future = executeCommand (new IMAPCommand(tag, "AUTHENTICATE XYMLOGIN", new Argument().addString(xymloginTok), new String[]{}));
//        future.addListener(new GenericFutureListener<ChannelFuture>() {
//            public void operationComplete(ChannelFuture future) throws Exception {
//                state = IMAPSessionState.OAUTH2_INIT;
//            }
//        });
//    	return future;
//    }
    
    /**
     * Execute logout command.
     *
     * @param tag
     *            IMAP tag used for this command
     * @return
     * @throws InterruptedException
     */
    public ChannelFuture executeLogoutCommand(final String tag, IMAPClientListener listener) {
    	ChannelFuture future = executeCommand(new IMAPCommand(tag, "LOGOUT", new Argument(), new String[]{}), listener);
    	return future;
    }

    /**
     * Execute a CAPABILITY command.
     *
     * @param tag
     */
    public ChannelFuture executeCapabilityCommand(String tag, final IMAPClientListener listener) {
    	setState(IMAPSessionState.Connected);
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
    public ChannelFuture executeAppendCommand(String tag, String labelName, String flags, String size, final IMAPClientListener listener) {
    	setState(IMAPSessionState.Connected);
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
    public ChannelFuture executeRawTextCommand(String rawText) {
        return executeCommand(new IMAPCommand("", rawText, null, new String[] {}), null);
    }
    
    /**
     * Sends a NOOP command
     * @param tag tag to be used
     * @return ChannelFuture
     * @throws InterruptedException
     */
    public ChannelFuture executeNOOPCommand(String tag, final IMAPClientListener listener) {
    	return executeCommand(new IMAPCommand(tag, "NOOP", new Argument(), new String[]{}), listener);
    }

    /**
     * Execute a command against the server. To run a command you should go through specific methods like `executeLoginCommand`.
     *
     * @param method
     */
	private ChannelFuture executeCommand(IMAPCommand method,
			IMAPClientListener listener) {
		/*
		String[] capabilitiesRequired = method.getCapabilities();
		for (String requiredCapability : capabilitiesRequired) {
			if (!this.hasCapability(requiredCapability)) {
				log.error("Do not have required capability: "
						+ requiredCapability);
			}
		}
		*/

		String args = (method.getArgs() != null ? method.getArgs().toString()
				: "");
		String line = method.getTag() + (method.getTag() != "" ? " " : "")
				+ method.getCommand() + (args.length() > 0 ? " " + args : "");
		log.info("--> " + line);

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
     * Returns true if this server broadcasts this capability. Capability is case-insensitive.
     *
     * @param capability
     * @return
     */
    public boolean hasCapability(String capability) {
        return this.capabilities.containsKey(capability.toLowerCase()) && this.capabilities.get(capability.toLowerCase()).equals(true);
    }


    public void addResponse(IMAPResponse response) {
        responses.add(response);
    }
    
    public String getIdleTag() {
    	return idleTag;
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

    public IMAPClientListener getClientListener(String tag) {
        return listeners.get(tag);
    }    
    
    public IMAPClientListener removeClientListener(String tag) {
        return listeners.remove(tag);
    }

    class SessionDisconnectListener implements GenericFutureListener<ChannelFuture> {
    	private final IMAPSession session;
    	SessionDisconnectListener(IMAPSession session) {
    		this.session = session;
    	}
    	
		public void operationComplete(ChannelFuture future)
				throws Exception {
			if (null != session.clientListener) {
				session.clientListener.onDisconnect(session);
			}
		}
    }

}
