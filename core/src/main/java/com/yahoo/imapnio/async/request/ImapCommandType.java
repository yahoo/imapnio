package com.yahoo.imapnio.async.request;

/**
 * IMAP command type.
 */
public interface ImapCommandType {
    /**
     * @return the type of the command
     */
    String getType();
}
