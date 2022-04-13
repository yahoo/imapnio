package com.yahoo.imapnio.async.data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.search.SearchTerm;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.request.EntryTypeRequest;

/**
 * This class models search-modsequence with {@code EntryTypeReq} from RFC7162. ABNF from RFC7162 is documented below.
 *
 * <pre>
 * {@code
 * 3.1.5.  MODSEQ Search Criterion in SEARCH
 *
 *    The MODSEQ criterion for the SEARCH (or UID SEARCH) command allows a
 *    client to search for the metadata items that were modified since a
 *    specified moment.
 *    Syntax: MODSEQ [<entry-name> <entry-type-req>] <mod-sequence-valzer>
 * ============================================================================
 * ABNF:
 * search-modsequence  = "MODSEQ" [search-modseq-ext] SP mod-sequence-valzer
 *
 * entry-name          = entry-flag-name
 *
 * entry-flag-name     = DQUOTE "/flags/" attr-flag DQUOTE
 *                       ;; Each system or user-defined flag <flag>
 *                       ;; is mapped to "/flags/<flag>".
 *                       ;;
 *                       ;; <entry-flag-name> follows the escape rules
 *                       ;; used by "quoted" string as described in
 *                       ;; Section 4.3 of [RFC3501]; e.g., for the
 *                       ;; flag \Seen, the corresponding <entry-name>
 *                       ;; is "/flags/\\seen", and for the flag
 *                       ;; $MDNSent, the corresponding <entry-name>
 *                       ;; is "/flags/$mdnsent".
 *
 * entry-type-resp     = "priv" / "shared"
 *                       ;; Metadata item type.
 *
 * entry-type-req      = entry-type-resp / "all"
 *                       ;; Perform SEARCH operation on a private
 *                       ;; metadata item, shared metadata item,
 *                       ;; or both.
 *
 * }
 * </pre>
 */
public final class ExtendedModifiedSinceTerm extends SearchTerm {

    /** The name of the metadata item. */
    private final Flags entryName;

    /** The type of metadata item. */
    private final EntryTypeRequest entryType;

    /** The given modification sequence. */
    private final long modSeq;

    /**
     * Initializes the {@code ExtendedModifiedSinceTerm} class with modification sequence.
     *
     * @param modSeq modification sequence number
     */
    public ExtendedModifiedSinceTerm(final long modSeq) {
        this.entryName = null;
        this.entryType = null;
        this.modSeq = modSeq;
    }

    /**
     * Initializes the {@code ExtendedModifiedSinceTerm} class with entry name, entry type, and modification sequence.
     *
     * @param entryName name of the metadata item
     * @param entryType type of the metadata item
     * @param modSeq modification sequence number
     * @throws ImapAsyncClientException if entryName has more than one flag.
     */
    public ExtendedModifiedSinceTerm(@Nonnull final Flags entryName, @Nonnull final EntryTypeRequest entryType, final long modSeq)
            throws ImapAsyncClientException {
        final Flags.Flag[] sf = entryName.getSystemFlags(); // get the system flags
        final String[] uf = entryName.getUserFlags(); // get the user flag strings
        if (sf.length + uf.length != 1) {
            throw new ImapAsyncClientException(ImapAsyncClientException.FailureType.INVALID_INPUT);
        }
        this.entryName = entryName;
        this.entryType = entryType;
        this.modSeq = modSeq;
    }

    /**
     * @return the entry name
     */
    @Nullable
    public Flags getEntryName() {
        return entryName;
    }

    /**
     * @return the entry type
     */
    @Nullable
    public EntryTypeRequest getEntryType() {
        return entryType;
    }

    /**
     * @return the modification sequence
     */
    public long getModSeq() {
        return modSeq;
    }

    /**
     * This method should never be called for ExtendedModifiedSinceTerm.
     *
     * @param msg the date comparator is applied to this Message's MODSEQ
     * @return UnsupportedOperationException if the method is called
     */
    @Override
    public boolean match(final Message msg) {
        throw new UnsupportedOperationException("This method should not be used. Only for override purpose.");
    }
}
