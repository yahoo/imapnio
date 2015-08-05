/**
 *
 */
package com.lafaspot.imapnio.command;

import java.io.IOException;
import java.util.List;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * @author kraman
 *
 */

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

/**
 * Basic response decoder. A ResponseDecoder (as opposed to a handler) is anything that outputs an IMAPResponse.
 *
 * @author jgross
 */
public class ImapClientRespDecoder extends MessageToMessageDecoder<String> {

    @Override
    protected void decode(final ChannelHandlerContext ctx, final String msg, final List<Object> out) throws IOException, ProtocolException {
        out.add(new IMAPResponse(msg));

    }
}
