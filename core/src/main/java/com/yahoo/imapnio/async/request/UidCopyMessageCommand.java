package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.UIDSet;
import com.yahoo.imapnio.async.data.MessageNumberSet;

/**
 * This class defines IMAP uid copy command from client.
 */
public class UidCopyMessageCommand extends AbstractMessageActionCommand {

    /** Command name. */
    private static final String COPY = "COPY";

    /**
     * Initializes a {@link UidCopyMessageCommand} with the message sequence syntax using UIDSet object.
     *
     * @param uidsets list of {@link UIDSet}
     * @param targetFolder the targetFolder to be stored
     */
    public UidCopyMessageCommand(@Nonnull final UIDSet[] uidsets, @Nonnull final String targetFolder) {
        super(COPY, true, UIDSet.toString(uidsets), targetFolder);
    }

    /**
     * Initializes a {@link UidCopyMessageCommand} with the message sequence syntax.
     *
     * @param uids the string representing UID based on RFC3501
     * @param targetFolder the targetFolder to be stored
     */
    public UidCopyMessageCommand(@Nonnull final String uids, @Nonnull final String targetFolder) {
        super(COPY, true, uids, targetFolder);
    }

    /**
     * Initializes a {@link UidCopyMessageCommand} with the {@link MessageNumberSet} array.
     *
     * @param msgsets the set of {@link MessageNumberSet}
     * @param targetFolder the targetFolder to be stored
     */
    public UidCopyMessageCommand(@Nonnull final MessageNumberSet[] msgsets, @Nonnull final String targetFolder) {
        super(COPY, true, msgsets, targetFolder);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.UID_COPY_MESSAGE;
    }
}
