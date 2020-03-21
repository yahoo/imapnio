package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap id command request from client.
 */
public class IdCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** NIL literal byte array. */
    private static final byte[] NIL_B = { 'N', 'I', 'L' };

    /** ID and space. */
    private static final String ID_SP = "ID ";

    /** Byte array for ID and space. */
    private static final byte[] ID_SP_B = ID_SP.getBytes(StandardCharsets.US_ASCII);

    /** ID command line initial space. */
    private static final int IDLINE_LEN = 200;

    /** Key and value pair, key and value should all be ascii. */
    private Map<String, String> params;

    /**
     * Initializes a {@link IdCommand}.
     *
     * @param params a collection of parameters, key and value should all be ascii.
     */
    public IdCommand(final Map<String, String> params) {
        this.params = params;
    }

    @Override
    public void cleanup() {
        this.params = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() throws ImapAsyncClientException {
        final ByteBuf sb = Unpooled.buffer(IDLINE_LEN);
        sb.writeBytes(ID_SP_B);

        if (params == null) {
            sb.writeBytes(NIL_B);
        } else {
            // every token has to be encoded (double quoted and escaped) if needed
            // ex: a023 ID ("name" "so/"dr" "version" "19.34")
            final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
            sb.writeByte(ImapClientConstants.L_PAREN);
            boolean isFirstEntry = true;
            for (final Map.Entry<String, String> e : params.entrySet()) {
                if (!isFirstEntry) {
                    sb.writeByte(ImapClientConstants.SPACE);
                } else {
                    isFirstEntry = false;
                }
                formatter.formatArgument(e.getKey(), sb, true);
                sb.writeByte(ImapClientConstants.SPACE);
                formatter.formatArgument(e.getValue(), sb, true);
            }
            sb.writeByte(ImapClientConstants.R_PAREN);
        }

        sb.writeBytes(CRLF_B);
        return sb;
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.ID;
    }
}
