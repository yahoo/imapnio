package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;
import javax.mail.Flags;

import com.sun.mail.imap.protocol.MessageSet;

/**
 * This class defines imap store command request from client.
 */
public class StoreFlagsCommand extends ImapRequestAdapter {

    /** UID STORE and space. */
    private static final String UID_STORE_SPACE = "UID STORE ";

    /** Literal for STORE. */
    private static final String STORE_SP = "STORE ";

    /** Literal for FLAGS. */
    private static final String FLAGS_SP = "FLAGS ";

    /** True if the message number represents UID; false if it represents Message Sequences. */
    private boolean isUid;

    /** A collection of messages id specified based on RFC3501 syntax. */
    private String msgIds;

    /** Messagesflags. */
    private Flags flags;

    /** Flag to inidicate whether to add or remove existing. */
    private final boolean isSet;

    /**
     * Initializes a @{code StoreFlagsCommand} with the MessageSet array.
     *
     * @param isUid either uid or message sequence
     * @param msgsets the set of message set
     * @param flags the flags to be stored
     * @param set true if the specified flags are to be set, false to clear them
     */
    public StoreFlagsCommand(final boolean isUid, @Nonnull final MessageSet[] msgsets, @Nonnull final Flags flags, final boolean set) {
        this(isUid, MessageSet.toString(msgsets), flags, set);
    }

    /**
     * Initializes a @{code StoreFlagsCommand} with the start and end message sequence.
     *
     * @param isUid either uid or message sequence
     * @param start the starting message sequence
     * @param end the ending message sequence
     * @param flags the flags to be stored
     * @param set true if the specified flags are to be set, false to clear them
     */
    public StoreFlagsCommand(final boolean isUid, final int start, final int end, @Nonnull final Flags flags, final boolean set) {
        this(isUid, new StringBuilder(String.valueOf(start)).append(ImapClientConstants.COLON).append(String.valueOf(end)).toString(), flags, set);
    }

    /**
     * Initializes a @{code StoreFlagsCommand} with the msg string directly.
     *
     * @param isUid either uid or message sequence
     * @param msgset the messages set
     * @param flags the flags to be stored
     * @param set true if the specified flags are to be set, false to clear them
     */
    private StoreFlagsCommand(@Nonnull final boolean isUid, @Nonnull final String msgset, @Nonnull final Flags flags, final boolean set) {
        this.isUid = isUid;
        this.msgIds = msgset;
        this.flags = flags;
        this.isSet = set;
    }

    @Override
    public void cleanup() {
        this.msgIds = null;
        this.flags = null;
    }

    @Override
    public String getCommandLine() {
        // Ex:STORE 2:4 +FLAGS (\Deleted)
        final ImapArgumentFormatter argWriter = new ImapArgumentFormatter();
        final StringBuilder sb = new StringBuilder(isUid ? UID_STORE_SPACE : STORE_SP).append(msgIds).append(ImapClientConstants.SPACE);

        sb.append(isSet ? ImapClientConstants.PLUS : ImapClientConstants.MINUS).append(FLAGS_SP).append(argWriter.buildFlagString(flags))
                .append(ImapClientConstants.CRLF);

        return sb.toString();
    }
}
