package com.yahoo.imapnio.async.data;

import javax.annotation.Nonnull;

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
    private Integer mailboxId;

    /**
     * Initializes an instance of @{code ExtensionMailboxInfo} with the server responses to select or examine command.
     *
     * @param resps response array
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

            final String s = ir.readAtom().toUpperCase();
            boolean handled = false;
            if (s.equals(MAILBOX_ID)) { // example where 26 is the mailbox id:"* OK [MAILBOXID (26)] Ok"
                final String[] values = ir.readSimpleList(); // reading the string within parentheses
                if (values != null && values.length >= 1) {
                    mailboxId = Integer.valueOf(values[0]);
                    handled = true;
                }
            }
            if (handled) {
                resps[i] = null; // remove this response
            } else {
                ir.reset(); // so ALERT can be read
            }
        }
    }

    /**
     * @return MAILBOXID, a server-allocated unique identifier for each mailbox. Please refer to OBJECTID, RFC 8474, for more detail.
     */
    public Integer getMailboxId() {
        return mailboxId;
    }
}
