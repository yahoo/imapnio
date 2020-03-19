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
        final ImapTestCommandType[] enumList = ImapTestCommandType.values();
        Assert.assertEquals(enumList.length, 1, "The enum count mismatched.");
        final ImapTestCommandType testType = ImapTestCommandType.valueOf("TEST");
        Assert.assertSame(testType, ImapTestCommandType.TEST, "Enum does not match.");
    }

    /**
     * Test imap command type.
     */
    enum ImapTestCommandType implements ImapCommandType {
        /** test command. */
        TEST;

        @Override
        public String getType() {
            return this.name();
        }
    }
}
