package com.yahoo.imapnio.async.client;

/**
 * Class for IMAP Client connection and channel settings.
 */
public final class ImapAsyncSessionConfig {

    /** Default connection timeout value in milliseconds. */
    public static final int DEFAULT_CONNECTION_TIMEOUT_MILLIS = 10000;

    /** Default IMAP command response read from server timeout value in milliseconds. */
    public static final int DEFAULT_READ_TIMEOUT_MILLIS = 10000;

    /**
     * Maximum time in milliseconds for opening a connection, this maps to CONNECT_TIMEOUT_MILLIS in {@code ChannelOption}, it will be used when
     * establishing a connection.
     */
    private int connectionTimeoutMillis = DEFAULT_CONNECTION_TIMEOUT_MILLIS;

    /**
     * Maximum time in milliseconds for read timeout. The maximum time allowing no responses from server since client command sent.
     */
    private int readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS;

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
