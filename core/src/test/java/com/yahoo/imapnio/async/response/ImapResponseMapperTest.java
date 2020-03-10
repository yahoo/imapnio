package com.yahoo.imapnio.async.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Flags.Flag;
import javax.mail.Folder;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.CopyUID;
import com.sun.mail.imap.protocol.ID;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.ListInfo;
import com.sun.mail.imap.protocol.MailboxInfo;
import com.sun.mail.imap.protocol.Status;
import com.yahoo.imapnio.async.data.Capability;
import com.yahoo.imapnio.async.data.ExtensionMailboxInfo;
import com.yahoo.imapnio.async.data.IdResult;
import com.yahoo.imapnio.async.data.ListInfoList;
import com.yahoo.imapnio.async.data.SearchResult;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

/**
 * Unit test for {@link ImapResponseMapper}.
 */
public class ImapResponseMapperTest {
    /** Imap server greeting. */
    private static final String GREETING = "* OK [CAPABILITY IMAP4rev1 SASL-IR AUTH=PLAIN AUTH=XOAUTH2 AUTH=OAUTHBEARER ID MOVE NAMESPACE "
            + "XYMHIGHESTMODSEQ UIDPLUS LITERAL+ CHILDREN X-MSG-EXT] IMAP4rev1 Hello";

