package com.yahoo.imapnio.async.response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.request.CapaCommand;
import com.yahoo.imapnio.async.request.ImapRequest;

/**
 * Unit test for {@link ImapAsyncResponse}.
 */
public class ImapAsyncResponseTest {

    /**
     * Tests ImapAsyncResponse constructor and getters.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testImapAsyncResponse() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapRequest imapRequest = new CapaCommand();
        final int requestTotalBytes = "a0".getBytes(StandardCharsets.US_ASCII).length + 1 + imapRequest.getCommandLineBytes().readableBytes();
        final Collection<IMAPResponse> imapResponses = new ArrayList<IMAPResponse>();
        final IMAPResponse oneImapResponse = new IMAPResponse("a1 OK CAPABILITY completed");
        imapResponses.add(oneImapResponse);
        final int responseTotalBytes = oneImapResponse.toString().getBytes(StandardCharsets.US_ASCII).length + 2;
        final long elapsedTime = 1234L;
        final ImapAsyncResponse resp = new ImapAsyncResponse(imapRequest.getCommandType(), requestTotalBytes, responseTotalBytes, imapResponses,
                elapsedTime);

        Assert.assertEquals(resp.getCommandType(), imapRequest.getCommandType(), "command type mismatched.");
        Assert.assertEquals(resp.getRequestTotalBytes(), requestTotalBytes, "request bytes length mismatched.");
        Assert.assertEquals(resp.getResponseTotalBytes(), responseTotalBytes, "response bytes length mismatched.");
        final Collection<IMAPResponse> actual = resp.getResponseLines();
        Assert.assertEquals(actual, imapResponses, "result mismatched.");
        Assert.assertEquals(actual.iterator().next(), oneImapResponse, "result mismatched.");
        Assert.assertEquals(resp.getTotalTimeElapsed(), elapsedTime);
    }
}
