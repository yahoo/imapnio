package com.lafaspot.imapnio.async.request;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeUtility;
import javax.mail.search.SearchException;
import javax.mail.search.SearchTerm;

import com.lafaspot.imapnio.command.Argument;
import com.sun.mail.imap.protocol.MessageSet;
import com.sun.mail.imap.protocol.SearchSequence;

/**
 * This class defines imap search command request from client.
 */
public class SearchCommand extends ImapRequestAdapter {

    /** Charset literal following by a space. */
    private static final String CHARSET = "CHARSET ";

    /** UID SEARCH and space. */
    private static final String UID_SEARCH_SPACE = "UID SEARCH ";

    /** Command name. */
    private static final String SEARCH_SP = "SEARCH ";

    /** True if the message number represents UID; false if it represents Message Sequences. */
    private boolean isUid;

    /** A collection of messages specified based on RFC3501 syntax. */
    private String msgIds;

    /** The search expression tree. */
    private SearchTerm term;

    /**
     * Initializes a @{code SearchCommand} object with required parameters.
     *
     * @param isUid either uid or message sequence
     * @param msgsets the set of message set
     * @param term the search expression tree
     */
    public SearchCommand(final boolean isUid, @Nonnull final MessageSet[] msgsets, @Nonnull final SearchTerm term) {
        this(isUid, MessageSet.toString(msgsets), term);
    }


    /**
     * Initializes a @{code SearchCommand} with the msg string directly.
     *
     * @param isUid either uid or message sequence
     * @param msgIds the messages id string
     * @param term the search expression tree
     */
    private SearchCommand(final boolean isUid, @Nonnull final String msgIds, @Nonnull final SearchTerm term) {
        this.isUid = isUid;
        this.msgIds = msgIds;
        this.term = term;
    }

    @Override
    public void cleanup() {
        this.msgIds = null;
        this.term = null;
    }

    @Override
    public String getCommandLine() throws SearchException, IOException {
        // maintain same semantic with IMAPProtocol, but we not retry when fail in various of charsets. we only use UTF_8
        // note:yahoo does not have UTF_8 in capability
        final String charset = SearchSequence.isAscii(term) ? null : StandardCharsets.UTF_8.name();

        // convert from search expression tree to String
        final SearchSequence searchSeq = new SearchSequence();
        final Argument args = new Argument();
        args.append(searchSeq.generateSequence(term, charset == null ? null : MimeUtility.javaCharset(charset)));
        args.writeAtom(msgIds);

        final String searchStr = args.toString();

        final StringBuilder sb = new StringBuilder(searchStr.length() + ImapClientConstants.PAD_LEN).append(isUid ? UID_SEARCH_SPACE : SEARCH_SP);

        if (charset != null) {
            sb.append(CHARSET).append(charset).append(ImapClientConstants.SPACE);
        }
        sb.append(searchStr).append(ImapClientConstants.CRLF);
        return sb.toString();
    }
}
