/**
 *
 */
package com.lafaspot.imapnio.command;

/**
 * @author kraman
 *
 */

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.io.IOException;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * Basic response decoder. A ResponseDecoder (as opposed to a handler) is anything that outputs an IMAPResponse.
 *
 * @author jgross
 */
public class ImapClientRespDecoder extends MessageToMessageDecoder<String> {

    /** logger. */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ImapClientRespDecoder.class);
    @Override
    protected void decode(ChannelHandlerContext ctx, String msg, List<Object> out) {
        try {
        // log.info("*** " + msg);
        out.add(new IMAPResponse(msg));
        // log.info("***1 " + msg);
        } catch (IOException ioe) {
            log.error("Parse failed" + ioe.getMessage());
        } catch (ProtocolException pe) {
            log.error("Parse failed" + pe.getMessage());
        }
    }
}

