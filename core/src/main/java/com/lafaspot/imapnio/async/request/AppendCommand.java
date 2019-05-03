package com.lafaspot.imapnio.async.request;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.Flags;

import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;
import com.lafaspot.imapnio.async.exception.ImapAsyncClientException.FailureType;
import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.INTERNALDATE;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap append command request from client.
 */
public class AppendCommand implements ImapRequest<ByteBuf> {

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
        this.folderName = folderName;
        this.flags = imapFlags;
        this.date = internalDate;
        this.data = data;
    }

    @Override
    public void cleanup() {
        this.folderName =  null;
        this.flags = null;
        this.date = null;
        this.data =  null;
    }

    @Override
    public ConcurrentLinkedQueue<IMAPResponse> getStreamingResponsesQueue() {
        return null;
    }

    @Override
    public String getCommandLine() {
        // Ex: APPEND saved-messages (\Seen) {310}
        // encode the folder name as per RFC2060
        final String folder = BASE64MailboxEncoder.encode(folderName);
        final int len = 2 * folder.length() + ImapClientConstants.PAD_LEN;

        final StringBuilder sb = new StringBuilder(len).append(APPEND_SP);

        // folder
        final ImapArgumentFormatter argWriter = new ImapArgumentFormatter();
        argWriter.formatArgument(folder, sb, false);
        sb.append(ImapClientConstants.SPACE);

        // flags
        if (flags != null) { // set Flags in appended message
            sb.append(argWriter.buildFlagString(flags));
            sb.append(ImapClientConstants.SPACE);
        }

        // date
        if (date != null) {
            argWriter.formatArgument(INTERNALDATE.format(date), sb, false);
            sb.append(ImapClientConstants.SPACE);
        }

        // length of the literal
        sb.append('{').append(Integer.toString(data.length)).append('}');
        sb.append(ImapClientConstants.CRLF);
        return sb.toString();

    }

    @Override
    public String getLogLine() {
        // TODO: decide whether we need to log some content
        return getCommandLine();
    }

    @Override
    public ByteBuf getNextCommandLineAfterContinuation(@Nonnull final IMAPResponse serverResponse) {
        // Note: we obtain only binary from client, therefore need to write binary directly to retain the correct charset encoding, CANNOT convert it
        // to String since we do not know the charset.
        final int length = data.length + ImapClientConstants.CRLFLEN;
        final ByteBuf buffer = Unpooled.buffer(length);
        buffer.writeBytes(data);
        buffer.writeBytes(ImapClientConstants.CRLF.getBytes(StandardCharsets.US_ASCII)); // CRLF is 10 and 13, < 128, so either ASCII or UTF-8 is fine
        return buffer;
    }

    @Override
    public String getTerminateCommandLine() throws ImapAsyncClientException {
        throw new ImapAsyncClientException(FailureType.OPERATION_NOT_SUPPORTED_FOR_COMMAND);
    }
}
