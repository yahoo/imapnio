package com.yahoo.imapnio.async.exception;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class defines various kinds of reason for imap asynchronous exception.
 */
public class ImapAsyncClientException extends Exception {

    /** Required. */
    private static final long serialVersionUID = 1L;

    /**
     * An Enum that specifies different types of failure for an unsuccessful operation.
     */
    public enum FailureType {
        /** Server greeting is absent upon connection. */
        CONNECTION_FAILED_WITHOUT_OK_RESPONSE("Server greeting is absent upon connection."),

        /** Connection failed with an exception. */
        CONNECTION_FAILED_EXCEPTION("Connection failed with an exception."),

        /** Connection failed with unknown host exception. */
        UNKNOWN_HOST_EXCEPTION("Connection failed with unknown host exception."),

        /** Connection failed with connection timeout exception. */
        CONNECTION_TIMEOUT_EXCEPTION("Connection failed with connection timeout exception."),

        /** Time-out on server connection. */
        CONNECTION_FAILED_EXCEED_IDLE_MAX("Time-out on server connection."),

        /** Connection failed due to ssl error. */
        CONNECTION_SSL_EXCEPTION("SSL error during connection."),

        /** Connection inactive. */
        CONNECTION_INACTIVE("Connection inactive."),

        /** Operation on an already closed channel. */
        OPERATION_PROHIBITED_ON_CLOSED_CHANNEL("Operation on a closed channel is prohibited."),

        /** Command is not allowed to be executed. */
        COMMAND_NOT_ALLOWED("Command is not allowed to be executed."),

        /** Write to imap server failed. */
        WRITE_TO_SERVER_FAILED("Write to imap server failed."),

        /** Failed in closing connection. */
        CLOSING_CONNECTION_FAILED("Failed in closing connection"),

        /** Encountering exception during communication to remote. */
        CHANNEL_EXCEPTION("Encountering exception during communication to remote."),

        /** Channel disconnected message. */
        CHANNEL_DISCONNECTED("Channel was closed already."),

        /** This operation is not supported for this command. */
        OPERATION_NOT_SUPPORTED_FOR_COMMAND("This operation is not supported for this command."),

        /** Timeout from server. */
        CHANNEL_TIMEOUT("Timeout from server after command is sent."),

        /** Given class type to parse to is unknown. */
        UNKNOWN_PARSE_RESULT_TYPE("Given class type to parse to is unknown."),

        /** Invalid input. */
        INVALID_INPUT("Input is invalid.");

        /** The error message associated with this failure type. */
        @Nonnull
        private final String message;

        /**
         * Constructor to add an error message for failure type belonging to this enum.
         *
         * @param message the error message associated with this failure type
         */
        FailureType(@Nonnull final String message) {
            this.message = message;
        }
    }

    /**
     * The failure type.
     */
    @Nonnull
    private FailureType failureType;

    /**
     * The session id.
     */
    @Nullable
    private Long sessionId;

    /**
     * The information about this session that client wants to be printed when exception is displayed.
     */
    @Nullable
    private String userInfo;

    /**
     * Initializes a {code ImapAsyncClientException} with failure type. It is used when session is not created.
     *
     * @param failureType the reason it fails
     */
    public ImapAsyncClientException(@Nonnull final FailureType failureType) {
        this(failureType, null, null, null);
    }

    /**
     * Initializes a {code ImapAsyncClientException}with failure type and cause. It is used when session is not created.
     *
     * @param failureType the reason it fails
     * @param cause the exception underneath
     */
    public ImapAsyncClientException(@Nonnull final FailureType failureType, final Throwable cause) {
        this(failureType, cause, null, null);
    }

    /**
     * Initializes a {code ImapAsyncClientException} with failure type and session id.
     *
     * @param failureType the reason it fails
     * @param sessionId the session id
     * @param sessionCtx user information sent by caller to identify this session, used for displaying in exception
     */
    public ImapAsyncClientException(@Nonnull final FailureType failureType, @Nonnull final Long sessionId, @Nonnull final Object sessionCtx) {
        this(failureType, null, sessionId, sessionCtx);
    }

    /**
     * Initializes a {@link ImapAsyncClientException} with failure type and cause.
     *
     * @param failureType the reason it fails
     * @param cause the exception underneath
     * @param sessionId the session id
     * @param sessionCtx user information sent by caller to identify this session, used for displaying in exception
     */
    public ImapAsyncClientException(@Nonnull final FailureType failureType, @Nullable final Throwable cause, @Nullable final Long sessionId,
            @Nullable final Object sessionCtx) {
        super(cause);
        this.failureType = failureType;
        this.sessionId = sessionId;
        this.userInfo = (sessionCtx != null) ? sessionCtx.toString() : null;
    }

    /**
     * @return the failure type
     */
    public FailureType getFailureType() {
        return failureType;
    }

    @Override
    public String getMessage() {
        final StringBuilder sb = new StringBuilder("failureType=").append(failureType.name());
        if (sessionId != null) {
            sb.append(",sId=").append(sessionId);
        }
        if (userInfo != null) {
            sb.append(",uId=").append(userInfo);
        }
        return sb.toString();
    }
}
