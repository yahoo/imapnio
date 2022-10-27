package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.data.PartialExtensionUidFetchInfo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines IMAP fetch command request from client. ABNF in RFC3501 is described as following:
 *
 * <pre>
 * {@code
 * fetch           = "FETCH" SP sequence-set SP ("ALL" / "FULL" / "FAST" /
 *                   fetch-att / "(" fetch-att *(SP fetch-att) ")")
 *
 * fetch-att       = "ENVELOPE" / "FLAGS" / "INTERNALDATE" /
 *                   "RFC822" [".HEADER" / ".SIZE" / ".TEXT"] /
 *                   "BODY" ["STRUCTURE"] / "UID" /
 *                   "BODY" section ["<" number "." nz-number ">"] /
 *                   "BODY.PEEK" section ["<" number "." nz-number ">"]
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

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Partial extension. */
    private static final String PARTIAL_EXTENSION_SP = " (PARTIAL ";

    /** Byte array for partial extension. */
    private static final byte [] PARTIAL_EXTENSION_SP_B = PARTIAL_EXTENSION_SP.getBytes(StandardCharsets.US_ASCII);

    /** Message numbers, either message sequence or UID. */
    private String msgNumbers;

    /** Fetch items. */
    private String dataItems;

    /** Fetch macro. */
    private FetchMacro macro;

    /** True if prepending UID; false otherwise. */
    private final boolean isUid;

    /** Partial uid fetch info. */
    private PartialExtensionUidFetchInfo partialExtUidFetchInfo;

    /**
     * Initializes a {@link FetchCommand} with the {@link MessageNumberSet} array.
     *
     * @param isUid whether prepending UID
     * @param msgsets the set of message set
     * @param items the data items
     * @param partialExtUidFetchInfo partial uid fetch info
     */
    protected AbstractFetchCommand(final boolean isUid, @Nonnull final MessageNumberSet[] msgsets, @Nonnull final String items,
                                @Nullable final PartialExtensionUidFetchInfo partialExtUidFetchInfo) {
        this(isUid, MessageNumberSet.buildString(msgsets), items, partialExtUidFetchInfo);
    }

    /**
     * Initializes a {@link FetchCommand} with the {@link MessageNumberSet} array.
     *
     * @param isUid whether prepending UID
     * @param msgsets the set of message set
     * @param items the data items
     */
    protected AbstractFetchCommand(final boolean isUid, @Nonnull final MessageNumberSet[] msgsets, @Nonnull final String items) {
        this(isUid, MessageNumberSet.buildString(msgsets), items, null);
    }

    /**
     * Initializes a {@link FetchCommand} with the {@link MessageNumberSet} array.
     *
     * @param isUid whether prepending UID
     * @param msgsets the set of message set
     * @param macro the macro
     * @param partialExtUidFetchInfo partial uid fetch info
     */
    protected AbstractFetchCommand(final boolean isUid, @Nonnull final MessageNumberSet[] msgsets, @Nonnull final FetchMacro macro,
                                @Nullable final PartialExtensionUidFetchInfo partialExtUidFetchInfo) {
        this(isUid, MessageNumberSet.buildString(msgsets), macro, partialExtUidFetchInfo);
    }

    /**
     * Initializes a {@link FetchCommand} with the {@link MessageNumberSet} array.
     *
     * @param isUid whether prepending UID
     * @param msgsets the set of message set
     * @param macro the macro
     */
    protected AbstractFetchCommand(final boolean isUid, @Nonnull final MessageNumberSet[] msgsets, @Nonnull final FetchMacro macro) {
        this(isUid, MessageNumberSet.buildString(msgsets), macro, null);
    }

    /**
     * Initializes a {@link FetchCommand} with the {@link MessageNumberSet} array.
     *
     * @param isUid whether prepending UID
     * @param msgNumbers the message numbers string
     * @param items the data items
     * @param partialExtUidFetchInfo partial uid fetch info
     */
    protected AbstractFetchCommand(final boolean isUid, @Nonnull final String msgNumbers, @Nonnull final String items,
                                   @Nullable final PartialExtensionUidFetchInfo partialExtUidFetchInfo) {
        this.isUid = isUid;
        this.msgNumbers = msgNumbers;
        this.dataItems = items;
        this.macro = null;
        this.partialExtUidFetchInfo = partialExtUidFetchInfo;
    }

    /**
     * Initializes a {@link FetchCommand} with the {@link MessageNumberSet} array.
     *
     * @param isUid whether prepending UID
     * @param msgNumbers the message numbers string
     * @param macro the macro
     * @param partialExtUidFetchInfo partial uid fetch info
     */
    protected AbstractFetchCommand(final boolean isUid, @Nonnull final String msgNumbers, @Nonnull final FetchMacro macro,
                                   @Nullable final PartialExtensionUidFetchInfo partialExtUidFetchInfo) {
        this.isUid = isUid;
        this.msgNumbers = msgNumbers;
        this.macro = macro;
        this.dataItems = null;
        this.partialExtUidFetchInfo = partialExtUidFetchInfo;
    }

    @Override
    public void cleanup() {
        this.msgNumbers = null;
        this.dataItems = null;
        this.macro = null;
        this.partialExtUidFetchInfo = null;
    }

    @Override
    @Nonnull
    public ByteBuf getCommandLineBytes() {
        final ByteBuf bb = Unpooled.buffer();
        bb.writeBytes(isUid ? UID_FETCH_SP_B : FETCH_SP_B);
        bb.writeBytes(msgNumbers.getBytes(StandardCharsets.US_ASCII));
        bb.writeByte(ImapClientConstants.SPACE);

        if (dataItems != null) {
            bb.writeByte(ImapClientConstants.L_PAREN);
            bb.writeBytes(dataItems.getBytes(StandardCharsets.US_ASCII));
            bb.writeByte(ImapClientConstants.R_PAREN);
        } else {
            bb.writeBytes(macro.name().getBytes(StandardCharsets.US_ASCII));
        }

        if (isUid && partialExtUidFetchInfo != null) {
            bb.writeBytes(PARTIAL_EXTENSION_SP_B);
            bb.writeBytes(String.valueOf(partialExtUidFetchInfo.getFirstUid()).getBytes(StandardCharsets.US_ASCII));
            bb.writeBytes(ImapClientConstants.COLON.getBytes(StandardCharsets.US_ASCII));
            bb.writeBytes(String.valueOf(partialExtUidFetchInfo.getLastUid()).getBytes(StandardCharsets.US_ASCII));
            bb.writeByte(ImapClientConstants.R_PAREN);
        }
        bb.writeBytes(CRLF_B);

        return bb;
    }
}
