package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

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
     * Initializes a {@link EnableCommand}.
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
    public ByteBuf getCommandLineBytes() {
        final ByteBuf sb = Unpooled.buffer(ENABLE_BUF_LEN);

        sb.writeBytes(ENABLE_B);

        for (int i = 0; i < capabilities.length; i++) {
            sb.writeByte(ImapClientConstants.SPACE);
            // capability ABNF is:
            // capability = ("AUTH=" auth-type) / atom
            sb.writeBytes(capabilities[i].getBytes(StandardCharsets.US_ASCII));
        }
        sb.writeBytes(CRLF_B);
        return sb;
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.ENABLE;
    }

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Enable and space. */
    private static final byte[] ENABLE_B = "ENABLE".getBytes(StandardCharsets.US_ASCII);

    /** Enable command buffer length. */
    private static final int ENABLE_BUF_LEN = 200;

    /** Capability values. */
    private String[] capabilities;
}
