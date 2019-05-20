package com.lafaspot.imapnio.async.request;

import javax.annotation.Nonnull;

/**
 * This class defines imap lsub command request from client.
 */
public class LSubCommand extends AbstractQueryFoldersCommand {

    /** Command name. */
    private static final String LSUB = "LSUB";

    /**
     * Initializes a @{code LSubCommand}.
     *
     * @param ref the reference string
     * @param pattern folder name with possible wildcards, see RFC3501 LSUB command for detail.
     */
    public LSubCommand(@Nonnull final String ref, @Nonnull final String pattern) {
        super(LSUB, ref, pattern);
    }
}
