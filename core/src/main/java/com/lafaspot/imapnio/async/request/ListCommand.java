package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

/**
 * This class defines imap list command request from client.
 */
public class ListCommand extends AbstractQueryFoldersCommand {

    /** Command name. */
    private static final String LIST = "LIST";

    /**
     * Initializes a @{code ListCommand}.
     *
     * @param ref the reference string
     * @param pattern folder name with possible wildcards, see RFC3501 list command for detail.
     */
    public ListCommand(@Nonnull final String ref, @Nonnull final String pattern) {
        super(LIST, ref, pattern);
    }
}
