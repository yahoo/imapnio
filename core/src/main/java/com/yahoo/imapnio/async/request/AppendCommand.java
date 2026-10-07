package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.Flags;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.INTERNALDATE;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines IMAP append command request from client.
 */
public class AppendCommand implements ImapRequest {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Maximum length of data that can be sent in alternate literal form when LITERAL- is supported. */
    private static final int MAX_LITERAL_MINUS_DATA_LEN = 4096;

    /** Literal for append. */
    private static final String APPEND_SP = "APPEND ";

    /** The folder for the message to be appended to. */
    private String folderName;

    /** The flags for the message. */
    private Flags flags;

    /** The internal date associated with the message. */
    private Date date;

    /** The message data. */
    private byte[] data;

    /** Whether to enable Literal support option. */
    private LiteralSupport literalOpt;

    /**
     * Initializes an append command for client.
     *
     * @param folderName the folder to which the message must be appended
     * @param imapFlags the flags for the message
     * @param internalDate the internal date associated with the message
     * @param data the message data
     */
    public AppendCommand(@Nonnull final String folderName, @Nullable final Flags imapFlags, @Nullable final Date internalDate,
            @Nonnull final byte[] data) {
        this(folderName, imapFlags, internalDate, data, LiteralSupport.DISABLE);
    }

    /**
     * Initializes an append command for client.
     *
     * @param folderName the folder to which the message must be appended
     * @param imapFlags the flags for the message
     * @param internalDate the internal date associated with the message
     * @param data the message data
     * @param literalOpt literal support option
     */
    public AppendCommand(@Nonnull final String folderName, @Nullable final Flags imapFlags, @Nullable final Date internalDate,
            @Nonnull final byte[] data, @Nonnull final LiteralSupport literalOpt) {
        this.folderName = folderName;
        this.flags = imapFlags;
        this.date = internalDate;
        this.data = data;
        this.literalOpt = literalOpt;
    }

    @Override
    public void cleanup() {
        this.folderName = null;
        this.flags = null;
        this.date = null;
        this.data = null;
        this.literalOpt = null;
    }

    @Override
    public ConcurrentLinkedQueue<IMAPResponse> getStreamingResponsesQueue() {
        return null;
    }

    @Override
    public ByteBuf getCommandLineBytes() throws ImapAsyncClientException {
        // Ex: APPEND saved-messages (\Seen) {310}
        // encode the folder name as per RFC2060
        final String base64Folder = BASE64MailboxEncoder.encode(folderName);

        // folder, already base64 encoded and so unable to need a literal of its own; the message data literal is written further down
        final ImapArgumentFormatter argWriter = new ImapArgumentFormatter();
        final ImapCommandLineBuilder builder = new ImapCommandLineBuilder(LiteralSupport.DISABLE)
                .raw(APPEND_SP.getBytes(StandardCharsets.US_ASCII))
                .astring(base64Folder, false, "folder name")
                .raw((byte) ImapClientConstants.SPACE);

        // flags
        if (flags != null) { // set Flags in appended message
            builder.raw(argWriter.buildFlagString(flags).getBytes(StandardCharsets.US_ASCII))
                    .raw((byte) ImapClientConstants.SPACE);
        }

        // date, generated here rather than supplied by the caller
        if (date != null) {
            builder.astring(INTERNALDATE.format(date), false, "internal date")
                    .raw((byte) ImapClientConstants.SPACE);
        }

        final ByteBuf buf = builder.openSingle();

        // length of the literal
        final boolean isLiteralPlus = (literalOpt == LiteralSupport.ENABLE_LITERAL_PLUS);
        final boolean isLiteralMinus = (literalOpt == LiteralSupport.ENABLE_LITERAL_MINUS && data.length < MAX_LITERAL_MINUS_DATA_LEN);

        buf.writeByte(ImapClientConstants.L_BRACE);
        buf.writeBytes(Integer.toString(data.length).getBytes(StandardCharsets.US_ASCII));
        if (isLiteralPlus || isLiteralMinus) {
            // RFC 7888 section 4: literal = "{" number ["+"] "}" CRLF *CHAR8. LITERAL- is marked with "+" exactly as LITERAL+ is, the two
            // differ only in the size cap, so there is no "-" form to write here
            buf.writeByte(ImapClientConstants.PLUS);
        }
        buf.writeByte(ImapClientConstants.R_BRACE);
        buf.writeBytes(CRLF_B);

        // decide to send literal
        if (isLiteralPlus || isLiteralMinus) {
            buf.writeBytes(buildDataByteBuf());
        }
        return buf;

    }

    @Override
    public String getCommandLine() throws ImapAsyncClientException {
        return getCommandLineBytes().toString(StandardCharsets.UTF_8);
    }

    @Override
    public boolean isCommandLineDataSensitive() {
        return false;
    }

    @Override
    public String getDebugData() {
        return null;
    }

    /**
     * @return the byte buffer for the literal data
     */
    private ByteBuf buildDataByteBuf() {
        // Note: we obtain only binary from client, therefore need to write binary directly to retain the correct charset encoding, CANNOT convert it
        // to String since we do not know the charset.
        final int length = data.length + ImapClientConstants.CRLFLEN;
        final ByteBuf buffer = Unpooled.buffer(length);
        buffer.writeBytes(data);
        buffer.writeBytes(CRLF_B); // CRLF is 10 and 13, < 128, so either ASCII or UTF-8 is fine
        return buffer;
    }

    @Override
    public ByteBuf getNextCommandLineAfterContinuation(@Nonnull final IMAPResponse serverResponse) throws ImapAsyncClientException {
        if (literalOpt == LiteralSupport.ENABLE_LITERAL_PLUS
                || (literalOpt == LiteralSupport.ENABLE_LITERAL_MINUS && data.length < MAX_LITERAL_MINUS_DATA_LEN)) {
            // should not reach here, since if LITERAL+ or LITERAL- is requested, server should not ask for next line
            throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
        }
        return buildDataByteBuf();
    }

    @Override
    public ByteBuf getTerminateCommandLine() throws ImapAsyncClientException {
        throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.APPEND_MESSAGE;
    }
}
