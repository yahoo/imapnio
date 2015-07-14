/**
 *
 */
package com.lafaspot.imapnio.config;

/**
 * @author kraman
 *
 */
public class ImapClientConfig {

    /** Number of threads used by the event loop group. */
    private static final int EVENT_GROUP_NUM_THREADS = 5;
    
    public int getNumThreads() {
    	return EVENT_GROUP_NUM_THREADS;
    }

}
