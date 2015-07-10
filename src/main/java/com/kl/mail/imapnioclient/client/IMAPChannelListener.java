package com.kl.mail.imapnioclient.client;

import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public class IMAPChannelListener implements ChannelHandler {
	

    /** logger. */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IMAPChannelListener.class);
	
	private final IMAPSession session;
	public IMAPChannelListener(IMAPSession session) {
		this.session = session;
	}

	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		log.info ("channel - added");	
	}

	/**
	 * A channel has been removed, socket disconnect event. Call the client listener if one was registered.
	 * @param ctx the channel handler context
	 */
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		log.info ("channel closed - removed");
		if (null != session && session.getSessionListener() != null) {
			session.getSessionListener().onDisconnect(session);
		}		
	}

	/**
	 * Got exception from channel. Call the client listener if one was registered.
	 */
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		// TODO Auto-generated method stub
		log.info  ("channel closed - exception");
		if (null != session && session.getSessionListener() != null) {
			session.getSessionListener().onDisconnect(session);
		}
	}

}
