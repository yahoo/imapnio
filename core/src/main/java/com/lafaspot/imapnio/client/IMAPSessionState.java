/**
 *
 */
package com.lafaspot.imapnio.client;

/**
 * @author kraman
 *
 */
public enum IMAPSessionState {
    /** socket connect reauest. */
	ConnectRequest("connect"),
	/** socket connected. */
    Connected("connected"), // indicates connection but no auth attempts or changes
    /** login failed. */
    LoginFailed("failed"), // indicates failed login
    /** idle command sent. */
    IDLE_REQUEST("idle"), // idle requested
    /** entered idle mode. */
    IDLING("idling"); // idling

	/** session state. */
    private final String state;

    /**
     * Constructs a session state.
     * @param state string representing the state
     */
    private IMAPSessionState(final String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return state;
    }
}
