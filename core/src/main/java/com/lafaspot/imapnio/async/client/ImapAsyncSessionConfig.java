package com.lafaspot.imapnio.async.client;

/**
 * Class for IMAP Client connection and channel settings.
 */
public final class ImapAsyncSessionConfig {

    /**
     * Maximum time in milliseconds for opening a connection, this maps to CONNECT_TIMEOUT_MILLIS in @{code ChannelOption}, it will be used when
     * establishing a connection.
     */
    private int connectionTimeoutMillis = -1;

    /**
     * Maximum time in milliseconds for read timeout, this maps to the readIdleTime in @{code IdleStateHandler}.
     */
    private int readTimeoutMillis = -1;

    /**
     * @return Maximum time for opening a connection
     */
    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    /**
     * Sets the maximum time for opening a connection in milliseconds.
     * 
     * @param connectionTimeoutMillis time in milliseconds
     */
    public void setConnectionTimeoutMillis(final int connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }

    /**
     * @return Maximum time for read timeout
     */
    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    /**
     * Sets the maximum time for read timeout, this means the time waiting for server to respond.
     * 
     * @param readTimeoutMillis time in milliseconds
     */
    public void setReadTimeoutMillis(final int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

}
