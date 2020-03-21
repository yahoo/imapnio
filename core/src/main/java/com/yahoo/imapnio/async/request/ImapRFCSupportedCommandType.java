package com.yahoo.imapnio.async.request;

/**
 * IMAP command type which is RFC-supported.
 */
public enum ImapRFCSupportedCommandType implements ImapCommandType {
    /** Append message command. */
    APPEND_MESSAGE,
    /** Authenticate plain command. */
    AUTHENTICATE,
    /** Capability command. */
    CAPABILITY,
    /** Check command. */
    CHECK,
    /** Close command. */
    CLOSE,
    /** Compress command. */
    COMPRESS,
    /** Copy message command. */
    COPY_MESSAGE,
    /** Create folder command. */
    CREATE_FOLDER,
    /** Delete folder command. */
    DELETE_FOLDER,
    /** Enable capability command. */
    ENABLE,
    /** Examine folder command. */
    EXAMINE_FOLDER,
    /** Expunge command. */
    EXPUNGE,
    /** Fetch command. */
    FETCH,
    /** Id command. */
    ID,
    /** Idle command. */
    IDLE,
    /** List command. */
    LIST,
    /** Login command. */
    LOGIN,
    /** Logout command. */
    LOGOUT,
    /** LSUB command. */
    LSUB,
    /** Move message command. */
    MOVE_MESSAGE,
    /** Namespace command. */
    NAMESPACE,
    /** Noop command. */
    NOOP,
    /** Rename folder command. */
    RENAME_FOLDER,
    /** Search command. */
    SEARCH,
    /** Select folder command. */
    SELECT_FOLDER,
    /** Status command. */
    STATUS,
    /** Store flags command. */
    STORE_FLAGS,
    /** Subscribe command. */
    SUBSCRIBE,
    /** UID copy command. */
    UID_COPY_MESSAGE,
    /** UID expunge command. */
    UID_EXPUNGE,
    /** UID fetch command. */
    UID_FETCH,
    /** UID move command. */
    UID_MOVE_MESSAGE,
    /** UID search command. */
    UID_SEARCH,
    /** UID store command. */
    UID_STORE_FLAGS,
    /** Unselect command. */
    UNSELECT,
    /** Unsubscribe command. */
    UNSUBSCRIBE;

    @Override
    public String getType() {
        return this.name();
    }
}
