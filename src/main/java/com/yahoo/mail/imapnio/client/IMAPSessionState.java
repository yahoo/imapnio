/**
 *
 */
package com.yahoo.mail.imapnio.client;

/**
 * @author kraman
 *
 */
public enum IMAPSessionState {
    Connected("connected"), // indicates connection but no auth attempts or changes
    LoginFailed("failed"), // indicates failed login
    LoggedIn("loggedIn"), // indicates successful login in the past, distinct from
    LoggedOut("loggedOut"), // `failed` and `connected`
    IDLE_REQUEST("idle"), // idle requested
    IDLING("idling"), // idling
    OAUTH2_INIT("oauth2Init"); // initiated

    public final String state;

    private IMAPSessionState(final String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return state;
    }
}
