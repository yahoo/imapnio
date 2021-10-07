package com.yahoo.imapnio.async.data;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * This class provides the functionality to allow callers to obtain Enable command result given by imap server.
 */
public final class EnableResult {

    /** EnableResult list. */
    private final Set<String> capabilities;

    /**
     * Initializes the {@link EnableResult} class.
     *
     * @param capas set of enabled capability name with its values if existing
     */
    public EnableResult(@Nonnull final Set<String> capas) {
        this.capabilities = Collections.unmodifiableSet(capas);
    }

    /**
     * Returns the enabled capabilities.
     *
     * @return the enabled capabilities
     */
    public Set<String> getEnabledCapabilities() {
        return capabilities;
    }
}
