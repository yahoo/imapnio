package com.kl.mail.imapnioclient.client;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public class SocketCloseListener implements ChannelHandler {
	
	private final IMAPSession session;
	public SocketCloseListener(IMAPSession session) {
		this.session = session;
	}

	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		System.out.println ("channel - added");
		
	}

	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		System.out.println ("channel closed - removed");
		
	}

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		// TODO Auto-generated method stub
		System.out.println ("channel closed - exception");
		
	}

}
