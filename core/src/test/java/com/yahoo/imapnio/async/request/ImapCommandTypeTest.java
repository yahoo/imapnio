package com.yahoo.imapnio.async.request;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@link ImapCommandRFCSupportedType}.
 */
public class ImapCommandTypeTest {

    /**
     * Tests CommandType enum.
     */
    @Test
    public void testCommandTypeEnum() {
        final ImapCommandRFCSupportedType[] enumList = ImapCommandRFCSupportedType.values();
        Assert.assertEquals(enumList.length, 36, "The enum count mismatched.");
        final ImapCommandRFCSupportedType uidFetch = ImapCommandRFCSupportedType.valueOf("UID_FETCH");
        Assert.assertSame(uidFetch, ImapCommandRFCSupportedType.UID_FETCH, "Enum does not match.");
    }
}
