package com.lafaspot.imapnio.client;

import javax.annotation.concurrent.Immutable;

import com.lafaspot.logfast.logging.LogContext;
/**
 * Logger for a single session.
 * 
 * @author ntraj
 *
 */
@Immutable
public class SessionLogContext extends LogContext {

    /**
     * Initialize the logger context with a name.
     *
     * @param name of the logger
     */
    public SessionLogContext(final String name) {
        super(name);
        debugId = null;
    }

    /**
     * Initialize the logger with name and debug id.
     *
     * @param name of the logger
     * @param debugId session identifier
     */
    public SessionLogContext(final String name, final String debugId) {
        super(name);
        this.debugId = debugId;
    }

    @Override
    public String getSerial() {
        // Log data related to this session
        return "{" + getName() + "," + debugId + "}";
    }

    /** instance id - used for debug. */
    private final String debugId;
}