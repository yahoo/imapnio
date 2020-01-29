package com.yahoo.imapnio.async.data;

import javax.annotation.Nullable;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.search.SearchTerm;

import com.sun.mail.imap.IMAPMessage;
import com.yahoo.imapnio.async.request.EntryTypeReq;

/**
 * Find messages that have been modified since a given MODSEQ value
 * with optional entry name and entry type.
 * Relies on the server implementing the CONDSTORE extension
 * (<A HREF="http://www.ietf.org/rfc/rfc7162.txt">RFC 7162</A>).
 */
public final class ExtendedModifiedSinceTerm extends SearchTerm {

    /** The name of the metadata item. */
    private final String entryName;

    /** The type of metadata item. */
    private final EntryTypeReq entryType;

    /** The given modification sequence. */
    private final long modSeq;

    /**
     * Constructor.
     *
     * @param modSeq modification sequence number
     */
    public ExtendedModifiedSinceTerm(final long modSeq) {
        this.entryName = null;
        this.entryType = null;
        this.modSeq = modSeq;
    }

    /**
     * Constructor.
     *
     * @param entryName name of the metadata item
     * @param entryType type of the metadata item
     * @param modSeq modification sequence number
     */
    public ExtendedModifiedSinceTerm(@Nullable final String entryName, @Nullable final EntryTypeReq entryType, final long modSeq) {
        this.entryName = entryName;
        this.entryType = entryType;
        this.modSeq = modSeq;
    }

    /**
     * @return the entry name
     */
    @Nullable
    public String getEntryName() {
        return entryName;
    }

    /**
     * @return the entry type
     */
    @Nullable
    public EntryTypeReq getEntryType() {
        return entryType;
    }

    /**
     * @return the modification sequence
     */
    public long getModSeq() {
        return modSeq;
    }

    /**
     * The match method.
     * TODO: Require to extend IMAP Message to support entry name and type for matching.
     *
     * @param msg the date comparator is applied to this Message's MODSEQ
     * @return true if the comparison succeeds, otherwise false
     */
    @Override
    public boolean match(final Message msg) {
        long m;

        try {
            if (msg instanceof IMAPMessage) {
                m = ((IMAPMessage) msg).getModSeq();
                return m >= modSeq;
            } else {
                return false;
            }
        } catch (MessagingException e) {
            return false;
        }
    }
}
