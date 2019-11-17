package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap enable command request from client. RFC5161 ABNF: https://tools.ietf.org/html/rfc5161
 *
 * <pre>
 * capability =/ "ENABLE"
 *
 * command-any =/ "ENABLE" 1*(SP capability)
 *
 * response-data =/ "*" SP enable-data CRLF
 *
 * enable-data = "ENABLED" *(SP capability)
 * </pre>
 *
 */
public class EnableCommand extends ImapRequestAdapter {

    /**
     * Initializes a @{code EnableCommand}.
     *
     * @param capabilities List of capability to enable
     */
    public EnableCommand(@Nonnull final String[] capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public void cleanup() {
        capabilities = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() throws ImapAsyncClientException {
        final ByteBuf sb = Unpooled.buffer(ImapClientConstants.PAD_LEN);

        sb.writeBytes(ENABLE_SP_B);

        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
        for (int i = 0, len = capabilities.length; i < len; i++) {
            formatter.formatArgument(capabilities[i], sb, false);
            if (i < len - 1) { // do not add space for last item
                sb.writeByte(ImapClientConstants.SPACE);
            }
        }
        sb.writeBytes(CRLF_B);
        return sb;
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.ENABLE;
    }

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Enable and space. */
    private static final byte[] ENABLE_SP_B = "ENABLE ".getBytes(StandardCharsets.US_ASCII);

    /** Capability values. */
    private String[] capabilities;
}
