package com.yahoo.imapnio.async.data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.Status;

/**
 * This class provides the result converted from LIST-STATUS responses. It contains a collection of ListInfo (similar to List command), also it has a
 * map of Status objects where the key is the Status object's mbox field value.
 */
public class ListStatusResult {

    /** List of ListInfo objects. */
    private final List<ExtensionListInfo> infos;

    /** A map where the key is mail box name, aka Status.mbox and the value is Status object. */
    private final Map<String, Status> statuses;

    /**
     * Initializes a ListStatusResult object with the given list of ListInfo objects and a map of Status objects.
     *
     * @param infos list of ListStatus objects
     * @param statuses a map of Status objects where the key is Status.mbox
     */
    public ListStatusResult(@Nonnull final List<ExtensionListInfo> infos, @Nonnull final Map<String, Status> statuses) {
        this.infos = Collections.unmodifiableList(infos);
        this.statuses = Collections.unmodifiableMap(statuses);
    }

    /**
     * @return list of ExtensionListInfo objects
     */
    public List<ExtensionListInfo> getListInfos() {
        return this.infos;
    }

    /**
     * @return a map of Status objects where the key is Status.mbox.
     */
    public Map<String, Status> getStatuses() {
        return this.statuses;
    }
}
