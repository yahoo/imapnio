/**
 *
 */
package com.lafaspot.imapnio.client;

/**
 * Valid states for IMAP session.
 *
 * @author kraman
 *
 */
public enum IMAPSessionState {
    /** session is in disconnected state. */
    DISCONNECTED("disconnected"),
    /** socket connect request. */
    CONNECT_SENT("connect"), /** socket connected. */
    CONNECTED("connected"), // indicates connection but no auth attempts or changes
    /** idle command sent. */
    IDLE_SENT("idle"), // idle requested
    /** entered idle mode. */
    IDLING("idling"), // idling
    /** IDLE completed, DONE sent. */
    DONE_SENT("done");

    /** session state. */
    private final String state;

    /**
     * Constructs a session state.
     *
     * @param state
     *            string representing the state
     */
    private IMAPSessionState(final String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return state;
    }
}
