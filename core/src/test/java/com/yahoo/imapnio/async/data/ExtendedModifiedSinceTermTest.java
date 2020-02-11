package com.yahoo.imapnio.async.data;

import javax.mail.Flags;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.request.EntryTypeReq;
import com.yahoo.imapnio.async.request.ImapArgumentFormatter;

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
        final ExtendedModifiedSinceTerm extendedModifiedSinceTerm = new ExtendedModifiedSinceTerm(flags, EntryTypeReq.ALL, 1L);

        Assert.assertEquals(extendedModifiedSinceTerm.getModSeq(), 1L, "Result mismatched.");
        Assert.assertTrue(extendedModifiedSinceTerm.getEntryName().contains(Flags.Flag.SEEN), "Result mismatched.");
        Assert.assertEquals(extendedModifiedSinceTerm.getEntryType().name(), "ALL", "Result mismatched.");
    }

    /**
     * Tests ExtendedModifiedSinceTerm constructor and getters.
     */
    @Test
    public void testExtendedModifiedSinceTermWithoutOptionalField() {
        final ExtendedModifiedSinceTerm extendedModifiedSinceTerm = new ExtendedModifiedSinceTerm(1L);

        Assert.assertEquals(extendedModifiedSinceTerm.getModSeq(), 1L, "Result mismatched.");
        Assert.assertNull(extendedModifiedSinceTerm.getEntryName(), "Entry name not null");
        Assert.assertNull(extendedModifiedSinceTerm.getEntryType(), "Entry type not null.");
    }

    /**
     * Tests ExtendedModifiedSinceTerm match throw exception.
     */
    @Test
    public void testExtendedModifiedSinceTermMatchException() {
        final ExtendedModifiedSinceTerm extendedModifiedSinceTerm = new ExtendedModifiedSinceTerm(1L);
        UnsupportedOperationException actual = null;
        try {
            extendedModifiedSinceTerm.match(null);
        } catch (final UnsupportedOperationException e) {
            actual = e;
        }
        Assert.assertNotNull(actual, "Should throw exception");
    }

    /**
     * Tests buildEntryFlagName with exception.
     */
    @Test
    public void testbuildEntryFlagNameFailed() {
        final Flags flags = new Flags();
        flags.add(Flags.Flag.ANSWERED);
        flags.add(Flags.Flag.DELETED);
        ImapAsyncClientException actual = null;
        final ImapArgumentFormatter writer = new ImapArgumentFormatter();
        try {
            final ExtendedModifiedSinceTerm extendedModifiedSinceTerm = new ExtendedModifiedSinceTerm(flags, EntryTypeReq.ALL, 1L);
        } catch (final ImapAsyncClientException e) {
            actual = e;
        }
        Assert.assertNotNull(actual, "Should throw exception");
        Assert.assertEquals(actual.getFaiureType(), ImapAsyncClientException.FailureType.INVALID_INPUT, "Should throw exception");
    }
}
