/**
 * 
 */
package org.apache.imapnioclient.exception;

/**
 * @author kraman
 *
 */
public class IMAPSessionException extends Exception {

	private final String reason;    
	protected final String id;
	public IMAPSessionException(String r, Throwable t) {
		super (t);
		reason = r;
        this.id = Thread.currentThread().getName() + ":" + System.currentTimeMillis();
	}
	
	public IMAPSessionException(String r) {
		reason = r;
        this.id = Thread.currentThread().getName() + ":" + System.currentTimeMillis();
	}
	
    /**
     * Convert the exception into a String for display and/or processing.
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
