package com.yahoo.imapnio.async.data;

import java.io.IOException;

import javax.mail.Flags.Flag;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * Unit test for {@link ExtensionMailboxInfo}.
 */
public class ExtensionMailboxInfoTest {

    /**
     * Tests calling constructor successfully.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testCreateExtensionMailboxInfoSuccess() throws IOException, ProtocolException {

        final IMAPResponse[] content = new IMAPResponse[9];
        content[0] = new IMAPResponse("* 3 EXISTS");
        content[1] = new IMAPResponse("* 0 RECENT");
        content[2] = new IMAPResponse("* OK [UIDVALIDITY 1459808247] UIDs valid");
        content[3] = new IMAPResponse("* OK [UIDNEXT 150400] Predicted next UID");
        content[4] = new IMAPResponse("* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen $Forwarded $Junk $NotJunk)");
        content[5] = new IMAPResponse("* OK [PERMANENTFLAGS ()] No permanent flags permitted");
        content[6] = new IMAPResponse("* OK [HIGHESTMODSEQ 614]");
        content[7] = new IMAPResponse("* OK [MAILBOXID (214-mailbox)] Ok");
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
        Assert.assertEquals(minfo.getMailboxId(), "214-mailbox", "MailboxId mismatched.");
        Assert.assertFalse(minfo.isNoModSeq(), "isNoModSeq() mismatched.");
        Assert.assertNull(content[7], "This element should be nulled out");
    }

    /**
     * Tests calling constructor when there is other text code, like MAILBOXABC, expects getMailboxId() to return null.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testCreateExtensionMailboxInfoSomeOtherTextCode() throws IOException, ProtocolException {

        final IMAPResponse[] content = new IMAPResponse[9];
        content[0] = new IMAPResponse("* 3 EXISTS");
        content[1] = new IMAPResponse("* 0 RECENT");
        content[2] = new IMAPResponse("* OK [UIDVALIDITY 1459808247] UIDs valid");
        content[3] = new IMAPResponse("* OK [UIDNEXT 150400] Predicted next UID");
        content[4] = new IMAPResponse("* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen $Forwarded $Junk $NotJunk)");
        content[5] = new IMAPResponse("* OK [PERMANENTFLAGS ()] No permanent flags permitted");
        content[6] = new IMAPResponse("* OK [HIGHESTMODSEQ 614]");
        content[7] = new IMAPResponse("* OK [MAILBOXABC (2147483647)] Ok");
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
        Assert.assertNull(minfo.getMailboxId(), "MailboxId mismatched, should not be set.");
        Assert.assertNotNull(content[7], "This element should not be nulled out");
        Assert.assertEquals(content[7].getRest(), "[MAILBOXABC (2147483647)] Ok", "The index is not reset to the point of the status code.");
    }

    /**
     * Tests constructing an instance of ExtensionMailboxInfo when MAILBOX ID does not follow the text code format.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testCreateExtensionMailboxInfoLeftBracketNotFound() throws IOException, ProtocolException {

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
        Assert.assertNotNull(content[7], "This element should not be nulled out");
        Assert.assertEquals(content[7].getRest(), "MAILBOXID (2147483647) Ok", "The index is not reset to the point of the status code.");
    }

    /**
     * Tests constructing an instance of ExtensionMailboxInfo when MAILBOX ID does not follow the text code format.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testCreateExtensionMailboxInfoLeftParaenthesisNotFound() throws IOException, ProtocolException {

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
        Assert.assertNotNull(content[7], "This element should not be nulled out");
        Assert.assertEquals(content[7].getRest(), "[MAILBOXID 26] Ok", "The index is not reset to the point of the status code.");
    }

    /**
     * Tests constructing an instance of ExtensionMailboxInfo when MAILBOX ID does not follow the text code format.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testCreateExtensionMailboxInfoNoValueInParenthsis() throws IOException, ProtocolException {

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
        Assert.assertNotNull(content[7], "This element should not be nulled out");
        Assert.assertEquals(content[7].getRest(), "[MAILBOXID ()] Ok", "The index is not reset to the point of the status code.");
    }

    /**
     * Tests calling constructor with no mod seq successfully.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testCreateExtensionMailboxInfoSuccessNoModSequence() throws IOException, ProtocolException {

        final IMAPResponse[] content = new IMAPResponse[9];
        content[0] = new IMAPResponse("* 3 EXISTS");
        content[1] = new IMAPResponse("* 0 RECENT");
        content[2] = new IMAPResponse("* OK [UIDVALIDITY 1459808247] UIDs valid");
        content[3] = new IMAPResponse("* OK [UIDNEXT 150400] Predicted next UID");
        content[4] = new IMAPResponse("* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen $Forwarded $Junk $NotJunk)");
        content[5] = new IMAPResponse("* OK [PERMANENTFLAGS ()] No permanent flags permitted");
        content[6] = new IMAPResponse("* OK [MAILBOXID (214-mailbox)] Ok");
        content[7] = new IMAPResponse("* OK [NOMODSEQ] Sorry, this mailbox format doesn't support modsequences");
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
        Assert.assertEquals(minfo.highestmodseq, -1, "highestmodseq mismatched.");
        Assert.assertEquals(minfo.uidvalidity, 1459808247, "uidvalidity mismatched.");
        Assert.assertEquals(minfo.uidnext, 150400, "uidnext mismatched.");
        Assert.assertEquals(minfo.getMailboxId(), "214-mailbox", "MailboxId mismatched.");
        Assert.assertTrue(minfo.isNoModSeq(), "isNoModSeq() mismatched.");
        Assert.assertNull(content[6], "This element should be nulled out");
        Assert.assertNull(content[7], "This element should be nulled out");
    }
}