    /**
     * Tests parseToCapabilities method successfully.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseToCapabilitiesFromCapaCommand() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final List<IMAPResponse> content = new ArrayList<>();
        content.add(new IMAPResponse("* some junks\r\n"));
        content.add(new IMAPResponse("* CAPABILITY IMAP4rev1 SASL-IR AUTH=PLAIN AUTH=XOAUTH2 AUTH=OAUTHBEARER ID MOVE NAMESPACE\r\n"));
        content.add(new IMAPResponse("* more junks\r\n"));
        content.add(new IMAPResponse("a1 OK CAPABILITY completed\r\n"));
        final Capability capa = mapper.readValue(content.toArray(new IMAPResponse[0]), Capability.class);

        // verify the result
        Assert.assertNotNull(capa, "result should never return null.");
        Assert.assertTrue(capa.hasCapability("IMAP4rev1".toUpperCase()), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("SASL-IR"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("ID"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("MOVE"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("NAMESPACE"), "One capability missed.");
        final List<String> authValues = capa.getCapability("AUTH");
        Assert.assertNotNull(authValues, "AUTH values missed.");
        Assert.assertEquals(authValues.size(), 3, "One Auth value missed");
        Assert.assertEquals(authValues.get(0), "PLAIN", "One Auth value missed");
        Assert.assertEquals(authValues.get(1), "XOAUTH2", "One Auth value missed");
        Assert.assertEquals(authValues.get(2), "OAUTHBEARER", "One Auth value missed");
    }

    /**
     * Tests parseToCapabilities method when ImapResponse array has zero length.
     *
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseToCapabilitiesArrayLengthZero() throws ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = {};

        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, Capability.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests parseToCapabilities method successfully from an OK response that has Capability response attached to.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseToCapabilitiesFromGreeting() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = { new IMAPResponse(GREETING) };
        final Capability capa = mapper.readValue(content, Capability.class);

        // verify the result
        Assert.assertNotNull(capa, "result should never return null.");
        Assert.assertTrue(capa.hasCapability("IMAP4rev1".toUpperCase()), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("SASL-IR"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("ID"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("MOVE"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("NAMESPACE"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("X-MSG-EXT"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("LITERAL+"), "One capability missed.");
        final List<String> authValues = capa.getCapability("AUTH");
        Assert.assertNotNull(authValues, "AUTH values missed.");
        Assert.assertEquals(authValues.size(), 3, "One Auth value missed");
        Assert.assertEquals(authValues.get(0), "PLAIN", "One Auth value missed");
        Assert.assertEquals(authValues.get(1), "XOAUTH2", "One Auth value missed");
        Assert.assertEquals(authValues.get(2), "OAUTHBEARER", "One Auth value missed");
    }

    /**
     * Tests parseToCapabilities method successfully when it has Netscape Messaging Server response.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseToCapabilitiesSkipStar() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = {
                new IMAPResponse("* CAPABILITY * IMAP4rev1 SASL-IR AUTH=PLAIN AUTH=XOAUTH2 AUTH=OAUTHBEARER ID MOVE NAMESPACE") };
        final Capability capa = mapper.readValue(content, Capability.class);

        // verify the result
        Assert.assertTrue(capa.hasCapability("IMAP4rev1"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("SASL-IR"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("ID"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("MOVE"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("NAMESPACE"), "One capability missed.");
        final List<String> authValues = capa.getCapability("AUTH");
        Assert.assertNotNull(authValues, "AUTH values missed.");
        Assert.assertEquals(authValues.size(), 3, "One Auth value missed");
        Assert.assertEquals(authValues.get(0), "PLAIN", "One Auth value missed");
        Assert.assertEquals(authValues.get(1), "XOAUTH2", "One Auth value missed");
        Assert.assertEquals(authValues.get(2), "OAUTHBEARER", "One Auth value missed");
    }

    /**
     * Tests parseCopyUid successfully.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseCopyUidSuccess() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[5];
        content[0] = new IMAPResponse("* OK [COPYUID 1549405125 150395 3]"); // a good response
        content[1] = new IMAPResponse("* BAD Some junks"); // test if it skips the bad response
        content[2] = null; // test if it skips null
        content[3] = new IMAPResponse("* OK [SOMETHING 111 222 3]"); // test if it detects it is not COPYUID keyword
        content[4] = new IMAPResponse("* OK"); // test when b is 0

        final CopyUID copyUid = mapper.readValue(content, CopyUID.class);

        // verify the result
        Assert.assertNotNull(copyUid, "result mismatched.");
        Assert.assertEquals(copyUid.uidvalidity, 1549405125, "result mismatched.");
        Assert.assertNotNull(copyUid.src, "result mismatched.");
        Assert.assertNotNull(copyUid.dst, "result mismatched.");
        Assert.assertEquals(copyUid.src.length, 1, "result mismatched.");
        Assert.assertEquals(copyUid.src[0].start, 150395, "result mismatched.");
        Assert.assertEquals(copyUid.src[0].end, 150395, "result mismatched.");
        Assert.assertEquals(copyUid.dst.length, 1, "result mismatched.");
        Assert.assertEquals(copyUid.dst[0].start, 3, "result mismatched.");
        Assert.assertEquals(copyUid.dst[0].end, 3, "result mismatched.");
    }

    /**
     * Tests ImapResponseParse parseCopyUid when Responses array is empty.
     *
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseCopyUidResponseArrayZero() throws ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[0];

        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, CopyUID.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");

    }

    /**
     * Tests parseAppendUid successfully.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseAppendUidSuccess() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[3];
        content[0] = new IMAPResponse("+ Ready for literal data");
        content[1] = new IMAPResponse("* 3 EXISTS");
        content[2] = new IMAPResponse("a5 OK [APPENDUID 1459808247 150399] APPEND completed");
        final AppendUID appendUid = mapper.readValue(content, AppendUID.class);

        // verify the result
        Assert.assertNotNull(appendUid, "result mismatched.");
        Assert.assertEquals(appendUid.uidvalidity, 1459808247, "result mismatched.");
        Assert.assertEquals(appendUid.uid, 150399, "result mismatched.");
    }

    /**
     * Tests parseToCapabilities method when ImapResponse array has zero length.
     *
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseToAppendUidsArrayLengthZero() throws ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = {};

        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, AppendUID.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests parseAppendUid with a BAD response.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseAppendUidNotOK() throws IOException, ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = { new IMAPResponse("* BAD Some junks") };

        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, AppendUID.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");

    }

    /**
     * Tests parseAppendUid when b is 0, or when left bracket is not found.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseAppendUidByteReadExhausted() throws IOException, ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = { new IMAPResponse("* OK") }; // test when b is 0

        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, AppendUID.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests parseAppendUid when b is 0.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseAppendUidNoAppendUidKeyword() throws IOException, ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = { new IMAPResponse("* OK [appendButNotUid 111 222 3]") }; // test when b is 0

        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, AppendUID.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");

    }

    /**
     * Tests parseMailboxInfo method successfully.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseMailboxInfoReadOnlySuccess() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[8];
        content[0] = new IMAPResponse("* 3 EXISTS");
        content[1] = new IMAPResponse("* 0 RECENT");
        content[2] = new IMAPResponse("* OK [UIDVALIDITY 1459808247] UIDs valid");
        content[3] = new IMAPResponse("* OK [UIDNEXT 150400] Predicted next UID");
        content[4] = new IMAPResponse("* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen $Forwarded $Junk $NotJunk)");
        content[5] = new IMAPResponse("* OK [PERMANENTFLAGS ()] No permanent flags permitted");
        content[6] = new IMAPResponse("* OK [HIGHESTMODSEQ 614]");
        content[7] = new IMAPResponse("002 OK [READ-ONLY] EXAMINE completed; now in selected state");
        final MailboxInfo minfo = mapper.readValue(content, MailboxInfo.class);

        // verify the result
        Assert.assertNotNull(minfo, "result mismatched.");
        Assert.assertEquals(minfo.mode, Folder.READ_ONLY, "mode mismatched.");
        Assert.assertNotNull(minfo.availableFlags, "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.ANSWERED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DELETED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DRAFT), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.FLAGGED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.SEEN), "availableFlags mismatched.");
        Assert.assertEquals(minfo.highestmodseq, 614, "highestmodseq mismatched.");
        Assert.assertEquals(minfo.uidvalidity, 1459808247, "uidvalidity mismatched.");
        Assert.assertEquals(minfo.uidnext, 150400, "uidnext mismatched.");
    }

    /**
     * Tests parseExtensionMailboxInfo method successfully.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseExtensionMailboxInfoReadOnlySuccess() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[9];
        content[0] = new IMAPResponse("* 3 EXISTS");
        content[1] = new IMAPResponse("* 0 RECENT");
        content[2] = new IMAPResponse("* OK [UIDVALIDITY 1459808247] UIDs valid");
        content[3] = new IMAPResponse("* OK [UIDNEXT 150400] Predicted next UID");
        content[4] = new IMAPResponse("* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen $Forwarded $Junk $NotJunk)");
        content[5] = new IMAPResponse("* OK [PERMANENTFLAGS ()] No permanent flags permitted");
        content[6] = new IMAPResponse("* OK [HIGHESTMODSEQ 614]");
        content[7] = new IMAPResponse("* OK [MAILBOXID (A26)] Ok");
        content[8] = new IMAPResponse("002 OK [READ-ONLY] EXAMINE completed; now in selected state");
        final ExtensionMailboxInfo minfo = mapper.readValue(content, ExtensionMailboxInfo.class);

        // verify the result
        Assert.assertNotNull(minfo, "result mismatched.");
        Assert.assertEquals(minfo.mode, Folder.READ_ONLY, "mode mismatched.");
        Assert.assertNotNull(minfo.availableFlags, "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.ANSWERED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DELETED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DRAFT), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.FLAGGED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.SEEN), "availableFlags mismatched.");
        Assert.assertEquals(minfo.highestmodseq, 614, "highestmodseq mismatched.");
        Assert.assertEquals(minfo.uidvalidity, 1459808247, "uidvalidity mismatched.");
        Assert.assertEquals(minfo.uidnext, 150400, "uidnext mismatched.");
        Assert.assertEquals(minfo.getMailboxId(), "A26", "MailboxId mismatched.");
    }

    /**
     * Tests parseMailboxInfo method successfully with READ-WRITE mode.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseMailboxInfoReadWriteSuccess() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[8];
        content[0] = new IMAPResponse("* 3 EXISTS");
        content[1] = new IMAPResponse("* 0 RECENT");
        content[2] = new IMAPResponse("* OK [UIDVALIDITY 1459808247] UIDs valid");
        content[3] = new IMAPResponse("* OK [UIDNEXT 150400] Predicted next UID");
        content[4] = new IMAPResponse("* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen $Forwarded $Junk $NotJunk)");
        content[5] = new IMAPResponse("* OK [PERMANENTFLAGS ()] No permanent flags permitted");
        content[6] = new IMAPResponse("* OK [HIGHESTMODSEQ 614]");
        content[7] = new IMAPResponse("002 OK [READ-WRITE] EXAMINE completed; now in selected state");
        final MailboxInfo minfo = mapper.readValue(content, MailboxInfo.class);

        // verify the result
        Assert.assertNotNull(minfo, "result mismatched.");
        Assert.assertEquals(minfo.mode, Folder.READ_WRITE, "mode mismatched.");
        Assert.assertNotNull(minfo.availableFlags, "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.ANSWERED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DELETED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DRAFT), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.FLAGGED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.SEEN), "availableFlags mismatched.");
        Assert.assertEquals(minfo.highestmodseq, 614, "highestmodseq mismatched.");
        Assert.assertEquals(minfo.uidvalidity, 1459808247, "uidvalidity mismatched.");
        Assert.assertEquals(minfo.uidnext, 150400, "uidnext mismatched.");
    }

    /**
     * Tests parseMailboxInfo method successfully with READ-WRITE mode.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseMailboxInfoBad() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[8];
        content[0] = new IMAPResponse("* 3 EXISTS");
        content[1] = new IMAPResponse("* 0 RECENT");
        content[2] = new IMAPResponse("* OK [UIDVALIDITY 1459808247] UIDs valid");
        content[3] = new IMAPResponse("* OK [UIDNEXT 150400] Predicted next UID");
        content[4] = new IMAPResponse("* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen $Forwarded $Junk $NotJunk)");
        content[5] = new IMAPResponse("* OK [PERMANENTFLAGS ()] No permanent flags permitted");
        content[6] = new IMAPResponse("* OK [HIGHESTMODSEQ 614]");
        content[7] = new IMAPResponse("002 BAD"); // make it bad so it does not update mode
        final MailboxInfo minfo = mapper.readValue(content, MailboxInfo.class);

        // verify the result
        Assert.assertNotNull(minfo, "result mismatched.");
        Assert.assertEquals(minfo.mode, 0, "mode mismatched.");
        Assert.assertNotNull(minfo.availableFlags, "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.ANSWERED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DELETED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.DRAFT), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.FLAGGED), "availableFlags mismatched.");
        Assert.assertTrue(minfo.availableFlags.contains(Flag.SEEN), "availableFlags mismatched.");
        Assert.assertEquals(minfo.highestmodseq, 614, "highestmodseq mismatched.");
        Assert.assertEquals(minfo.uidvalidity, 1459808247, "uidvalidity mismatched.");
        Assert.assertEquals(minfo.uidnext, 150400, "uidnext mismatched.");
    }

    /**
     * Tests parseMailboxInfo method successfully with READ-WRITE mode.
     *
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseMailboxInfoResponseArray0() throws ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[0];
        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, MailboxInfo.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Adds the data for test input.
     *
     * @param rr output parameter, list of IMAPResponse
     * @param expectedNames output parameter, list of expected folder names
     * @param respStr response string
     * @param folder folder name
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    private void buildListInfoIMAPResponse(final List<IMAPResponse> rr, final List<String> expectedNames, final String respStr, final String folder)
            throws IOException, ProtocolException {
        expectedNames.add(folder);
        rr.add(new IMAPResponse(respStr + " \"" + folder + "\"\r\n"));
    }

    /**
     * Tests parseListInfos method successfully.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseListInfosSuccess() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final List<IMAPResponse> content = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        // add a non-relevant response
        content.add(new IMAPResponse("* 115140 EXPUNGE\r\n"));
        buildListInfoIMAPResponse(content, names, "* LIST (\\Archive \\HasNoChildren) \"/\"", "Archive");
        content.add(new IMAPResponse("* some junks\r\n"));
        buildListInfoIMAPResponse(content, names, "* LIST (\\Junk \\HasNoChildren) \"/\"", "Bulk Mail");
        buildListInfoIMAPResponse(content, names, "* LIST (\\Drafts \\HasNoChildren) \"/\"", "Draft");
        buildListInfoIMAPResponse(content, names, "* LIST (\\HasNoChildren) \"/\"", "Inbox");
        buildListInfoIMAPResponse(content, names, "* LIST (\\Sent \\HasNoChildren) \"/\"", "Sent");
        buildListInfoIMAPResponse(content, names, "* LIST (\\Trash \\HasNoChildren) \"/\"", "Trash");
        buildListInfoIMAPResponse(content, names, "* LIST (\\HasChildren) \"/\"", "test1");
        content.add(new IMAPResponse("* 115141 EXISTS\r\n"));
        buildListInfoIMAPResponse(content, names, "* LIST (\\HasNoChildren) \"/\"", "test1/test1_1");
        content.add(new IMAPResponse("a3 OK LIST completed"));
        final ListInfoList ll = mapper.readValue(content.toArray(new IMAPResponse[0]), ListInfoList.class);
        final List<ListInfo> infos = ll.getListInfo();

        // verify the result
        Assert.assertNotNull(infos, "result mismatched.");
        Assert.assertEquals(infos.size(), 8, "ListInfo count mismatched.");
        Assert.assertEquals(infos.size(), names.size(), "ListInfo count mismatched.");
        for (int i = 0; i < infos.size(); i++) {
            final ListInfo info = infos.get(i);
            final String expectedFolder = names.get(i);
            Assert.assertNotNull(info, "ListInfo should not be null.");
            Assert.assertNotNull(expectedFolder, "folder name should not be null.");
            Assert.assertTrue(info.hasInferiors, "hasInferiors mismatched.");
            Assert.assertNotNull(info.name, "Name mismatched.");
            Assert.assertEquals(info.name, expectedFolder, "folder name mismatched.");
        }
    }

    /**
     * Tests parseListInfos method successfully when responses are results of LSUB command.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseListInfosFromLSubSuccess() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final List<IMAPResponse> content = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        buildListInfoIMAPResponse(content, names, "* LSUB (\\Archive \\HasNoChildren) \"/\"", "Archive");
        content.add(new IMAPResponse("* 115140 EXPUNGE\r\n"));
        content.add(new IMAPResponse("* MORE JUNKS\r\n"));
        buildListInfoIMAPResponse(content, names, "* LSUB (\\Junk \\HasNoChildren) \"/\"", "Bulk Mail");
        buildListInfoIMAPResponse(content, names, "* LSUB (\\Drafts \\HasNoChildren) \"/\"", "Draft");
        buildListInfoIMAPResponse(content, names, "* LSUB (\\HasNoChildren) \"/\"", "Inbox");
        buildListInfoIMAPResponse(content, names, "* LSUB (\\Sent \\HasNoChildren) \"/\"", "Sent");
        buildListInfoIMAPResponse(content, names, "* LSUB (\\Trash \\HasNoChildren) \"/\"", "Trash");
        buildListInfoIMAPResponse(content, names, "* LSUB (\\HasChildren) \"/\"", "test1");
        content.add(new IMAPResponse("* 115141 EXISTS\r\n"));
        buildListInfoIMAPResponse(content, names, "* LSUB (\\HasNoChildren) \"/\"", "test1/test1_1");
        content.add(new IMAPResponse("a3 OK LSUB completed"));
        final ListInfoList ll = mapper.readValue(content.toArray(new IMAPResponse[0]), ListInfoList.class);
        final List<ListInfo> infos = ll.getListInfo();

        // verify the result
        Assert.assertNotNull(infos, "result mismatched.");
        Assert.assertEquals(infos.size(), 8, "ListInfo count mismatched.");
        Assert.assertEquals(infos.size(), names.size(), "ListInfo count mismatched.");
        for (int i = 0; i < infos.size(); i++) {
            final ListInfo info = infos.get(i);
            final String expectedFolder = names.get(i);
            Assert.assertNotNull(info, "ListInfo should not be null.");
            Assert.assertNotNull(expectedFolder, "folder name should not be null.");
            Assert.assertTrue(info.hasInferiors, "hasInferiors mismatched.");
            Assert.assertNotNull(info.name, "Name mismatched.");
            Assert.assertEquals(info.name, expectedFolder, "folder name mismatched.");
        }
    }

    /**
     * Tests parseListInfos method when final response is not OK.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseListInfosNoOK() throws IOException, ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final List<IMAPResponse> content = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        buildListInfoIMAPResponse(content, names, "* LIST (\\Archive \\HasNoChildren) \"/\"", "\"Archive\"");
        buildListInfoIMAPResponse(content, names, "* LIST (\\Junk \\HasNoChildren) \"/\"", "\"Bulk Mail\"");
        buildListInfoIMAPResponse(content, names, "* LIST (\\Drafts \\HasNoChildren) \"/\"", "\"Draft\"");
        buildListInfoIMAPResponse(content, names, "* LIST (\\HasNoChildren) \"/\"", "\"Inbox\"");
        buildListInfoIMAPResponse(content, names, "* LIST (\\Sent \\HasNoChildren) \"/\"", "\"Sent\"");
        buildListInfoIMAPResponse(content, names, "* LIST (\\Trash \\HasNoChildren) \"/\"", "\"Trash\"");
        buildListInfoIMAPResponse(content, names, "* LIST (\\HasChildren) \"/\"", "\"test1\"");
        buildListInfoIMAPResponse(content, names, "* LIST (\\HasNoChildren) \"/\"", "\"test1/test1_1\"");
        content.add(new IMAPResponse("a3 BAD LIST completed"));

        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content.toArray(new IMAPResponse[0]), ListInfoList.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");

    }

    /**
     * Tests parseListInfos method when final response is not OK.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseListInfosOnlyOKResponse() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final List<IMAPResponse> rr = new ArrayList<>();
        rr.add(new IMAPResponse("a3 OK LIST completed"));
        final ListInfoList infos = mapper.readValue(rr.toArray(new IMAPResponse[0]), ListInfoList.class);

        // verify the result
        Assert.assertNotNull(infos, "result mismatched.");
    }

    /**
     * Tests parseListInfos method when response array length is 0.
     *
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseListInfosEmptyResponses() throws ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final List<IMAPResponse> content = new ArrayList<>();
        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content.toArray(new IMAPResponse[0]), ListInfoList.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests ExtensionMailboxInfo method when response array length is 0.
     *
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseMailboxExtensionInfosEmptyResponses() throws ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final List<IMAPResponse> content = new ArrayList<>();
        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content.toArray(new IMAPResponse[0]), ExtensionMailboxInfo.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests parseStatus method with response array 0.
     *
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseStatusArray0() throws ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[0];
        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, Status.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests parse a class that mapper does not support.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseClassUnknown() throws IOException, ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[1];
        content[0] = new IMAPResponse("002 OK"); // make it bad so it does not update mode

        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, ID.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.UNKNOWN_PARSE_RESULT_TYPE, "Failure type mismatched.");
    }

    /**
     * Tests parseStatus method with not OK response.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseStatusNotOK() throws IOException, ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[1];
        content[0] = new IMAPResponse("002 BAD"); // make it bad so it does not update mode

        // verify the result
        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, Status.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests parseStatus method successfully.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseStatusNoStatusResponse() throws IOException, ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[2];
        content[0] = new IMAPResponse("* NOSTATUS blurdybloop (MESSAGES 231 UIDNEXT 44292)");
        content[1] = new IMAPResponse("A042 OK STATUS completed");

        // verify the result
        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, Status.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests parseStatus method successfully.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseStatusOK() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final List<IMAPResponse> content = new ArrayList<>();
        content.add(new IMAPResponse("* STATUS blurdybloop (MESSAGES 231 UIDNEXT 44292 UNSEEN 3)"));
        content.add(new IMAPResponse("* S2TATUS blurdybloop (MESSAGES 232 UIDNEXT 44293)"));
        content.add(new IMAPResponse("* STATUS blurdybloop (UNSEEN 4)"));
        content.add(new IMAPResponse("* STATUS blurdybloop (UIDVALIDITY 999333)"));
        content.add(new IMAPResponse("A042 OK STATUS completed"));

        final Status status = mapper.readValue(content.toArray(new IMAPResponse[0]), Status.class);

        // verify the result
        Assert.assertNotNull(status, "status mismatched.");
        Assert.assertEquals(status.uidnext, 44292, "uidnext mismatched.");
        Assert.assertEquals(status.total, 231, "total mismatched.");
        Assert.assertEquals(status.unseen, 4, "unseen mismatched, should take the latter one.");
        Assert.assertEquals(status.uidvalidity, 999333, "uidvalidity mismatched.");
    }

    /**
     * Tests parseToID method with response array 0.
     *
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseToIdResultArray0() throws ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[0];
        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, IdResult.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests parseToID method with not OK response.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseToIdResultNotOK() throws IOException, ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[1];
        content[0] = new IMAPResponse("002 BAD"); // make it bad so it does not update mode

        // verify the result
        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, IdResult.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests parseToIdResult with first byte is N.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseToIdResultFirstByteIsN() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[2];
        content[0] = new IMAPResponse("* ID NIL \n");
        content[1] = new IMAPResponse("a042 OK ID command completed");

        final IdResult id = mapper.readValue(content, IdResult.class);

        // verify the result
        Assert.assertNotNull(id, "id mismatched.");
        Assert.assertFalse(id.hasKey("name"), "name key mismatched.");
        Assert.assertNull(id.getValue("name"), "name value mismatched.");
    }

    /**
     * Tests parseToIdResult with first byte is n.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseToIdResultFirstByteIsLowercaseN() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[2];
        content[0] = new IMAPResponse("* ID nIL \n");
        content[1] = new IMAPResponse("a042 OK ID command completed");

        final IdResult id = mapper.readValue(content, IdResult.class);

        // verify the result
        Assert.assertNotNull(id, "id mismatched.");
        Assert.assertFalse(id.hasKey("name"), "name key mismatched.");
        Assert.assertNull(id.getValue("name"), "name value mismatched.");
    }

    /**
     * Tests parseToIdResult with first byte not left parenthesis.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseToIdResultNotStartWithLeftParenthesis() throws IOException, ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[2];
        content[0] = new IMAPResponse("* ID X \n");
        content[1] = new IMAPResponse("a042 OK ID command completed");

        // verify the result
        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, IdResult.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests parseToIdResult with first byte not left parenthesis.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseToIdResultNameAbsent() throws IOException, ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[2];
        content[0] = new IMAPResponse("* ID () \n");
        content[1] = new IMAPResponse("a042 OK ID command completed");

        // verify the result
        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, IdResult.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests parseToIdResult with first byte not left parenthesis.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseToIdResultValueAbsent() throws IOException, ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[2];
        content[0] = new IMAPResponse("* ID (') \n");
        content[1] = new IMAPResponse("a042 OK ID command completed");

        // verify the result
        ImapAsyncClientException cause = null;
        try {
            mapper.readValue(content, IdResult.class);
        } catch (final ImapAsyncClientException e) {
            cause = e;
        }
        // verify the result
        Assert.assertNotNull(cause, "cause mismatched.");
        Assert.assertEquals(cause.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests parseToID method successfully.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseToIdResultOK() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[3];
        content[0] = new IMAPResponse("* ID (\"name\" \"Cyrus\" \"version\" \"1.5\" \"os\" \"sunos\"\n\"os-version\" \"5.5\" \"support-url\"\n"
                + "\"mailto:cyrus-bugs+@andrew.cmu.edu\")\n");
        content[1] = new IMAPResponse("junk");
        content[2] = new IMAPResponse("a042 OK ID command completed");

        final IdResult id = mapper.readValue(content, IdResult.class);

        // verify the result
        Assert.assertNotNull(id, "id mismatched.");
        Assert.assertTrue(id.hasKey("name"), "name should be present.");
        Assert.assertEquals(id.getValue("name"), "Cyrus", "name value mismatched.");
    }

    /**
     * Tests parseSearchResult method successfully.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseToSearchResultOK() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[2];
        content[0] = new IMAPResponse("* SEARCH 150404 150406 150407\r\n");
        content[1] = new IMAPResponse("a3 OK UID SEARCH completed\r\n");

        final SearchResult result = mapper.readValue(content, SearchResult.class);

        // verify the result
        Assert.assertNotNull(result, "result mismatched.");
        final List<Long> list = result.getMessageNumbers();
        Assert.assertNotNull(list, "getMessageSequence() mismatched.");
        Assert.assertEquals(list.size(), 3, "getMessageSequence() mismatched.");
        Assert.assertEquals(list.get(0), Long.valueOf(150404), "getMessageSequence() mismatched.");
        Assert.assertEquals(list.get(1), Long.valueOf(150406), "getMessageSequence() mismatched.");
        Assert.assertEquals(list.get(2), Long.valueOf(150407), "getMessageSequence() mismatched.");
    }

    /**
     * Tests parseSearchResult method successfully.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testParseToSearchResultOKNoSearchResult() throws IOException, ProtocolException, ImapAsyncClientException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[2];
        content[0] = new IMAPResponse("* SEARCH\r\n");
        content[1] = new IMAPResponse("a3 OK UID SEARCH completed\r\n");

        final SearchResult result = mapper.readValue(content, SearchResult.class);

        // verify the result
        Assert.assertNotNull(result, "result mismatched.");
        final List<Long> list = result.getMessageNumbers();
        Assert.assertNotNull(list, "getMessageSequence() mismatched.");
        Assert.assertEquals(list.size(), 0, "getMessageSequence() mismatched.");
    }

    /**
     * Tests parseToSearchResult method when tagged response is not OK.
     *
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseToSearchResultZeroLengthResponse() throws ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[0];

        ImapAsyncClientException actual = null;
        try {
            final SearchResult result = mapper.readValue(content, SearchResult.class);
        } catch (final ImapAsyncClientException e) {
            actual = e;
        }
        // verify the result
        Assert.assertNotNull(actual, "ImapAsyncClientException should occur.");
        Assert.assertEquals(actual.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }

    /**
     * Tests parseToSearchResult method when tagged response is not OK.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testParseToSearchResultNotOK() throws IOException, ProtocolException {
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = new IMAPResponse[1];
        content[0] = new IMAPResponse("a3 BAD SEARCH completed (Failure)\r\n");

        ImapAsyncClientException actual = null;
        try {
            final SearchResult result = mapper.readValue(content, SearchResult.class);
        } catch (final ImapAsyncClientException e) {
            actual = e;
        }
        // verify the result
        Assert.assertNotNull(actual, "ImapAsyncClientException should occur.");
        Assert.assertEquals(actual.getFailureType(), FailureType.INVALID_INPUT, "Failure type mismatched.");
    }
}
