package com.lafaspot.imapnio.async.request;

import java.util.Map;

/**
 * This class defines imap id command request from client.
 */
public class IdCommand extends ImapRequestAdapter {

    /** ID and space. */
    private static final String ID_SPACE = "ID ";

    /** ID command line initial space. */
    private static final int IDLINE_LEN = 200;

    /** Key and value pair, key and value should all be ascii. */
    private Map<String, String> params;

    /**
     * Initializes a @{code IdCommand}.
     *
     * @param params a collection of parameters, key and value should all be ascii.
     */
    public IdCommand(final Map<String, String> params) {
        this.params = params;
    }

    @Override
    public void cleanup() {
        this.params = null;
    }

    @Override
    public String getCommandLine() {
        final StringBuilder sb = new StringBuilder(IDLINE_LEN).append(ID_SPACE);

        if (params == null) {
            sb.append(ImapClientConstants.NIL);
        } else {
            // every token has to be encoded (double quoted and escaped) if needed
            // ex: a023 ID ("name" "so/"dr" "version" "19.34")
            final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
            sb.append(ImapClientConstants.L_PAREN);
            boolean isFirstEntry = true;
            for (final Map.Entry<String, String> e : params.entrySet()) {
                if (!isFirstEntry) {
                    sb.append(ImapClientConstants.SPACE);
                } else {
                    isFirstEntry = false;
                }
                formatter.formatArgument(e.getKey(), sb, true);
                sb.append(ImapClientConstants.SPACE);
                formatter.formatArgument(e.getValue(), sb, true);
            }
            sb.append(ImapClientConstants.R_PAREN);
        }

        sb.append(ImapClientConstants.CRLF);
        return sb.toString();
    }
}
