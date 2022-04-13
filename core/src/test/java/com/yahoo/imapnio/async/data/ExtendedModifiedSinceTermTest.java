package com.yahoo.imapnio.async.data;

import javax.mail.Flags;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.request.EntryTypeRequest;

/**
 * Unit test for {@code ExtendedModifiedSinceTerm}.
 */
public class ExtendedModifiedSinceTermTest {

    /**
     * Tests ExtendedModifiedSinceTerm constructor and getters.
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testExtendedModifiedSinceTermWithOptionalField() throws ImapAsyncClientException {
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        final ExtendedModifiedSinceTerm extendedModifiedSinceTerm = new ExtendedModifiedSinceTerm(flags, EntryTypeRequest.ALL, 1L);

        Assert.assertEquals(extendedModifiedSinceTerm.getModSeq(), 1L, "getModSeq() mismatched.");
        Assert.assertNotNull(extendedModifiedSinceTerm.getEntryName(), "getEntryName() should not return null.");
        Assert.assertTrue(extendedModifiedSinceTerm.getEntryName().contains(Flags.Flag.SEEN), "getEntryName() mismatched.");
        Assert.assertNotNull(extendedModifiedSinceTerm.getEntryType(), "getEntryType() should not return null.");
        Assert.assertEquals(extendedModifiedSinceTerm.getEntryType().name(), "ALL", "getEntryType() mismatched.");
    }

    /**
     * Tests ExtendedModifiedSinceTerm constructor and getters.
     */
    @Test
    public void testExtendedModifiedSinceTermWithoutOptionalField() {
        final ExtendedModifiedSinceTerm extendedModifiedSinceTerm = new ExtendedModifiedSinceTerm(1L);

        Assert.assertEquals(extendedModifiedSinceTerm.getModSeq(), 1L, "getModSeq() mismatched.");
        Assert.assertNull(extendedModifiedSinceTerm.getEntryName(), "Entry name should be null");
        Assert.assertNull(extendedModifiedSinceTerm.getEntryType(), "Entry type should be null.");
    }

    /**
     * Tests ExtendedModifiedSinceTerm match throws UnsupportedOperationException.
     */
    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testExtendedModifiedSinceTermMatchException() {
        final ExtendedModifiedSinceTerm extendedModifiedSinceTerm = new ExtendedModifiedSinceTerm(1L);
        extendedModifiedSinceTerm.match(null);
    }

    /**
     * Tests buildEntryFlagName with ImapAsyncClientException.
     */
    @Test
    public void testBuildEntryFlagNameFailed() {
        final Flags flags = new Flags();
        flags.add(Flags.Flag.ANSWERED);
        flags.add(Flags.Flag.DELETED);
        ImapAsyncClientException actual = null;

        try {
            new ExtendedModifiedSinceTerm(flags, EntryTypeRequest.ALL, 1L);
        } catch (final ImapAsyncClientException e) {
            actual = e;
        }
        Assert.assertNotNull(actual, "Should throw ImapAsyncClientException");
        Assert.assertEquals(actual.getFailureType(), ImapAsyncClientException.FailureType.INVALID_INPUT, "FailureType should be INVALID_INPUT");
    }
}
