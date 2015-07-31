/**
 * 
 */
package com.lafaspot.imapnio.exception;

/**
 * @author kraman
 *
 */
public class IMAPSessionException extends Exception {

    /** The reason for the exception. */
    private final String reason;
    /** the id. */
    protected final String id;

    /**
     * Constructs a session exception. 
     * @param r reason string
     * @param t the throwable
     */
    public IMAPSessionException(final String r, final Throwable t) {
        super(t);
        reason = r;
        this.id = Thread.currentThread().getName() + ":" + System.currentTimeMillis();
    }

    /** 
     * Constructs a session exception.
     * @param r reason string
     */
    public IMAPSessionException(final String r) {
        reason = r;
        this.id = Thread.currentThread().getName() + ":" + System.currentTimeMillis();
    }

    /**
     * Convert the exception into a String for display and/or processing.
     * @return the string value
     */
    @Override
    public String toString() {
        final StringBuilder toRet = new StringBuilder(super.toString());

        toRet.append("\nExceptionId:").append(this.id);

        toRet.append("\nRoot cause: ");
        toRet.append(reason);
        toRet.append(super.toString());

        return toRet.toString();
    }

    /** A default serialization ID. */
    private static final long serialVersionUID = 1L;
}
