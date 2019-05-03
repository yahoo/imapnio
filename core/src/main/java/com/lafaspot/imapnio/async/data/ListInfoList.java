package com.lafaspot.imapnio.async.data;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.ListInfo;

/**
 * This class provides the ListInfo information converted from LIST or LSUB command IMAPResponse.
 */
public class ListInfoList {
    /** List of ListInfo objects. */
    private final List<ListInfo> infos;

    /**
     * Initializes a ListInfoList object with the given ListInfo collection.
     *
     * @param infos list of ListInfo objects
     */
    public ListInfoList(@Nonnull final List<ListInfo> infos) {
        // make it immutable here so we avoid keeping creating UnmodifiableList whenever getter is called, assuming we do not change
        this.infos = Collections.unmodifiableList(infos);
    }

    /**
     * @return list of ListInfo objects
     */
    public List<ListInfo> getListInfo() {
        return this.infos;
    }
}
