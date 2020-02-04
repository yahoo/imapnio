package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.mail.Flags;

import com.yahoo.imapnio.async.data.MessageNumberSet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap store command request from client, with formal syntax below.
 *
 * <pre>
 *
 * store                = "STORE" SP sequence-set SP store-att-flags
 *
 * store-att-flags      = (["+" / "-"] "FLAGS" [".SILENT"]) SP
 *                        (flag-list / (flag *(SP flag)))
 * flag-list            = "(" [flag *(SP flag)] ")"
 *
 * flag                 = "\Answered" / "\Flagged" / "\Deleted" /
 *                        "\Seen" / "\Draft" / flag-keyword / flag-extension
 *                        ; Does not include "\Recent"
 *
 * flag-extension       = "\" atom
 *                        ; Future expansion.  Client implementations
 *                        ; MUST accept flag-extension flags.  Server
 *                        ; implementations MUST NOT generate
 *                        ; flag-extension flags except as defined by
 *                        ; future standard or standards-track
 *                        ; revisions of this specification.
 *
 * store-modifier      =/ "UNCHANGEDSINCE" SP mod-sequence-valzer
 *                       ;; Only a single "UNCHANGEDSINCE" may be
 *                       ;; specified in a STORE operation.
 *
 * mod-sequence-value  = 1*DIGIT
 *                       ;; Positive unsigned 63-bit integer
 *                       ;; (mod-sequence)
 *                       ;; (1 \leq n \leq 9,223,372,036,854,775,807).
 *
 * mod-sequence-valzer = "0" / mod-sequence-value
 * </pre>
 */
public abstract class AbstractStoreFlagsCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Literal for STORE. */
    private static final String STORE_SP = "STORE ";

    /** Byte array for STORE. */
    private static final byte[] STORE_SP_B = STORE_SP.getBytes(StandardCharsets.US_ASCII);

    /** Literal for UID STORE. */
    private static final String UID_STORE_SP = "UID STORE ";

    /** Byte array for UID STORE. */
    private static final byte[] UID_STORE_SP_B = UID_STORE_SP.getBytes(StandardCharsets.US_ASCII);

    /** Literal for FLAGS. */
    private static final String FLAGS = "FLAGS";

    /** Literal for .SILENT to append after FLAGS. */
    private static final String SILENT = ".SILENT";

    /** Literal for UNCHANGEDSINCE. */
    private static final String UNCHANGEDSINCE = "UNCHANGEDSINCE";

    /** Unchanged since the modification seqeuence. */
    private Long unchangedSince;

    /** Flag whether adding UID before store command. */
    private boolean isUid;

    /** A collection of messages numbers specified based on RFC3501 sequence-set syntax. */
    private String msgNumbers;

    /** Messages flags. */
    private Flags flags;

    /** Action to indicate whether to add, replace or remove existing flag. */
    private FlagsAction action;

    /** Flag to indicate whether silent or not. */
    private final boolean isSilent;

    /**
     * Initializes a {@link AbstractStoreFlagsCommand} with the MessageNumberSet array, flags, action and silent flag whether server should return new
     * values.
     *
     * @param isUid whether to have UID prepended
     * @param msgsets the set of message set
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     * @param silent true if asking server to respond silently
     */
    protected AbstractStoreFlagsCommand(final boolean isUid, @Nonnull final MessageNumberSet[] msgsets, @Nonnull final Flags flags,
            @Nonnull final FlagsAction action, final boolean silent) {
        this(isUid, MessageNumberSet.buildString(msgsets), flags, action, silent);
    }

    /**
     * Initializes a @{code AbstractStoreFlagsCommand} with the MessageNumberSet array, flags, action, silent flag whether server should return new
     * values, and unchanged since the given modification sequence.
     *
     * @param isUid whether to have UID prepended
     * @param msgsets the set of message set
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     * @param silent true if asking server to respond silently
     * @param unchangedSince unchanged since the given modification sequence
     */
    protected AbstractStoreFlagsCommand(final boolean isUid, @Nonnull final MessageNumberSet[] msgsets, @Nonnull final Flags flags,
            @Nonnull final FlagsAction action, final boolean silent, @Nonnull final Long unchangedSince) {
        this(isUid, MessageNumberSet.buildString(msgsets), flags, action, silent, unchangedSince);
    }

    /**
     * Initializes a @{code AbstractStoreFlagsCommand} with string form message numbers (could be sequence sets or UIDs), flags, action,
     * and silent flag whether server should return new value.
     *
     * @param isUid whether to have UID prepended
     * @param msgNumbers the message id
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     * @param silent true if asking server to respond silently
     */
    protected AbstractStoreFlagsCommand(final boolean isUid, @Nonnull final String msgNumbers, @Nonnull final Flags flags,
            @Nonnull final FlagsAction action, final boolean silent) {
        this.isUid = isUid;
        this.msgNumbers = msgNumbers;
        this.flags = flags;
        this.action = action;
        this.isSilent = silent;
        this.unchangedSince = null;
    }

    /**
     * Initializes a @{code AbstractStoreFlagsCommand} with string form message numbers (could be sequence sets or UIDs), flags, action,
     * silent flag whether server should return new values, and unchanged since the given modification sequence.
     *
     * @param isUid whether to have UID prepended
     * @param msgNumbers the message id
     * @param flags the flags to be stored
     * @param action whether to replace, add or remove the flags
     * @param silent true if asking server to respond silently
     * @param unchangedSince unchanged since the given modification sequence
     */
    protected AbstractStoreFlagsCommand(final boolean isUid, @Nonnull final String msgNumbers, @Nonnull final Flags flags,
            @Nonnull final FlagsAction action, final boolean silent, @Nonnull final Long unchangedSince) {
        this.isUid = isUid;
        this.msgNumbers = msgNumbers;
        this.flags = flags;
        this.action = action;
        this.isSilent = silent;
        this.unchangedSince = unchangedSince;
    }

    @Override
    public void cleanup() {
        this.msgNumbers = null;
        this.flags = null;
        this.action = null;
        this.unchangedSince = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() {
        // Ex:STORE 2:4 +FLAGS (\Deleted)
        final StringBuilder sb = new StringBuilder();
        sb.append(msgNumbers);
        sb.append(ImapClientConstants.SPACE);
        if (unchangedSince != null) {
            sb.append(ImapClientConstants.L_PAREN);
            sb.append(UNCHANGEDSINCE);
            sb.append(ImapClientConstants.SPACE);
            sb.append(unchangedSince);
            sb.append(ImapClientConstants.R_PAREN);
            sb.append(ImapClientConstants.SPACE);
        }

        if (action == FlagsAction.ADD) {
            sb.append(ImapClientConstants.PLUS);
        } else if (action == FlagsAction.REMOVE) {
            sb.append(ImapClientConstants.MINUS);
        }

        sb.append(FLAGS);

        if (isSilent) {
            sb.append(SILENT);
        }

        sb.append(ImapClientConstants.SPACE);

        // buildFlagString generates "(" [flag *(SP flag)] ")"
        final ImapArgumentFormatter argWriter = new ImapArgumentFormatter();
        sb.append(argWriter.buildFlagListString(flags));

        final String storeCmdStr = sb.toString();
        final int len = ImapClientConstants.PAD_LEN  + storeCmdStr.length();
        final ByteBuf byteBuf = Unpooled.buffer(len);
        byteBuf.writeBytes(isUid ? UID_STORE_SP_B : STORE_SP_B);
        byteBuf.writeBytes(storeCmdStr.getBytes(StandardCharsets.US_ASCII));

        byteBuf.writeBytes(CRLF_B);

        return byteBuf;
    }
}
