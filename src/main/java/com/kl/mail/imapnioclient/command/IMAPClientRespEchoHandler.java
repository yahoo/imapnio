/**
 *
 */
package com.kl.mail.imapnioclient.command;


/**
 * Echo and pass along IMAPResponses without modifying them.
 *
 * @author kraman
 */
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

import org.slf4j.LoggerFactory;

import com.sun.mail.imap.protocol.IMAPResponse;

public class IMAPClientRespEchoHandler extends MessageToMessageDecoder<IMAPResponse> {

    /** logger. */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IMAPClientRespEchoHandler.class);
    @Override
    protected void decode(ChannelHandlerContext ctx, IMAPResponse msg, List<Object> out) {
        log.debug(" <<< " + msg.toString());
        out.add(msg);
    }
}
