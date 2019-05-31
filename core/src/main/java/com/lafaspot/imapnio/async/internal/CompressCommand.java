package com.lafaspot.imapnio.async.internal;

import com.lafaspot.imapnio.async.request.AbstractNoArgsCommand;

/**
 * This class defines imap Compress request from client.
 * 
 * @see "RFC 4978"
 */
final class CompressCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String COMPRESS_DEFLATE = "COMPRESS DEFLATE";

    /**
     * Initializes the @{code COMPRESS_DEFLATECommand}. Constructor is only visible to this package so it can only be called by @{code
     * ImapAsyncSessionImpl}.
     * 
     * @see "RFC 4978"
     */
    CompressCommand() {
        super(COMPRESS_DEFLATE);
    }

    @Override
    public boolean isCompressionRequested() {
        return true;
    }
}
