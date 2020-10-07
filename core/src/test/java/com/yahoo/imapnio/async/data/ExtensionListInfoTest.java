package com.yahoo.imapnio.async.data;

import java.io.IOException;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.ListInfo;
import com.yahoo.imapnio.async.data.ExtensionListInfo.ExtendedListAttribute;

/**
 * Unit test for {@link ExtensionListInfo}.
 */
public class ExtensionListInfoTest {

    /**
     * Tests calling constructor and verifies the result.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testCreateExtensionListInfo() throws IOException, ProtocolException {

        {
            // test some attributes present, some not
            final IMAPResponse listResp = new IMAPResponse("* LIST (\\HasChildren \\NonExistent) \"/\" \"SomeInvisibleFolder\"");
            final ExtensionListInfo linfo = new ExtensionListInfo(listResp);
            final Set<ExtendedListAttribute> presented = linfo.getAvailableExtendedAttributes();
            Assert.assertTrue(presented.contains(ExtensionListInfo.ExtendedListAttribute.HAS_CHILDREN), "should have HasChildren");
            Assert.assertTrue(presented.contains(ExtensionListInfo.ExtendedListAttribute.NON_EXISTENT), "should have NonExistent");
            Assert.assertFalse(linfo.canOpen, "linfo.canOpen mismatched since NonExistent present");
            Assert.assertFalse(presented.contains(ExtensionListInfo.ExtendedListAttribute.REMOTE), "should NOT have Remote");
            Assert.assertFalse(presented.contains(ExtensionListInfo.ExtendedListAttribute.SUBSCRIBED), "should NOT have Subscribed");
            Assert.assertFalse(presented.contains(ExtensionListInfo.ExtendedListAttribute.HAS_NO_CHILDREN), "should NOT have HasNoChildren");
        }
        {
            // test all of attributes present, cases are different
            final IMAPResponse listResp = new IMAPResponse(
                    "* LIST (\\HasCHILdren \\HASNoChildren \\REMOTE \\SuBScribed \\NONExistent) \"/\" \"ABCFolder\"");
            final ExtensionListInfo linfo = new ExtensionListInfo(listResp);
            final Set<ExtendedListAttribute> presented = linfo.getAvailableExtendedAttributes();
            Assert.assertTrue(presented.contains(ExtensionListInfo.ExtendedListAttribute.HAS_CHILDREN), "should have HasChildren");
            Assert.assertTrue(presented.contains(ExtensionListInfo.ExtendedListAttribute.NON_EXISTENT), "should have NonExistent");
            Assert.assertFalse(linfo.canOpen, "linfo.canOpen mismatched since NonExistent present");
            Assert.assertTrue(presented.contains(ExtensionListInfo.ExtendedListAttribute.REMOTE), "should have Remote");
            Assert.assertTrue(presented.contains(ExtensionListInfo.ExtendedListAttribute.SUBSCRIBED), "should have Subscribed");
            Assert.assertTrue(presented.contains(ExtensionListInfo.ExtendedListAttribute.HAS_NO_CHILDREN), "should have HasNoChildren");
        }
        {
            // test none of the attributes that ExtensionListInfo cares is present
            final IMAPResponse listResp = new IMAPResponse("* LIST (\\Marked \\Noselect \\HelloWorldAttribute) \"/\" \"SomeInvisibleFolder\"");
            final ExtensionListInfo linfo = new ExtensionListInfo(listResp);
            final Set<ExtendedListAttribute> presented = linfo.getAvailableExtendedAttributes();
            Assert.assertFalse(presented.contains(ExtensionListInfo.ExtendedListAttribute.HAS_CHILDREN), "should NOT have HasChildren");
            Assert.assertFalse(presented.contains(ExtensionListInfo.ExtendedListAttribute.NON_EXISTENT), "should NOT have NonExistent");
            Assert.assertFalse(presented.contains(ExtensionListInfo.ExtendedListAttribute.REMOTE), "should NOT have Remote");
            Assert.assertFalse(presented.contains(ExtensionListInfo.ExtendedListAttribute.SUBSCRIBED), "should NOT have Subscribed");
            Assert.assertFalse(presented.contains(ExtensionListInfo.ExtendedListAttribute.HAS_NO_CHILDREN), "should NOT have HasNoChildren");
            // test the attributes in ListInfo
            Assert.assertEquals(linfo.changeState, ListInfo.CHANGED, "linfo.changeState mismatched");
            Assert.assertFalse(linfo.canOpen, "linfo.canOpen mismatched since Noselect present");
        }
        {
            // test HasChildren attribute present
            final IMAPResponse listResp = new IMAPResponse("* LIST (\\HasChildren) \"/\" \"SomeInvisibleFolder\"");
            final ExtensionListInfo linfo = new ExtensionListInfo(listResp);
            final Set<ExtendedListAttribute> presented = linfo.getAvailableExtendedAttributes();
            Assert.assertTrue(presented.contains(ExtensionListInfo.ExtendedListAttribute.HAS_CHILDREN), "should have HasChildren");
            Assert.assertFalse(presented.contains(ExtensionListInfo.ExtendedListAttribute.NON_EXISTENT), "should NOT have NonExistent");
            Assert.assertTrue(linfo.canOpen, "linfo.canOpen mismatched since NonExistent nor Noselect exist");
            Assert.assertFalse(presented.contains(ExtensionListInfo.ExtendedListAttribute.REMOTE), "should NOT have Remote");
            Assert.assertFalse(presented.contains(ExtensionListInfo.ExtendedListAttribute.SUBSCRIBED), "should NOT have Subscribed");
            Assert.assertFalse(presented.contains(ExtensionListInfo.ExtendedListAttribute.HAS_NO_CHILDREN), "should NOT have HasNoChildren");
        }
    }

}
