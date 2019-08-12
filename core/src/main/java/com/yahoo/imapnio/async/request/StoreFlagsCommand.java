package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;
import javax.mail.Flags;

import com.sun.mail.imap.protocol.MessageSet;

/**
 * This class defines imap store command request from client.
 */
public class StoreFlagsCommand extends ImapRequestAdapter {

    /** Literal for STORE. */
    private static final String STORE_SP = "STORE ";

    /** Literal for FLAGS. */
    private static final String FLAGS_SP = "FLAGS ";

    /** A collection of messages id specified based on RFC3501 syntax. */
    private String msgIds;

    /** Messages flags. */
    private Flags flags;

    /** Flag to indicate whether to add or remove existing. */
    private final boolean isSet;

    /**
     * Initializes a @{code StoreFlagsCommand} with the MessageSet array.
     *
     * @param msgsets the set of message set
     * @param flags the flags to be stored
     * @param set true if the specified flags are to be set, false to clear them
     */
    public StoreFlagsCommand(@Nonnull final MessageSet[] msgsets, @Nonnull final Flags flags, final boolean set) {
        this(MessageSet.toString(msgsets), flags, set);
    }

    /**
     * Initializes a @{code StoreFlagsCommand} with the start and end message sequence.
     *
     * @param start the starting message sequence
     * @param end the ending message sequence
     * @param flags the flags to be stored
     * @param set true if the specified flags are to be set, false to clear them
     */
    public StoreFlagsCommand(final int start, final int end, @Nonnull final Flags flags, final boolean set) {
        this(new StringBuilder(String.valueOf(start)).append(ImapClientConstants.COLON).append(String.valueOf(end)).toString(), flags, set);
    }

    /**
     * Initializes a @{code StoreFlagsCommand} with the msg string directly.
     *
     * @param msgset the messages set
     * @param flags the flags to be stored
     * @param set true if the specified flags are to be set, false to clear them
     */
    private StoreFlagsCommand(@Nonnull final String msgset, @Nonnull final Flags flags, final boolean set) {
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
        final StringBuilder sb = new StringBuilder(STORE_SP).append(msgIds).append(ImapClientConstants.SPACE);

        sb.append(isSet ? ImapClientConstants.PLUS : ImapClientConstants.MINUS).append(FLAGS_SP).append(argWriter.buildFlagString(flags))
                .append(ImapClientConstants.CRLF);

        return sb.toString();
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.STORE_FLAGS;
    }
}
