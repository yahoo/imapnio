package com.yahoo.imapnio.async.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.MODSEQ;

public class FetchResultTest {
    /**
     * Tests FetchResult constructor and getters.
     */
    @Test
    public void testFetchhResult() throws IOException, ProtocolException {
        final IMAPResponse imapResponse = new IMAPResponse("* 1 FETCH (UID 4 MODSEQ (12121231000))");
        final FetchResponse fetchResponse = new FetchResponse(imapResponse);
        final List<FetchResponse> fetchResponses = new ArrayList<>();
        fetchResponses.add(fetchResponse);
        final List<Long> modifiedMsgNums = new ArrayList<>();
        modifiedMsgNums.add(1L);
        final FetchResult infos = new FetchResult(1L, fetchResponses);
        final List<FetchResponse>fetchResponsesResult = infos.getFetchResponses();
        final long highestModSeq = infos.getHighestModSeq();
        final long fetchedModSeq = fetchResponsesResult.get(0).getItem(MODSEQ.class).modseq;
        Assert.assertEquals(fetchResponsesResult.size(), 1, "Result mismatched.");
        Assert.assertEquals(fetchedModSeq, 12121231000L, "Result mismatched.");
        Assert.assertEquals(highestModSeq, 1L, "Result mismatched.");
    }

    /**
     * Tests FetchResult constructor and getters when passing null highest mod sequence and null fetch responses collection.
     */
    @Test
    public void testFetchhResultNullHighestModSeq() {
        final FetchResult infos = new FetchResult(new ArrayList<>());
        final List<FetchResponse>fetchResponsesResult = infos.getFetchResponses();
        final Long highestModSeq = infos.getHighestModSeq();
        Assert.assertEquals(fetchResponsesResult.size(), 0, "Result mismatched.");
        Assert.assertNull(highestModSeq, "Result mismatched.");
    }
}
