/**
 *
 */
package com.lafaspot.imapnio.command;

import java.util.List;

import org.slf4j.LoggerFactory;

import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * Echo and pass along IMAPResponses without modifying them.
 *
 * @author kraman
 */
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

/**
 * TODO: Describe me.
 *
 */
public class ImapClientRespEchoHandler extends MessageToMessageDecoder<IMAPResponse> {

    /** logger. */
    private final org.slf4j.Logger log = LoggerFactory.getLogger(ImapClientRespEchoHandler.class);

    @Override
    protected void decode(final ChannelHandlerContext ctx, final IMAPResponse msg, final List<Object> out) {
        log.debug(" <<< " + msg.toString());
        out.add(msg);
    }
}
