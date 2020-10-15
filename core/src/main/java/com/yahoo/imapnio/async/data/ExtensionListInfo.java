package com.yahoo.imapnio.async.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.sun.mail.iap.ParsingException;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.ListInfo;

/**
 * This class provides the result from LIST-EXTENDED command. It extends from ListInfo so clients can continue use the data from ListInfo. The public
 * methods provided here allows callers to easily identify if server sends the specific interested attribute or not. The attributes supported are
 * listed in RFC 5258.
 */
public class ExtensionListInfo extends ListInfo {
    /**
     * Extended attributes supported for LIST-EXTENDED capability, RFC 5258.
     */
    public enum ExtendedListAttribute {

        /** HasChildren attribute. */
        HAS_CHILDREN("\\HasChildren"),

        /** HasNoChildren attribute. */
        HAS_NO_CHILDREN("\\HasNoChildren"),

        /** NonExistent attribute. */
        NON_EXISTENT("\\NonExistent"),

        /** Remote attribute. */
        REMOTE("\\Remote"),

        /** Subscribed attribute. */
        SUBSCRIBED("\\Subscribed");

        /**
         * Initializes a ExtendedListAttribute instance with the actual keyword.
         *
         * @param value the actual keyword
         */
        ExtendedListAttribute(@Nonnull final String value) {
            this.value = value;
        }

        /**
         * Returns the actual keyword used for IMAP protocol.
         * 
         * @return the actual keyword
         */
        public String getValue() {
            return value;
        }

        /** IMAP attribute sent from server. */
        @Nonnull
        private final String value;
    }

    /** A set of ExtendedListAttribute that are present from server responses. */
    private Set<ExtendedListAttribute> extendedAttrs;

    /**
     * Initializes an instance of {@link ExtensionListInfo} from the server responses. It also parses the RFC 5258 supported list attribute to
     * identify whether the attribute is present or not.
     *
     * @param resp a LIST command response
     * @throws ParsingException for errors parsing the responses
     */
    public ExtensionListInfo(@Nonnull final IMAPResponse resp) throws ParsingException {
        super(resp);
        final Set<ExtendedListAttribute> localSet = new HashSet<ExtendedListAttribute>();

        for (int i = 0; i < attrs.length; i++) {
            // case insensitive to be consistent with ListInfo parsing
            if (ExtendedListAttribute.NON_EXISTENT.getValue().equalsIgnoreCase(attrs[i])) {
                localSet.add(ExtendedListAttribute.NON_EXISTENT);
                // if the folder does not exist, it cannot be open/selected. See https://tools.ietf.org/html/rfc5258#ref-IMAP4
                this.canOpen = false;
            } else if (ExtendedListAttribute.SUBSCRIBED.getValue().equalsIgnoreCase(attrs[i])) {
                localSet.add(ExtendedListAttribute.SUBSCRIBED);
            } else if (ExtendedListAttribute.REMOTE.getValue().equalsIgnoreCase(attrs[i])) {
                localSet.add(ExtendedListAttribute.REMOTE);
            } else if (ExtendedListAttribute.HAS_CHILDREN.getValue().equalsIgnoreCase(attrs[i])) {
                localSet.add(ExtendedListAttribute.HAS_CHILDREN);
            } else if (ExtendedListAttribute.HAS_NO_CHILDREN.getValue().equalsIgnoreCase(attrs[i])) {
                localSet.add(ExtendedListAttribute.HAS_NO_CHILDREN);
            }
        }
        extendedAttrs = Collections.unmodifiableSet(localSet);
    }

    /**
     * @return available extended attributes in a Set data structure
     */
    public Set<ExtendedListAttribute> getAvailableExtendedAttributes() {
        return extendedAttrs;
    }
}
