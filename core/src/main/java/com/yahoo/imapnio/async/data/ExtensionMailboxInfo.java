package com.yahoo.imapnio.async.data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sun.mail.iap.ParsingException;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.MailboxInfo;

/**
 * This class provides the mailbox information and extension items.
 */
public class ExtensionMailboxInfo extends MailboxInfo {

    /** Literal for MAILBOXID. */
    private static final String MAILBOX_ID = "MAILBOXID";

    /** Variable to store mailbox Id. */
    private String mailboxId;

    /**
     * Initializes an instance of {@link ExtensionMailboxInfo} from the server responses for the select or examine command.
     *
     * @param resps response array from server
     * @throws ParsingException for errors parsing the responses
     */
    public ExtensionMailboxInfo(@Nonnull final IMAPResponse[] resps) throws ParsingException {
        super(resps);
        for (int i = 0; i < resps.length; i++) {
            if (resps[i] == null) { // since MailboxInfo nulls it out when finishing parsing an identified response
                continue;
            }
            final IMAPResponse ir = resps[i];

            ir.skipSpaces();
            if (ir.readByte() != '[') {
                ir.reset();
                continue;
            }

            String key = ir.readAtom();
            if (key == null) { // no key present
                ir.reset();
                continue;
            }
            key = key.toUpperCase();
            if (key.equals(MAILBOX_ID)) { // example when 26 is the mailbox id:"* OK [MAILBOXID (26)] Ok"
                final String[] values = ir.readSimpleList(); // reading the string, aka as above example, "(26)", within parentheses
                if (values != null && values.length >= 1) {
                    mailboxId = values[0];
                    resps[i] = null; // Nulls out this element in array to be consistent with MailboxInfo behavior
                    break;
                }
            }
            ir.reset(); // default back the parsing index
        }
    }

    /**
     * @return MAILBOXID, a server-allocated unique identifier for each mailbox. Please refer to OBJECTID, RFC 8474, for more detail.
     */
    @Nullable
    public String getMailboxId() {
        return mailboxId;
    }
}
