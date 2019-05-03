package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;

/**
 * This class defines imap selecct command request from client.
 */
abstract class AbstractQueryFoldersCommand extends ImapRequestAdapter {

    /** The Command. */
    private String op;

    /** reference name. */
    private String ref;

    /** search pattern. */
    private String pattern;

    /**
     * Initializes with command name, reference name, and pattern.
     *
     * @param op command/operator name, for ex, "LIST"
     * @param ref the reference string
     * @param pattern folder name with possible wildcards, see RFC3501 list command for detail.
     */
    AbstractQueryFoldersCommand(@Nonnull final String op, @Nonnull final String ref, @Nonnull final String pattern) {
        this.op = op;
        this.ref = ref;
        this.pattern = pattern;
    }

    @Override
    public void cleanup() {
        this.op = null;
        this.ref = null;
        this.pattern = null;
    }

    @Override
    public String getCommandLine() {
        // Ex:LIST /usr/staff/jones ""

        // encode the arguments as per RFC2060
        final String ref64 = BASE64MailboxEncoder.encode(ref);
        final String pat64 = BASE64MailboxEncoder.encode(pattern);

        final int len = 2 * ref64.length() + 2 * pat64.length() + ImapClientConstants.PAD_LEN;
        final StringBuilder sb = new StringBuilder(len).append(op);
        sb.append(ImapClientConstants.SPACE);

        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
        formatter.formatArgument(ref64, sb, false);
        sb.append(ImapClientConstants.SPACE);

        formatter.formatArgument(pat64, sb, false);
        sb.append(ImapClientConstants.CRLF);

        return sb.toString();
    }
}
