package com.yahoo.imapnio.async.request;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.MimeUtility;
import javax.mail.search.SearchException;
import javax.mail.search.SearchTerm;

import com.sun.mail.imap.protocol.MessageSet;
import com.sun.mail.imap.protocol.SearchSequence;
import com.yahoo.imapnio.command.Argument;

/**
 * This class defines IMAP search command request from client.
 */
public class SearchCommand extends ImapRequestAdapter {

    /** Charset literal following by a space. */
    private static final String CHARSET = "CHARSET ";

    /** Command name. */
    private static final String SEARCH_SP = "SEARCH ";

    /** Message sequences. */
    private String msgSeqs;

    /** The complete search String. */
    private String searchStr;

    /** Character set. */
    private String charset;

    /**
     * Initializes a @{code SearchCommand} object with required parameters.
     * 
     * @param msgsets the set of message set
     * @param term the search expression tree
     * @throws IOException when SearchSequence.generateSequence() encounters IO error
     * @throws SearchException when SearchSequence.generateSequence() encounters error
     */
    public SearchCommand(@Nonnull final MessageSet[] msgsets, @Nullable final SearchTerm term) throws SearchException, IOException {
        this.msgSeqs = MessageSet.toString(msgsets);
        if (term != null) {
            // maintain same semantic with IMAPProtocol, but we do not retry when fail in various of charsets. we only use UTF_8
            this.charset = SearchSequence.isAscii(term) ? null : StandardCharsets.UTF_8.name();

            // convert from search expression tree to String
            final SearchSequence searchSeq = new SearchSequence();
            final Argument args = new Argument();
            args.append(searchSeq.generateSequence(term, charset == null ? null : MimeUtility.javaCharset(charset)));
            this.searchStr = args.toString();
        }
    }

    /**
     * Initializes a @{code SearchCommand} with the string form of message sequence, search string and character set name.
     *
     * @param msgSeqs the messages sequence string
     * @param searchString the search string
     * @param charset the character set
     */
    public SearchCommand(@Nonnull final String msgSeqs, @Nullable final String searchString, @Nullable final String charset) {
        this.msgSeqs = msgSeqs;
        this.searchStr = searchString;
        this.charset = charset;
    }

    @Override
    public void cleanup() {
        this.msgSeqs = null;
        this.searchStr = null;
        this.charset = null;
    }

    @Override
    public String getCommandLine() throws SearchException, IOException {
        final int searchLen = (searchStr != null) ? searchStr.length() : 0;
        final StringBuilder sb = new StringBuilder(msgSeqs.length() + searchLen + ImapClientConstants.PAD_LEN).append(SEARCH_SP);

        if (charset != null) {
            sb.append(CHARSET).append(charset).append(ImapClientConstants.SPACE);
        }
        sb.append(msgSeqs);

        if (searchStr != null) {
            sb.append(ImapClientConstants.SPACE).append(searchStr);
        }
        sb.append(ImapClientConstants.CRLF);
        return sb.toString();
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.SEARCH;
    }
}
