package com.lafaspot.imapnio.async.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.async.response.ImapAsyncResponse;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * Unit test for {@code ImapAsyncResponse}.
 */
public class ImapAsyncResponseTest {

    /**
     * Tests ImapAsyncResponse constructor and getters.
     * 
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testImapAsyncResponse() throws IOException, ProtocolException {
        final Collection<IMAPResponse> imapResponses = new ArrayList<IMAPResponse>();
        final IMAPResponse oneImapResponse = new IMAPResponse("a1 OK AUTHENTICATE completed");
        imapResponses.add(oneImapResponse);
        final ImapAsyncResponse resp = new ImapAsyncResponse(imapResponses);

        final Collection<IMAPResponse> actual = resp.getResponseLines();
        Assert.assertEquals(actual, imapResponses, "result mismatched.");
        Assert.assertEquals(actual.iterator().next(), oneImapResponse, "result mismatched.");
    }
}
