package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.yahoo.imapnio.async.data.MessageNumberSet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines IMAP fetch command request from client. ABNF in RFC3501 and RFC7162 is described as following:
 *
 * <pre>
 * {@code
 * fetch               = "FETCH" SP sequence-set SP ("ALL" / "FULL" / "FAST" /
 *                       fetch-att / "(" fetch-att *(SP fetch-att) ")")
 *
 * fetch-att           = "ENVELOPE" / "FLAGS" / "INTERNALDATE" /
 *                       "RFC822" [".HEADER" / ".SIZE" / ".TEXT"] /
 *                       "BODY" ["STRUCTURE"] / "UID" /
 *                       "BODY" section ["<" number "." nz-number ">"] /
 *                       "BODY.PEEK" section ["<" number "." nz-number ">"]
 *
 * fetch-modifier      =/ chgsince-fetch-mod
 *                       ;; Conforms to the generic "fetch-modifier"
 *                       ;; syntax defined in [RFC4466].
 *
 * chgsince-fetch-mod  = "CHANGEDSINCE" SP mod-sequence-value
 *                       ;; CHANGEDSINCE FETCH modifier conforms to
 *                       ;; the fetch-modifier syntax.
 *
 * fetch-att           =/ fetch-mod-sequence
 *                       ;; Modifies original IMAP4 fetch-att.
 *
 * fetch-mod-sequence  = "MODSEQ"
 *
 * fetch-mod-resp      = "MODSEQ" SP "(" permsg-modsequence ")"
 *
 * permsg-modsequence  = mod-sequence-value
 *                       ;; Per-message mod-sequence.
 *
 * mod-sequence-value  = 1*DIGIT
 *                       ;; Positive unsigned 63-bit integer
 *                       ;; (mod-sequence)
 *                       ;; (1 <= n <= 9,223,372,036,854,775,807).
 *
 * rexpunges-fetch-mod  =  "VANISHED"
 *                       ;; VANISHED UID FETCH modifier conforms
 *                       ;; to the fetch-modifier syntax
 *                       ;; defined in [RFC4466].  It is only
 *                       ;; allowed in the UID FETCH command.
 * }
 * </pre>
 */
public abstract class AbstractFetchCommand extends ImapRequestAdapter {

    /** FETCH and space. */
    private static final String FETCH_SP = "FETCH ";

    /** Byte array for FETCH. */
    private static final byte[] FETCH_SP_B = FETCH_SP.getBytes(StandardCharsets.US_ASCII);

    /** UID FETCH and space. */
    private static final String UID_FETCH_SP = "UID FETCH ";

    /** Byte array for UID FETCH. */
    private static final byte[] UID_FETCH_SP_B = UID_FETCH_SP.getBytes(StandardCharsets.US_ASCII);

    /** Changed Since and space. */
    private static final String CHANGEDSINCE_SP = "CHANGEDSINCE ";

    /** Space and vanished. */
    private static final String SP_VANISHED = " VANISHED";

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Message numbers, either message sequence or UID. */
    private String msgNumbers;

    /** Fetch items. */
    private String dataItems;

    /** Fetch macro. */
    private FetchMacro macro;

    /** True if prepending UID; false otherwise. */
    private boolean isUid;

    /** Changed since the given modification sequence. */
    private Long changedSince;

    /** Indicate whether vanished flag is used. */
    private boolean vanished;

    /**
     * Initializes a @{code FetchCommand} with the flag indicate whether prepending UID, the @{code MessageNumberSet} array and the data items.
     *
     * @param isUid whether prepending UID
     * @param msgsets the set of message set
     * @param items the data items
     */
    public AbstractFetchCommand(final boolean isUid, @Nonnull final MessageNumberSet[] msgsets, @Nonnull final String items) {
        this(isUid, MessageNumberSet.buildString(msgsets), items, null, false);
    }

