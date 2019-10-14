package com.yahoo.imapnio.async.data;

import java.io.IOException;

import javax.mail.Flags.Flag;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * Unit test for {@code ExtensionMailboxInfo}.
 */
public class ExtensionMailboxInfoTest {

    /**
     * Tests parseMailboxInfo method successfully.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseExtensionMailboxInfoSuccess() throws IOException, ProtocolException, ImapAsyncClientException {

        final IMAPResponse[] content = new IMAPResponse[9];
        content[0] = new IMAPResponse("* 3 EXISTS");
        content[1] = new IMAPResponse("* 0 RECENT");
        content[2] = new IMAPResponse("* OK [UIDVALIDITY 1459808247] UIDs valid");
        content[3] = new IMAPResponse("* OK [UIDNEXT 150400] Predicted next UID");
        content[4] = new IMAPResponse("* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen $Forwarded $Junk $NotJunk)");
        content[5] = new IMAPResponse("* OK [PERMANENTFLAGS ()] No permanent flags permitted");
        content[6] = new IMAPResponse("* OK [HIGHESTMODSEQ 614]");
        content[7] = new IMAPResponse("* OK [MAILBOXID (2147483647)] Ok");
        content[8] = new IMAPResponse("002 OK [READ-ONLY] EXAMINE completed; now in selected state");
        final ExtensionMailboxInfo minfo = new ExtensionMailboxInfo(content);

        // verify the result
        Assert.assertNotNull(minfo, "result mismatched.");
        Assert.assertNotNull(minfo.availableFlags, "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.ANSWERED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DELETED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DRAFT), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.FLAGGED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.SEEN), "availableFlags mismatched.");
        Assert.assertEquals(minfo.highestmodseq, 614, "highestmodseq mismatched.");
        Assert.assertEquals(minfo.uidvalidity, 1459808247, "uidvalidity mismatched.");
        Assert.assertEquals(minfo.uidnext, 150400, "uidnext mismatched.");
        Assert.assertEquals(minfo.getMailboxId(), new Integer(2147483647), "MailboxId mismatched.");
    }

    /**
     * Tests constructing an instance of ExtensionMailboxInfo when MAILBOX ID does not follow the text code format.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseExtensionMailboxInfoLeftBracketNotFound() throws IOException, ProtocolException, ImapAsyncClientException {

        final IMAPResponse[] content = new IMAPResponse[9];
        content[0] = new IMAPResponse("* 3 EXISTS");
        content[1] = new IMAPResponse("* 0 RECENT");
        content[2] = new IMAPResponse("* OK [UIDVALIDITY 1459808247] UIDs valid");
        content[3] = new IMAPResponse("* OK [UIDNEXT 150400] Predicted next UID");
        content[4] = new IMAPResponse("* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen $Forwarded $Junk $NotJunk)");
        content[5] = new IMAPResponse("* OK [PERMANENTFLAGS ()] No permanent flags permitted");
        content[6] = new IMAPResponse("* OK [HIGHESTMODSEQ 614]");
        content[7] = new IMAPResponse("* OK MAILBOXID (2147483647) Ok");
        content[8] = new IMAPResponse("002 OK [READ-ONLY] EXAMINE completed; now in selected state");
        final ExtensionMailboxInfo minfo = new ExtensionMailboxInfo(content);

        // verify the result
        Assert.assertNotNull(minfo, "result mismatched.");
        Assert.assertNotNull(minfo.availableFlags, "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.ANSWERED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DELETED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DRAFT), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.FLAGGED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.SEEN), "availableFlags mismatched.");
        Assert.assertEquals(minfo.highestmodseq, 614, "highestmodseq mismatched.");
        Assert.assertEquals(minfo.uidvalidity, 1459808247, "uidvalidity mismatched.");
        Assert.assertEquals(minfo.uidnext, 150400, "uidnext mismatched.");
        Assert.assertNull(minfo.getMailboxId(), "MailboxId mismatched since format not correct.");
    }

    /**
     * Tests constructing an instance of ExtensionMailboxInfo when MAILBOX ID does not follow the text code format.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseExtensionMailboxInfoLeftParaenthesisNotFound() throws IOException, ProtocolException, ImapAsyncClientException {

        final IMAPResponse[] content = new IMAPResponse[9];
        content[0] = new IMAPResponse("* 3 EXISTS");
        content[1] = new IMAPResponse("* 0 RECENT");
        content[2] = new IMAPResponse("* OK [UIDVALIDITY 1459808247] UIDs valid");
        content[3] = new IMAPResponse("* OK [UIDNEXT 150400] Predicted next UID");
        content[4] = new IMAPResponse("* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen $Forwarded $Junk $NotJunk)");
        content[5] = new IMAPResponse("* OK [PERMANENTFLAGS ()] No permanent flags permitted");
        content[6] = new IMAPResponse("* OK [HIGHESTMODSEQ 614]");
        content[7] = new IMAPResponse("* OK [MAILBOXID 26] Ok");
        content[8] = new IMAPResponse("002 OK [READ-ONLY] EXAMINE completed; now in selected state");
        final ExtensionMailboxInfo minfo = new ExtensionMailboxInfo(content);

        // verify the result
        Assert.assertNotNull(minfo, "result mismatched.");
        Assert.assertNotNull(minfo.availableFlags, "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.ANSWERED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DELETED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DRAFT), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.FLAGGED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.SEEN), "availableFlags mismatched.");
        Assert.assertEquals(minfo.highestmodseq, 614, "highestmodseq mismatched.");
        Assert.assertEquals(minfo.uidvalidity, 1459808247, "uidvalidity mismatched.");
        Assert.assertEquals(minfo.uidnext, 150400, "uidnext mismatched.");
        Assert.assertNull(minfo.getMailboxId(), "MailboxId mismatched since format not correct.");
    }

    /**
     * Tests constructing an instance of ExtensionMailboxInfo when MAILBOX ID does not follow the text code format.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseExtensionMailboxInfoNoValueInParenthsis() throws IOException, ProtocolException, ImapAsyncClientException {

        final IMAPResponse[] content = new IMAPResponse[9];
        content[0] = new IMAPResponse("* 3 EXISTS");
        content[1] = new IMAPResponse("* 0 RECENT");
        content[2] = new IMAPResponse("* OK [UIDVALIDITY 1459808247] UIDs valid");
        content[3] = new IMAPResponse("* OK [UIDNEXT 150400] Predicted next UID");
        content[4] = new IMAPResponse("* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen $Forwarded $Junk $NotJunk)");
        content[5] = new IMAPResponse("* OK [PERMANENTFLAGS ()] No permanent flags permitted");
        content[6] = new IMAPResponse("* OK [HIGHESTMODSEQ 614]");
        content[7] = new IMAPResponse("* OK [MAILBOXID ()] Ok");
        content[8] = new IMAPResponse("002 OK [READ-ONLY] EXAMINE completed; now in selected state");
        final ExtensionMailboxInfo minfo = new ExtensionMailboxInfo(content);

        // verify the result
        Assert.assertNotNull(minfo, "result mismatched.");
        Assert.assertNotNull(minfo.availableFlags, "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.ANSWERED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DELETED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DRAFT), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.FLAGGED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.SEEN), "availableFlags mismatched.");
        Assert.assertEquals(minfo.highestmodseq, 614, "highestmodseq mismatched.");
        Assert.assertEquals(minfo.uidvalidity, 1459808247, "uidvalidity mismatched.");
        Assert.assertEquals(minfo.uidnext, 150400, "uidnext mismatched.");
        Assert.assertNull(minfo.getMailboxId(), "MailboxId mismatched since format not correct.");
    }
}
