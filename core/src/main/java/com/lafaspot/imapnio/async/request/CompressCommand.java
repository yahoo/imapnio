package com.lafaspot.imapnio.async.request;

/**
 * This class defines imap Compress request from client.
 * 
 * @see "RFC 4978"
 */
public class CompressCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String COMPRESS_DEFLATE = "COMPRESS DEFLATE";

    /**
     * Initializes the @{code COMPRESS_DEFLATECommand}.
     * 
     * @see "RFC 4978"
     */
    public CompressCommand() {
        super(COMPRESS_DEFLATE);
    }

    @Override
    public boolean isCompressionRequested() {
        return true;
    }
}
