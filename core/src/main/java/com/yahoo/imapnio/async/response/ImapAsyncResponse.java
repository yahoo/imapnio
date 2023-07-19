package com.yahoo.imapnio.async.response;

import java.util.Collection;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.request.ImapCommandType;

/**
 * This class defines the Response for IMAP asynchronous requests.
 */
public class ImapAsyncResponse {

    /** Command type. */
    private ImapCommandType commandType;
    /** Request total number of bytes. */
    private int requestTotalBytes;
    /** Response total number of bytes. */
    private int responseTotalBytes;
    /** List of IMAPResponse lines. */
    private Collection<IMAPResponse> responses;
    /** Total time elapsed by command in millis. */
    private long totalTimeElapsedInMillis;

    /**
     * Initializes an {@link ImapAsyncResponse} object.
     *
     * @param commandType imap command type
     * @param requestTotalBytes number of bytes in request
     * @param responseTotalBytes number of bytes in response
     * @param responses list of response lines
     * @param totalTimeElapsedInMillis total time elapsed in millis
     */
    public ImapAsyncResponse(final ImapCommandType commandType, final int requestTotalBytes, final int responseTotalBytes,
            final Collection<IMAPResponse> responses, final long totalTimeElapsedInMillis) {
        this.responses = responses;
        this.commandType = commandType;
        this.requestTotalBytes = requestTotalBytes;
        this.responseTotalBytes = responseTotalBytes;
        this.totalTimeElapsedInMillis = totalTimeElapsedInMillis;
    }

    /**
     * @return command type
     */
    public ImapCommandType getCommandType() {
        return commandType;
    }

    /**
     * @return number of bytes in request
     */
    public int getRequestTotalBytes() {
        return requestTotalBytes;
    }

    /**
     * @return number of bytes in response
     */
    public int getResponseTotalBytes() {
        return responseTotalBytes;
    }

    /**
     * @return list of IMAPResponse lines.
     */
    public Collection<IMAPResponse> getResponseLines() {
        return responses;
    }

    /**
     * @return total time elapsed in milli seconds.
     */
    public long getTotalTimeElapsed() {
        return totalTimeElapsedInMillis;
    }
}
