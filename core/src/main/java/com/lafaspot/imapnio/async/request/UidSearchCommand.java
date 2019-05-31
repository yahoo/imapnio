package com.lafaspot.imapnio.async.request;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeUtility;
import javax.mail.search.SearchException;
import javax.mail.search.SearchTerm;

import com.lafaspot.imapnio.command.Argument;
import com.sun.mail.imap.protocol.SearchSequence;
import com.sun.mail.imap.protocol.UIDSet;

/**
 * This class defines IMAP UID search command request from client.
 */
public class UidSearchCommand extends ImapRequestAdapter {

    /** Charset literal following by a space. */
    private static final String CHARSET = "CHARSET ";

    /** UID SEARCH and space. */
    private static final String UID_SEARCH_SPACE = "UID SEARCH ";

    /** A collection of message UID specified based on RFC3501 syntax. */
    private String uids;

    /** The search expression tree. */
    private SearchTerm term;

    /**
     * Initializes a @{code UidSearchCommand} object with required parameters.
     *
     * @param uidsets the list of UID set
     * @param term the search expression tree
     */
    public UidSearchCommand(@Nonnull final UIDSet[] uidsets, @Nonnull final SearchTerm term) {
        this(UIDSet.toString(uidsets), term);
    }

    /**
     * Initializes a @{code UidSearchCommand} with the msg UID string directly.
     *
     * @param uids the messages UID string
     * @param term the search expression tree
     */
    private UidSearchCommand(@Nonnull final String uids, @Nonnull final SearchTerm term) {
        this.uids = uids;
        this.term = term;
    }

    @Override
    public void cleanup() {
        this.uids = null;
        this.term = null;
    }

    @Override
    public String getCommandLine() throws SearchException, IOException {
        // maintain same semantic with IMAPProtocol, but we do not retry when fail in various of charsets. we only use UTF_8
        final String charset = SearchSequence.isAscii(term) ? null : StandardCharsets.UTF_8.name();

        // convert from search expression tree to String
        final SearchSequence searchSeq = new SearchSequence();
        final Argument args = new Argument();
        args.append(searchSeq.generateSequence(term, charset == null ? null : MimeUtility.javaCharset(charset)));
        args.writeAtom(uids);

        final String searchStr = args.toString();

        final StringBuilder sb = new StringBuilder(searchStr.length() + ImapClientConstants.PAD_LEN).append(UID_SEARCH_SPACE);

        if (charset != null) {
            sb.append(CHARSET).append(charset).append(ImapClientConstants.SPACE);
        }
        sb.append(searchStr).append(ImapClientConstants.CRLF);
        return sb.toString();
    }
}
