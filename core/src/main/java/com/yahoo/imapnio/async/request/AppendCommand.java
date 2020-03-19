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
        final int len = 2 * base64Folder.length() + ImapClientConstants.PAD_LEN;

        final ByteBuf buf = Unpooled.buffer(len);
        buf.writeBytes(APPEND_SP.getBytes(StandardCharsets.US_ASCII));

        // folder
        final ImapArgumentFormatter argWriter = new ImapArgumentFormatter();
        argWriter.formatArgument(base64Folder, buf, false); // already base64 encoded so can be formatted and write to buf
        buf.writeByte(ImapClientConstants.SPACE);

        // flags
        if (flags != null) { // set Flags in appended message
            buf.writeBytes(argWriter.buildFlagString(flags).getBytes(StandardCharsets.US_ASCII));
            buf.writeByte(ImapClientConstants.SPACE);
        }

        // date
        if (date != null) {
            argWriter.formatArgument(INTERNALDATE.format(date), buf, false);
            buf.writeByte(ImapClientConstants.SPACE);
        }

        // length of the literal
        final boolean isLiteralPlus = (literalOpt == LiteralSupport.ENABLE_LITERAL_PLUS);
        final boolean isLiteralMinus = (literalOpt == LiteralSupport.ENABLE_LITERAL_MINUS && data.length < MAX_LITERAL_MINUS_DATA_LEN);

        buf.writeByte('{');
        buf.writeBytes(Integer.toString(data.length).getBytes(StandardCharsets.US_ASCII));
        if (isLiteralPlus) {
            buf.writeByte('+');
        } else if (isLiteralMinus) {
            buf.writeByte('-');
        }
        buf.writeByte('}');
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
