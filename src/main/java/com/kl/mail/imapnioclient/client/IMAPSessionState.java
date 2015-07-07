/**
 *
 */
package com.kl.mail.imapnioclient.client;

/**
 * @author kraman
 *
 */
public enum IMAPSessionState {
	ConnectRequest("connect"),
    Connected("connected"), // indicates connection but no auth attempts or changes
    LoginFailed("failed"), // indicates failed login
    IDLE_REQUEST("idle"), // idle requested
    IDLING("idling"); // idling

    public final String state;

    private IMAPSessionState(final String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return state;
    }
}
