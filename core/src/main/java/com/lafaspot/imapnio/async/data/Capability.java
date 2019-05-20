package com.lafaspot.imapnio.async.data;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * This class provides the functionality to allow callers to obtain capabilities given by imap server.
 */
public final class Capability {

    /** Capability list. */
    private final Map<String, List<String>> capas;

    /**
     * Initializes the @{code Capability} class.
     *
     * @param capabilities map of capability name with its values if existing
     */
    public Capability(@Nonnull final Map<String, List<String>> capabilities) {
        this.capas = capabilities;
    }

    /**
     * Returns true if the capability is supported from server; false otherwise.
     *
     * @param capaName the capability name to find
     * @return true if the capability is supported from server
     */
    public boolean hasCapability(@Nonnull final String capaName) {
        return capas.containsKey(capaName.toUpperCase());
    }

    /**
     * Returns the various values for a specific capability, such as AUTH mechanisms that Imap server supports. For example, passing "AUTH" can return
     * a list of PLAIN, XOAUTH2, XBLURDYBLOOP.
     *
     * @param capaName the capability name to find
     * @return list of values for a specific capability name, List is immutable
     */
    public List<String> getCapability(@Nonnull final String capaName) {
        return capas.get(capaName.toUpperCase());
    }
}
