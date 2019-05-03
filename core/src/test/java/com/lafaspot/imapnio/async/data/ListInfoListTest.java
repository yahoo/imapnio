package com.lafaspot.imapnio.async.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.lafaspot.imapnio.async.data.ListInfoList;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.ListInfo;

/**
 * Unit test for {@code ListInfoList}.
 */
public class ListInfoListTest {

    /**
     * Tests ListInfoList constructor and getters.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testListInfo() throws IOException, ProtocolException {
        final List<ListInfo> ll = new ArrayList<ListInfo>();
        ll.add(new ListInfo(new IMAPResponse("* LIST (\\Archive \\HasNoChildren) \"/\" \"Archive\"")));
        ll.add(new ListInfo(new IMAPResponse("* LIST (\\Junk \\HasNoChildren) \"/\" \"Bulk Mail\"")));
        ll.add(new ListInfo(new IMAPResponse("* LIST (\\Drafts \\HasNoChildren) \"/\" \"Draft\"")));
        ll.add(new ListInfo(new IMAPResponse("* LIST (\\HasNoChildren) \"/\" \"Inbox\"")));
        ll.add(new ListInfo(new IMAPResponse("* LIST (\\Sent \\HasNoChildren) \"/\" \"Sent\"")));
        ll.add(new ListInfo(new IMAPResponse("* LIST (\\Trash \\HasNoChildren) \"/\" \"Trash\"")));
        ll.add(new ListInfo(new IMAPResponse("* LIST (\\HasChildren) \"/\" \"test1\"")));

        final ListInfoList infos = new ListInfoList(ll);
        final List<ListInfo> result = infos.getListInfo();
        Assert.assertEquals(result.size(), 7, "Result mismatched.");
        Assert.assertNotNull(result.get(0), "Result should not be null.");
        Assert.assertEquals(result.get(0).name, "Archive", "Result should not be null.");
    }
}
