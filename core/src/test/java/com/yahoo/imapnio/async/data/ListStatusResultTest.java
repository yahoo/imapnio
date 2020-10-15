package com.yahoo.imapnio.async.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.Status;

/**
 * Unit test for {@link ListStatusResult}.
 */
public class ListStatusResultTest {

    /**
     * Tests ListStatusResult constructor and getters.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testListInfo() throws IOException, ProtocolException {
        final List<ExtensionListInfo> infos = new ArrayList<ExtensionListInfo>();
        final ExtensionListInfo linfo1 = new ExtensionListInfo(new IMAPResponse("* LIST (\\Archive \\HasNoChildren) \"/\" \"Archive\""));
        final ExtensionListInfo linfo2 = new ExtensionListInfo(new IMAPResponse("* LIST (\\HasNoChildren) \"/\" \"INBOX\""));

        infos.add(linfo1);
        infos.add(linfo2);

        final Map<String, Status> statuses = new HashMap<>();
        final Status status1 = new Status(new IMAPResponse("* STATUS \"Archive\" (HIGHESTMODSEQ 82676 MESSAGES 0 UIDNEXT 9 UIDVALIDITY 4 UNSEEN 0)"));
        statuses.put(status1.mbox, status1);
        final Status status2 = new Status(new IMAPResponse("* STATUS \"INBOX\" (HIGHESTMODSEQ 11 MESSAGES 0 UIDNEXT 9 UIDVALIDITY 4 UNSEEN 0)"));
        statuses.put(status2.mbox, status2);

        final ListStatusResult result = new ListStatusResult(infos, statuses);
        // verify getListInfos() result
        final List<ExtensionListInfo> actualInfos = result.getListInfos();
        Assert.assertNotNull(actualInfos, "getListInfos() should not be null");
        Assert.assertNotSame(actualInfos, infos, "should not return as same instance as input.");
        Assert.assertEquals(actualInfos.size(), 2, "getListInfos().size() mismatched.");
        Assert.assertNotNull(actualInfos.get(0), "ListInfo(0) mailbox name mismatched.");
        Assert.assertEquals(actualInfos.get(0).name, "Archive", "ListInfo(0) mailbox name mismatched.");
        Assert.assertNotNull(actualInfos.get(1), "ListInfo(1) mailbox name mismatched.");
        Assert.assertEquals(actualInfos.get(1).name, "INBOX", "ListInfo(1) mailbox name mismatched.");

        // verify getStatuses() result
        final Map<String, Status> actualStatuses = result.getStatuses();
        Assert.assertNotNull(actualStatuses, "getStatuses() should not be null");
        Assert.assertNotSame(actualStatuses, statuses, "should not return as same instance as input.");
        Assert.assertEquals(actualStatuses.size(), 2, "getStatuses().size() mismatched.");
        Assert.assertEquals(actualStatuses.get(status1.mbox), status1, "Status mismatched for " + status1.mbox);
        Assert.assertEquals(actualStatuses.get(status2.mbox), status2, "Status mismatched for " + status2.mbox);
    }
}
