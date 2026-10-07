package com.yahoo.imapnio.async.request;

import javax.mail.Flags;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * Unit test for {@link ImapArgumentFormatter}.
 */
public class ImapArgumentFormatterTest {

    /**
     * Tests buildFlagString with all flags.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testBuildFlagString() throws ImapAsyncClientException {
        final Flags flags = new Flags();
        flags.add(Flags.Flag.ANSWERED);
        flags.add(Flags.Flag.DELETED);
        flags.add(Flags.Flag.DRAFT);
        flags.add(Flags.Flag.FLAGGED);
        flags.add(Flags.Flag.RECENT);
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.USER); // for continue
        final ImapArgumentFormatter writer = new ImapArgumentFormatter();
        final String s = writer.buildFlagString(flags);
        Assert.assertNotNull(s, "buildFlagString() should not return null.");
        Assert.assertEquals(s, "(\\Answered \\Deleted \\Draft \\Flagged \\Recent \\Seen)", "result mismatched.");
    }

    /**
     * Tests buildFlagString with user flags.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testBuildFlagStringWithUserFlagString() throws ImapAsyncClientException {
        final Flags flags = new Flags();
        flags.add("userflag1");
        flags.add("userflag2");
        final ImapArgumentFormatter writer = new ImapArgumentFormatter();
        final String s = writer.buildFlagString(flags);
        Assert.assertNotNull(s, "buildFlagString() should not return null.");
        Assert.assertEquals(s, "(userflag1 userflag2)", "result mismatched.");
    }
}
