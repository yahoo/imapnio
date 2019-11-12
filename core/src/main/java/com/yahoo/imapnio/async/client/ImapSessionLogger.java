package com.yahoo.imapnio.async.client;

/**
 * A class that allows caller to send the debug messages. This class can decide the format and added session related information.
 */
public interface ImapSessionLogger {

    /**
     * Returns true if debug is enabled; false otherwise. This method can be used for caller to decide whether to generate the debug message.
     *
     * @return true if debug is enabled; false otherwise
     */
    boolean isDebugEnabled();

    /**
     * Allows caller to send the debug message. This class can decide the format and added session related information.
     *
     * @param debugMessage the debugging message that caller wants to log
     */
    void logDebugMessage(String debugMessage);

}
