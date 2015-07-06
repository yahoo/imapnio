/**
 *
 */
package org.apache.imapnioclient.client;

import org.apache.imapnioclient.command.IMAPClientRespDecoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

/**
 * @author kraman
 *
 */
public class IMAPClientInitializer extends ChannelInitializer<SocketChannel> {

    private final IMAPSession session;
    private final SslContext sslCtx;
    private final String host;
    private final int port;

    public IMAPClientInitializer(IMAPSession session, SslContext sslCtx, String host, int port) {
        this.session = session;
        this.sslCtx = sslCtx;
        this.host = host;
        this.port = port;
        // this.channelHandlers = channelHandlers;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc(), host, port));
        }

        // Add the text line codec combination first
        // Many discussions say that 8000 or 10000 is a large enough max value for line length here - but Gmail easily
        // sends lines >10k characters in length. (FETCH 1:* (ALL))
        pipeline.addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Delimiters.lineDelimiter()));
        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());

        // And then business logic.
        pipeline.addLast(new IMAPClientRespDecoder());
        pipeline.addLast(new IMAPClientRespHandler(session));
    }

}
