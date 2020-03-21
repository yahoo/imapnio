package com.yahoo.imapnio.async.request;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.search.SearchException;
import javax.mail.search.SearchTerm;

import com.sun.mail.iap.Argument;
import com.yahoo.imapnio.async.data.Capability;
import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * This class defines IMAP UID search command request from client.
 */
public class UidSearchCommand extends AbstractSearchCommand {

    /**
     * Initializes this object with the MessageNumberSet array, search string and character set name.
     *
     * @param msgsets the set of MessageNumberSet
     * @param term the search term
     * @param capa the capability instance to check if it has literal
     * @throws ImapAsyncClientException when both msgsets and searchString are null
     * @throws IOException when parsing error for generate sequence
     * @throws SearchException when search term cannot be found
     */
    public UidSearchCommand(@Nullable final MessageNumberSet[] msgsets, @Nullable final SearchTerm term, @Nullable final Capability capa)
            throws ImapAsyncClientException, SearchException, IOException {
        super(true, msgsets, term, capa);
    }

    /**
     * Initializes this object with the string form of message sequence, SearchTerm.
     *
     * @param msgNumbers the string form message numbers in sequence-set syntax
     * @param term the search term
     * @param capa the capability instance to find if it has literal
     * @throws ImapAsyncClientException when both msgNumber and searchString are null
     * @throws IOException when parsing error for generate sequence
     * @throws SearchException when search term cannot be found
     */
    public UidSearchCommand(@Nullable final String msgNumbers, @Nullable final SearchTerm term, @Nullable final Capability capa)
            throws ImapAsyncClientException, SearchException, IOException {
        super(true, msgNumbers, term, capa);
    }

    /**
     * Initializes this object with the string form of message sequence, character set name, and Argument that expresses the search term.
     *
     * @param msgNumbers the string form message numbers in sequence-set syntax
     * @param charset the character set
     * @param args the search term in argument format
     * @param capa the capability instance to find if it has literal
     * @throws ImapAsyncClientException when both msgNumber and searchString are null
     */
    public UidSearchCommand(@Nullable final String msgNumbers, @Nullable final String charset, @Nonnull final Argument args,
            @Nullable final Capability capa) throws ImapAsyncClientException {
        super(true, msgNumbers, charset, args, capa);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.UID_SEARCH;
    }
}
