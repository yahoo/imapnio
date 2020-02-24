package com.yahoo.imapnio.command;

import io.netty.handler.codec.MessageToMessageDecoder;

import java.io.IOException;
import java.util.List;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;
/**
 * @author kraman
 *
 */
import io.netty.channel.ChannelHandlerContext;

/**
 * Basic response decoder. A ResponseDecoder (as opposed to a handler) is anything that outputs an IMAPResponse.
 *
 * @author kraman
 */
public class ImapClientRespDecoder extends MessageToMessageDecoder<String> {

    @Override
    protected void decode(final ChannelHandlerContext ctx, final String msg, final List<Object> out) throws IOException, ProtocolException {
        out.add(new IMAPResponse(msg));

    }
}
