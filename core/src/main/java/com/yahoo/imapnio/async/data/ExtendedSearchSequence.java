package com.yahoo.imapnio.async.data;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.search.SearchException;
import javax.mail.search.SearchTerm;

import com.sun.mail.iap.Argument;
import com.sun.mail.imap.protocol.SearchSequence;

/**
 * This class extends the search sequence for modification sequence with the optional fields entry
 * name and type defined in https://tools.ietf.org/html/rfc7162#section-3.1.5.
 */
public class ExtendedSearchSequence extends SearchSequence {

    /**
     * Generate the IMAP search sequence for the given search expression.
     *
     * @param term the search term
     * @return the IMAP search sequence argument
     * @throws SearchException will not throw
     * @throws IOException will not throw
     */
    public Argument generateSequence(@Nonnull final SearchTerm term) throws IOException, SearchException {
        return generateSequence(term, null);
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
    public Argument generateSequence(@Nonnull final SearchTerm term, @Nullable final String charset) throws SearchException, IOException {
        if (term instanceof ExtendedModifiedSinceTerm) {
            return modifiedSince((ExtendedModifiedSinceTerm) term);
        } else {
            return super.generateSequence(term, charset);
        }
    }

    /**
     * Modification of SearchSequence modifiedSince method to support optional entry name and entry type.
     *
     * @param term the extended modified since search term
     * @return the IMAP search sequence argument
     */
    protected Argument modifiedSince(final ExtendedModifiedSinceTerm term) {
        final Argument result = new Argument();
        result.writeAtom("MODSEQ");

        if (term.getEntryName() != null && term.getEntryType() != null) {
            result.writeAtom("\"" + term.getEntryName() + "\"");
            result.writeAtom(term.getEntryType().name());
        }

        result.writeNumber(term.getModSeq());
        return result;
    }
}
