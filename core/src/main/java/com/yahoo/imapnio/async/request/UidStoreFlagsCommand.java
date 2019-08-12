package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;
import javax.mail.Flags;

import com.sun.mail.imap.protocol.UIDSet;

/**
 * This class defines IMAP UID store command request from client.
 */
public class UidStoreFlagsCommand extends ImapRequestAdapter {

    /** UID STORE and space. */
    private static final String UID_STORE_SPACE = "UID STORE ";

    /** Literal for FLAGS. */
    private static final String FLAGS_SP = "FLAGS ";

    /** A collection of messages id specified based on RFC3501 syntax. */
    private String uids;

    /** Messages flags. */
    private Flags flags;

    /** Flag to indicate whether to add or remove existing. */
    private final boolean isSet;

    /**
     * Initializes a @{code UidStoreFlagsCommand} with the UIDSet array.
     *
     * @param uidsets the set of uid set
     * @param flags the flags to be stored
     * @param set true if the specified flags are to be set, false to clear them
     */
    public UidStoreFlagsCommand(@Nonnull final UIDSet[] uidsets, @Nonnull final Flags flags, final boolean set) {
        this(UIDSet.toString(uidsets), flags, set);
    }

    /**
     * Initializes a @{code UidStoreFlagsCommand} with the uid string directly.
     *
     * @param uids the messages set
     * @param flags the flags to be stored
     * @param set true if the specified flags are to be set, false to clear them
     */
    public UidStoreFlagsCommand(@Nonnull final String uids, @Nonnull final Flags flags, final boolean set) {
        this.uids = uids;
        this.flags = flags;
        this.isSet = set;
    }

    @Override
    public void cleanup() {
        this.uids = null;
        this.flags = null;
    }

    @Override
    public String getCommandLine() {
        // Ex:STORE 2:4 +FLAGS (\Deleted)
        final ImapArgumentFormatter argWriter = new ImapArgumentFormatter();
        final StringBuilder sb = new StringBuilder(UID_STORE_SPACE).append(uids).append(ImapClientConstants.SPACE);

        sb.append(isSet ? ImapClientConstants.PLUS : ImapClientConstants.MINUS).append(FLAGS_SP).append(argWriter.buildFlagString(flags))
                .append(ImapClientConstants.CRLF);

        return sb.toString();
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.UID_STORE_FLAGS;
    }
}
