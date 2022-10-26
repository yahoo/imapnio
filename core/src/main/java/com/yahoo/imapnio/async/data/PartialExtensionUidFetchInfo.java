package com.yahoo.imapnio.async.data;

/**
 * The PARTIAL extension of the Internet Message Access Protocol (RFC 3501/RFC 9051) allows clients
 * to limit the number of search results returned, as well as to perform incremental (paged) searches.
 * This also helps servers to optimize resource usage when performing searches.
 *
 * <pre>
 * {@code
 * partial-range-first = nz-number ":" nz-number
 *        ;; Request to search from oldest (lowest UIDs) to
 *        ;; more recent messages.
 *        ;; A range 500:400 is the same as 400:500.
 *        ;; This is similar to <seq-range> from [RFC3501],
 *        ;; but cannot contain "*".
 *
 * partial-range-last  = MINUS nz-number ":" MINUS nz-number
 *        ;; Request to search from newest (highest UIDs) to
 *        ;; oldest messages.
 *        ;; A range -500:-400 is the same as -400:-500.
 *
 * nz-number       = digit-nz *DIGIT
 *                   ; Non-zero unsigned 32-bit integer
 *                   ; (0 < n < 4,294,967,296)
 * }
 * </pre>
 */

public class PartialExtensionUidFetchInfo {
    /** Lowest uid number that needs to be searched. */
    private final int lowestUid;

    /** Highest uid number that needs to be searched. */
    private final int highestUid;

    /**
     * Instantiates a {@link PartialExtensionUidFetchInfo} with specific range including lowest and highest uids.
     *
     * @param lowestUid lowest uid to be searched
     * @param highestUid highest uid to be searched
     */
    public PartialExtensionUidFetchInfo(final int lowestUid, final int highestUid) {
        this.lowestUid = lowestUid;
        this.highestUid = highestUid;
    }

    /**
     * Returns lowest uid.
     *
     * @return lowest uid
     */
    public int getLowestUid() {
        return lowestUid;
    }

    /**
     * Returns highest uid.
     *
     * @return highest uid
     */
    public int getHighestUid() {
        return highestUid;
    }
}