    /**
     * Initializes a @{code FetchCommand} with the flag indicate whether prepending UID, the @{code MessageNumberSet} array, and the macro.
     *
     * @param isUid whether prepending UID
     * @param msgsets the set of message set
     * @param macro the macro
     */
    public AbstractFetchCommand(final boolean isUid, @Nonnull final MessageNumberSet[] msgsets, @Nonnull final FetchMacro macro) {
        this(isUid, MessageNumberSet.buildString(msgsets), macro);
    }

    /**
     * Initializes a @{code FetchCommand} with the flag indicate whether prepending UID, the the message numbers string, and the macro.
     *
     * @param isUid whether prepending UID
     * @param msgNumbers the message numbers string
     * @param macro the macro
     */
    public AbstractFetchCommand(final boolean isUid, @Nonnull final String msgNumbers, @Nonnull final FetchMacro macro) {
        this(isUid, msgNumbers, macro, null);
    }

    /**
     * Initializes a @{code FetchCommand} with the flag indicate whether prepending UID, the the message numbers string, and the data items.
     *
     * @param isUid whether prepending UID
     * @param msgNumbers the message numbers string
     * @param items the data items
     */
    public AbstractFetchCommand(final boolean isUid, @Nonnull final String msgNumbers, @Nonnull final String items) {
        this(isUid, msgNumbers, items, null);
    }

    /**
     * Initializes a @{code FetchCommand} with the flag indicate whether prepending UID, the @{code MessageNumberSet} array,
     * the data items, and changed since the given modification sequence.
     *
     * @param isUid whether prepending UID
     * @param msgsets the set of message set
     * @param items the data items
     * @param changedSince changed since the given modification sequence
     */
    public AbstractFetchCommand(final boolean isUid, @Nonnull final MessageNumberSet[] msgsets, @Nonnull final String items,
                                @Nullable final Long changedSince) {
        this(isUid, MessageNumberSet.buildString(msgsets), items, changedSince);
    }

    /**
     * Initializes a @{code FetchCommand} with the flag indicate whether prepending UID, the @{code MessageNumberSet} array, the macro,
     * and changed since the given modification sequence.
     *
     * @param isUid whether prepending UID
     * @param msgsets the set of message set
     * @param macro the macro
     * @param changedSince changed since the given modification sequence
     */
    public AbstractFetchCommand(final boolean isUid, @Nonnull final MessageNumberSet[] msgsets, @Nonnull final FetchMacro macro,
                                @Nullable final Long changedSince) {
        this(isUid, MessageNumberSet.buildString(msgsets), macro, changedSince);
    }

    /**
     * Initializes a @{code FetchCommand} with the flag indicate whether prepending UID, the message numbers string, the data items,
     * and changed since the given modification sequence.
     *
     * @param isUid whether prepending UID
     * @param msgNumbers the message numbers string
     * @param items the data items
     * @param changedSince changed since the given modification sequence
     */
    protected AbstractFetchCommand(final boolean isUid, @Nonnull final String msgNumbers, @Nonnull final String items,
                                   @Nullable final Long changedSince) {
        this(isUid, msgNumbers, items, changedSince, false);
    }

    /**
     * Initializes a @{code FetchCommand} with the flag indicate whether prepending UID, the message numbers string, the macro,
     * and changed since the given modification sequence.
     *
     * @param isUid whether prepending UID
     * @param msgNumbers the message numbers string
     * @param macro the macro
     * @param changedSince changed since the given modification sequence
     */
    protected AbstractFetchCommand(final boolean isUid, @Nonnull final String msgNumbers, @Nonnull final FetchMacro macro,
                                   @Nullable final Long changedSince) {
        this(isUid, msgNumbers, macro, changedSince, false);
    }

    /**
     * Initializes a @{code FetchCommand} with the flag indicate whether prepending UID, the @{code MessageNumberSet} array, the data items,
     * changed since the given modification sequence, and the flag to check whether uid fetch with vanished option.
     *
     * @param isUid whether prepending UID
     * @param msgsets the set of message set
     * @param items the data items
     * @param changedSince changed since the given modification sequence
     * @param vanished whether uid fetch with vanished option
     */
    public AbstractFetchCommand(final boolean isUid, @Nonnull final MessageNumberSet[] msgsets, @Nonnull final String items,
                                @Nullable final Long changedSince, final boolean vanished) {
        this(isUid, MessageNumberSet.buildString(msgsets), items, changedSince, vanished);
    }

