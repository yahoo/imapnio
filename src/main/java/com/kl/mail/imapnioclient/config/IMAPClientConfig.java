/**
 *
 */
package com.kl.mail.imapnioclient.config;

/**
 * @author kraman
 *
 */
public class IMAPClientConfig {

    /** Number of threads used by the event loop group. */
    private static final int EVENT_GROUP_NUM_THREADS = 5;
    
    public int getNumThreads() {
    	return EVENT_GROUP_NUM_THREADS;
    }

}
