package com.yahoo.imapnio.async.data;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class provides the functionality to allow callers to obtain Id command result given by imap server.
 */
public final class IdResult {

    /** IdResult list. */
    private final Map<String, String> params;

    /**
     * Initializes the {@link IdResult} class.
     *
     * @param map map of capability name with its values if existing
     */
    public IdResult(@Nonnull final Map<String, String> map) {
        this.params = map;
    }

    /**
     * Returns true if the keyName given is present in the id list; false otherwise.
     *
     * @param keyName the capability name to find
     * @return true if the result has the key; false otherwise
     */
    public boolean hasKey(@Nullable final String keyName) {
        return params.containsKey(keyName);
    }

    /**
     * Returns the value based on the given key name.
     *
     * @param keyName the capability name to find
     * @return the value
     */
    public String getValue(@Nullable final String keyName) {
        return params.get(keyName);
    }
}