    /**
     * Initializes a @{code FetchCommand} with the flag indicate whether prepending UID, the @{code MessageNumberSet} array, the macro,
     * changed since the given modification sequence, and the flag to check whether uid fetch with vanished option.
     *
     * @param isUid whether prepending UID
     * @param msgsets the set of message set
     * @param macro the macro
     * @param changedSince changed since the given modification sequence
     * @param vanished whether uid fetch with vanished option
     */
    public AbstractFetchCommand(final boolean isUid, @Nonnull final MessageNumberSet[] msgsets, @Nonnull final FetchMacro macro,
                                @Nullable final Long changedSince, final boolean vanished) {
        this(isUid, MessageNumberSet.buildString(msgsets), macro, changedSince, vanished);
    }

    /**
     * Initializes a @{code FetchCommand} with the flag indicate whether prepending UID, the message numbers string, the data items,
     * changed since the given modification sequence, and the flag to check whether uid fetch with vanished option.
     *
     * @param isUid whether prepending UID
     * @param msgNumbers the message numbers string
     * @param items the data items
     * @param changedSince changed since the given modification sequence
     * @param vanished whether uid fetch with vanished option
     */
    protected AbstractFetchCommand(final boolean isUid, @Nonnull final String msgNumbers, @Nonnull final String items,
                                   @Nullable final Long changedSince, final boolean vanished) {
        this.isUid = isUid;
        this.msgNumbers = msgNumbers;
        this.dataItems = items;
        this.macro = null;
        this.changedSince = changedSince;
        this.vanished = vanished;
    }

    /**
     * Initializes a @{code FetchCommand} with the flag indicate whether prepending UID, the message numbers string, the macro,
     * changed since the given modification sequence, and the flag to check whether uid fetch with vanished option.
     *
     * @param isUid whether prepending UID
     * @param msgNumbers the message numbers string
     * @param macro the macro
     * @param changedSince changed since the given modification sequence
     * @param vanished whether uid fetch with vanished option
     */
    protected AbstractFetchCommand(final boolean isUid, @Nonnull final String msgNumbers, @Nonnull final FetchMacro macro,
                                   @Nullable final Long changedSince, final boolean vanished) {
        this.isUid = isUid;
        this.msgNumbers = msgNumbers;
        this.macro = macro;
        this.dataItems = null;
        this.changedSince = changedSince;
        this.vanished = vanished;
    }


    @Override
    public void cleanup() {
        this.msgNumbers = null;
        this.dataItems = null;
        this.macro = null;
        this.changedSince = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() {
        // Ex:UID FETCH 300:500 (FLAGS) (CHANGEDSINCE 1234 VANISHED)
        final StringBuilder sb = new StringBuilder();
        sb.append(msgNumbers);
        sb.append(ImapClientConstants.SPACE);
        if (dataItems != null) {
            sb.append(ImapClientConstants.L_PAREN);
            sb.append(dataItems);
            sb.append(ImapClientConstants.R_PAREN);
        } else {
            sb.append(macro.name());
        }

        if (changedSince != null) {
            sb.append(ImapClientConstants.SPACE);
            sb.append(ImapClientConstants.L_PAREN);
            sb.append(CHANGEDSINCE_SP);
            sb.append(changedSince);
            if (vanished) {
                sb.append(SP_VANISHED);
            }
            sb.append(ImapClientConstants.R_PAREN);
        }

        final String fetchCmdStr = sb.toString();
        final int len = ImapClientConstants.PAD_LEN  + fetchCmdStr.length();
        final ByteBuf byteBuf = Unpooled.buffer(len);
        byteBuf.writeBytes(isUid ? UID_FETCH_SP_B : FETCH_SP_B);
        byteBuf.writeBytes(fetchCmdStr.getBytes(StandardCharsets.US_ASCII));

        byteBuf.writeBytes(CRLF_B);

        return byteBuf;
    }
}
