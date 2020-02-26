package com.yahoo.imapnio.async.data;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * Unit test for {@code StoreResult}.
 */
public class StoreResultTest {
    /**
     * Tests StoreResult constructor and getters.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testStoreResult() throws IOException, ProtocolException {
        final FetchResponse fr = new FetchResponse(new IMAPResponse("* 1 FETCH (UID 4 MODSEQ (12121231000))"));
        final List<FetchResponse> fetchResponses = Collections.singletonList(fr);
        final MessageNumberSet[] modifiedMsgSet = { new MessageNumberSet(1L, 1L) };
        final StoreResult storeResult = new StoreResult(1L, fetchResponses, modifiedMsgSet);
        final Long highestModSeq = storeResult.getHighestModSeq();
        final List<FetchResponse> responses = storeResult.getFetchResponses();

        Assert.assertEquals(responses.size(), 1, "getFetchResponses() mismatched.");
        Assert.assertNotNull(highestModSeq, "getHighestModSeq() should not return null");
        Assert.assertEquals(highestModSeq, Long.valueOf(1L), "getHighestModSeq() mismatched.");
        final MessageNumberSet[] modifiedMsgsets = storeResult.getModifiedMsgSets();
        Assert.assertNotNull(modifiedMsgsets, "getModifiedMsgSets() should not return null.");
        Assert.assertEquals(modifiedMsgsets.length, 1, "getModifiedMsgSets() size mismatched.");
        Assert.assertEquals(modifiedMsgsets[0], modifiedMsgSet[0], "getModifiedMsgSets() mismatched.");
    }
}
