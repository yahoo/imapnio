package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

import io.netty.buffer.ByteBuf;

/**
 * This class defines imap id command request from client.
 *
 * <p>
 * A field or value carrying CR or LF is sent as a literal, since a quoted string cannot represent one. With a synchronizing literal the
 * command line is sent in more than one piece, each piece released by a command continuation request from the server.
 * </p>
 */
public class IdCommand extends ImapRequestAdapter {

    /** NIL literal byte array. */
    private static final byte[] NIL_B = { 'N', 'I', 'L' };

    /** ID and space. */
    private static final String ID_SP = "ID ";

    /** Byte array for ID and space. */
    private static final byte[] ID_SP_B = ID_SP.getBytes(StandardCharsets.US_ASCII);

    /** Key and value pair, key and value should all be ascii. */
    private Map<String, String> params;

    /** Which literal form the server accepts. */
    private LiteralSupport literalOpt;

    /** The command line pieces, populated when the command line is built. */
    private List<ByteBuf> segments;

    /** Index of the piece to send on the next command continuation request. */
    private int nextSegment;

    /**
     * Initializes a {@link IdCommand}.
     *
     * @param params a collection of parameters, key and value should all be ascii.
     */
    public IdCommand(final Map<String, String> params) {
        this(params, LiteralSupport.DISABLE);
    }

    /**
     * Initializes a {@link IdCommand} with a literal form.
     *
     * @param params a collection of parameters, key and value should all be ascii.
     * @param literalOpt the literal form the server accepts, which decides whether a literal costs a round trip
     */
    public IdCommand(final Map<String, String> params, @Nonnull final LiteralSupport literalOpt) {
        this.params = params;
        this.literalOpt = literalOpt;
    }

    @Override
    public void cleanup() {
        this.params = null;
        this.literalOpt = null;
        this.segments = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() throws ImapAsyncClientException {
        final ImapCommandLineBuilder builder = new ImapCommandLineBuilder(literalOpt);
        builder.raw(ID_SP_B);

        if (params == null) {
            builder.raw(NIL_B);
        } else {
            // every token has to be encoded (double quoted and escaped) if needed
            // ex: a023 ID ("name" "so/"dr" "version" "19.34")
            builder.raw((byte) ImapClientConstants.L_PAREN);
            boolean isFirstEntry = true;
            for (final Map.Entry<String, String> e : params.entrySet()) {
                if (!isFirstEntry) {
                    builder.raw((byte) ImapClientConstants.SPACE);
                } else {
                    isFirstEntry = false;
                }
                builder.astring(e.getKey(), true, "id field");
                builder.raw((byte) ImapClientConstants.SPACE);
                builder.astring(e.getValue(), true, "id value");
            }
            builder.raw((byte) ImapClientConstants.R_PAREN);
        }

        segments = builder.finish();
        nextSegment = 1;
        return segments.get(0);
    }

    @Override
    public ByteBuf getNextCommandLineAfterContinuation(@Nonnull final IMAPResponse serverResponse) throws ImapAsyncClientException {
        if (segments == null || nextSegment >= segments.size()) {
            // the server asked for more than this command line has left to give
            throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
        }
        return segments.get(nextSegment++);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.ID;
    }
}
