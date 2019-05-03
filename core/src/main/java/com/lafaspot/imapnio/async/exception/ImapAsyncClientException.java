package com.lafaspot.imapnio.async.exception;

import javax.annotation.Nonnull;

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

        /** Time-out on server connection. */
        CONNECTION_FAILED_EXCEED_IDLE_MAX("Time-out on server connection."),

        /** Connection inactive. */
        CONNECTION_INACTIVE("Connection inactive."),

        /** Operation on an already closed channel. */
        OPERATION_PROHIBITED_ON_CLOSED_CHANNEL("Operation on a closed channel is prohibited."),

        /** Command is not allowed to be executed. */
        COMMAND_NOT_ALLOWED("Command is not allowed to be executed."),

        /** Write to imap server failed. */
        WRITE_TO_SERVER_FAILED("Write to imap server failed."),

        /** Constructing imap command failed. */
        COMMAND_CONSTRUCTION_FAILED("Constructing imap command failed."),

        /** Failed in closing conneciton. */
        CLOSING_CONNECTION_FAILED("Failed in closing conneciton"),

        /** Encountering exception during communication to remote. */
        CHANNEL_EXCEPTION("Encountering exception during communication to remote."),

        /** Channel disconnected message. */
        CHANNEL_DISCONNECTED("Channel was closed already."),

        /** This operation is not supported for this command. */
        OPERATION_NOT_SUPPORTED_FOR_COMMAND("This operation is not supported for this command."),

        /** Timeout from server. */
        CHANNEL_TIMEOUT("Timeout from server after command is sent."),

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
     * Initializes a {code ImapAsyncClientException} with failure type.
     *
     * @param failureType the reason it fails
     */
    public ImapAsyncClientException(@Nonnull final FailureType failureType) {
        this(failureType, null);
    }

    /**
     * Initializes a @{code ImapAsyncClientException} with failure type and cause.
     *
     * @param failureType the reason it fails
     * @param cause the exception underneath
     */
    public ImapAsyncClientException(@Nonnull final FailureType failureType, final Throwable cause) {
        super(cause);
        this.failureType = failureType;
    }

    /**
     * @return the failure type
     */
    public FailureType getFaiureType() {
        return failureType;
    }

    @Override
    public String getMessage() {
        final StringBuilder sb = new StringBuilder("failureType=").append(failureType.name());
        return sb.toString();
    }
}
