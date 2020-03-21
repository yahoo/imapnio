package com.yahoo.imapnio.async.request;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for {@link ImapRFCSupportedCommandType}.
 */
public class ImapRFCSupportedCommandTypeTest {

    /**
     * Tests CommandType enum.
     */
    @Test
    public void testCommandTypeEnum() {
        final ImapRFCSupportedCommandType[] enumList = ImapRFCSupportedCommandType.values();
        Assert.assertEquals(enumList.length, 36, "The enum count mismatched.");
        final ImapRFCSupportedCommandType uidFetch = ImapRFCSupportedCommandType.valueOf("UID_FETCH");
        Assert.assertSame(uidFetch, ImapRFCSupportedCommandType.UID_FETCH, "Enum does not match.");
    }
}
