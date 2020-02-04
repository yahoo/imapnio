package com.yahoo.imapnio.async.data;

import javax.mail.Flags;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.imap.IMAPMessage;
import com.yahoo.imapnio.async.request.EntryTypeReq;

/**
 * Unit test for {@code ExtendedModifiedSinceTerm}.
 */
public class ExtendedModifiedSinceTermTest {

    /**
     * Tests ExtendedModifiedSinceTerm constructor and getters.
     */
    @Test
    public void testExtendedModifiedSinceTermWithOptionalField() {
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
     * Tests ExtendedModifiedSinceTerm match null.
     */
    @Test
    public void testExtendedModifiedSinceTermMatchNull() {
        final ExtendedModifiedSinceTerm extendedModifiedSinceTerm = new ExtendedModifiedSinceTerm(1L);
        final IMAPMessage imapMessage = null;

        Assert.assertFalse(extendedModifiedSinceTerm.match(imapMessage), "match() mismatched.");
    }

    /**
     * Tests ExtendedModifiedSinceTerm not match.
     */
    @Test
    public void testExtendedModifiedSinceTermNotMatch() {
        final ExtendedModifiedSinceTerm extendedModifiedSinceTerm = new ExtendedModifiedSinceTerm(1L);
        final IMAPMessage imapMessage = Mockito.mock(IMAPMessage.class);

        Assert.assertFalse(extendedModifiedSinceTerm.match(imapMessage), "match() mismatched.");
    }

    /**
     * Tests ExtendedModifiedSinceTerm match.
     */
    @Test
    public void testExtendedModifiedSinceTermMatch() {
        final ExtendedModifiedSinceTerm extendedModifiedSinceTerm = new ExtendedModifiedSinceTerm(-1L);
        final IMAPMessage imapMessage = Mockito.mock(IMAPMessage.class);

        Assert.assertEquals(extendedModifiedSinceTerm.match(imapMessage), true, "match() mismatched.");
    }
}
