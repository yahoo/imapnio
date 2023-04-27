package com.yahoo.imapnio.async.request;

/**
 * This class defines imap Compress request from client.
 * 
 * @see "RFC 4978"
 */
public final class CompressCommand extends AbstractNoArgsCommand {

    /** Command name. */
    private static final String COMPRESS_DEFLATE = "COMPRESS DEFLATE";

    /**
     * Initializes the {@link CompressCommand}.
     * 
     * @see "RFC 4978"
     */
    public CompressCommand() {
        super(COMPRESS_DEFLATE);
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.COMPRESS;
    }
}
