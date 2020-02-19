package com.yahoo.imapnio.async.request;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@link ImapCommandType}.
 */
public class ImapCommandTypeTest {

    /**
     * Tests CommandType enum.
     */
    @Test
    public void testCommandTypeEnum() {
        final ImapCommandType[] enumList = ImapCommandType.values();
        Assert.assertEquals(enumList.length, 36, "The enum count mismatched.");
        final ImapCommandType uidFetch = ImapCommandType.valueOf("UID_FETCH");
        Assert.assertSame(uidFetch, ImapCommandType.UID_FETCH, "Enum does not match.");
    }
}
