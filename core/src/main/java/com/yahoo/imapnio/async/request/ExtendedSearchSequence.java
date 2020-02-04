package com.yahoo.imapnio.async.request;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.mail.search.SearchException;
import javax.mail.search.SearchTerm;

import com.sun.mail.iap.Argument;
import com.sun.mail.imap.protocol.SearchSequence;
import com.yahoo.imapnio.async.data.ExtendedModifiedSinceTerm;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * This class extends the search sequence for modification sequence with the optional fields entry
 * name and type defined in https://tools.ietf.org/html/rfc7162#section-3.1.5.
 */
public class ExtendedSearchSequence extends SearchSequence {

    /** Literal for MODSEQ. */
    private static final String MODSEQ = "MODSEQ";

    /**
     * Generate the IMAP search sequence for the given search expression.
     *
     * @param term the search term
     * @return the IMAP search sequence argument
     * @throws SearchException will not throw
     * @throws IOException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    public Argument generateSequence(@Nonnull final SearchTerm term) throws IOException, SearchException, ImapAsyncClientException {
        if (term instanceof ExtendedModifiedSinceTerm) {
            return modifiedSince((ExtendedModifiedSinceTerm) term);
        } else {
            return super.generateSequence(term, null);
        }
    }

    /**
     * Generate the IMAP search sequence for the given search expression.
     *
     * @param term the search term
     * @param charset the character set
     * @return the IMAP search sequence argument
     * @throws SearchException will not throw
     * @throws IOException will not throw
     */
    @Override
    public Argument generateSequence(@Nonnull final SearchTerm term, @Nonnull final String charset) throws SearchException, IOException {
        if (term instanceof ExtendedModifiedSinceTerm) {
            try {
                return modifiedSince((ExtendedModifiedSinceTerm) term);
            } catch (ImapAsyncClientException e) {
                throw new SearchException(e.getMessage());
            }
        } else {
            return super.generateSequence(term, charset);
        }
    }

    /**
     * Modification of SearchSequence modifiedSince method to support optional entry name and entry type.
     *
     * @param term the extended modified since search term
     * @return the IMAP search sequence argument
     * @throws ImapAsyncClientException will not throw
     */
    protected Argument modifiedSince(@Nonnull final ExtendedModifiedSinceTerm term) throws ImapAsyncClientException {
        final Argument result = new Argument();
        result.writeAtom(MODSEQ);
        if (term.getEntryName() != null && term.getEntryType() != null) {
            final ImapArgumentFormatter argWriter = new ImapArgumentFormatter();
            result.writeAtom(argWriter.buildEntryFlagName(term.getEntryName()));
            result.writeAtom(term.getEntryType().name());
        }

        result.writeNumber(term.getModSeq());
        return result;
    }
}
