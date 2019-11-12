package com.yahoo.imapnio.async.client;

import javax.annotation.Nullable;

/**
 * Class for allowing client putting context about this session.
 */
public final class ImapAsyncSessionClientContext {

    /** Empty string constant. */
    private static final String NA = "NA";

    /**
     * User Id. For example, the email address.
     */
    @Nullable
    private String userId;

    /**
     * @return the user id
     */
    @Nullable
    public String getUserId() {
        return (userId == null) ? NA : userId;
    }

    /**
     * Sets the user Id for this session.
     *
     * @param userId the user Id for the session
     */
    public void setUserId(final String userId) {
        this.userId = userId;
    }
}
