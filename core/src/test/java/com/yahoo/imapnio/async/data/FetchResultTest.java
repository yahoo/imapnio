package com.yahoo.imapnio.async.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.MODSEQ;

/**
 * Unit test for {@code FetchResult}.
 */
public class FetchResultTest {
    /**
     * Tests FetchResult constructor and getters.
     */
    @Test
    public void testFetchhResult() throws IOException, ProtocolException {
        final IMAPResponse imapResponse = new IMAPResponse("* 1 FETCH (UID 4 MODSEQ (12121231000))");
        final List<IMAPResponse> expectedFetchResponses = Collections.singletonList(imapResponse);
        final FetchResult infos = new FetchResult(expectedFetchResponses);
        final List<IMAPResponse>fetchResponsesResult = infos.getIMAPResponses();
        final FetchResponse actualFetchResponse = new FetchResponse(fetchResponsesResult.get(0));
        final long fetchedModSeq = actualFetchResponse.getItem(MODSEQ.class).modseq;

        Assert.assertEquals(fetchResponsesResult.size(), 1, "Result mismatched.");
        Assert.assertEquals(fetchedModSeq, 12121231000L, "Result mismatched.");
    }

    /**
     * Tests FetchResult constructor and getters when passing null highest mod sequence and null empty responses collection.
     */
    @Test
    public void testFetchhResultNullHighestModSeq() {
        final FetchResult infos = new FetchResult(Collections.emptyList());
        final List<IMAPResponse>fetchResponsesResult = infos.getIMAPResponses();
        Assert.assertEquals(fetchResponsesResult.size(), 0, "Result mismatched.");
    }
}
