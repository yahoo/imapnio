package com.yahoo.imapnio.async.data;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * Based on RFC 3501.
 *
 * <pre>
 *
seq-number      = nz-number / "*"
                    ; message sequence number (COPY, FETCH, STORE
                    ; commands) or unique identifier (UID COPY,
                    ; UID FETCH, UID STORE commands).
                    ; * represents the largest number in use.  In
                    ; the case of message sequence numbers, it is
                    ; the number of messages in a non-empty mailbox.
                    ; In the case of unique identifiers, it is the
                    ; unique identifier of the last message in the
                    ; mailbox or, if the mailbox is empty, the
                    ; mailbox's current UIDNEXT value.
                    ; The server should respond with a tagged BAD
                    ; response to a command that uses a message
                    ; sequence number greater than the number of
                    ; messages in the selected mailbox.  This
                    ; includes "*" if the selected mailbox is empty.

seq-range       = seq-number ":" seq-number
                    ; two seq-number values and all values between
                    ; these two regardless of order.
                    ; Example: 2:4 and 4:2 are equivalent and indicate
                    ; values 2, 3, and 4.
                    ; Example: a unique identifier sequence range of
                    ; 3291:* includes the UID of the last message in
                    ; the mailbox, even if that value is less than 3291.

sequence-set    = (seq-number / seq-range) *("," sequence-set)
                    ; set of seq-number values, regardless of order.
                    ; Servers MAY coalesce overlaps and/or execute the
                    ; sequence in any order.
                    ; Example: a message sequence number set of
                    ; 2,4:7,9,12:* for a mailbox with 15 messages is
                    ; equivalent to 2,4,5,6,7,9,12,13,14,15
                    ; Example: a message sequence number set of *:4,5:7
                    ; for a mailbox with 10 messages is equivalent to
                    ; 10,9,8,7,6,5,4,5,6,7 and MAY be reordered and
                    ; overlap coalesced to be 4,5,6,7,8,9,10.
 *
 * </pre>
 */
@SuppressWarnings("hideutilityclassconstructor")
public final class MessageNumberSet {

    /**
     * Message end specifications. Whether an ending message is an absolute number or last message, or just last message.
     */
    private enum EndMessageSpecs {

        /** An absolute ending message sequence number or UID is given, for example: 3:10. */
        ABSOLUTE_END,

        /** Ends with the last message in the mailbox, for example: 3:* . */
        LAST_MESSAGE_END,

        /** Only need the last message, for example: * . */
        LAST_MESSAGE_ONLY
    }

    /** End message specification. */
    private final EndMessageSpecs endOpt;

    /** Starting message number, could be message sequence or UID. */
    private final long start;

    /** Ending message number, could be message sequence or UID. */
    private final long end;

    /**
     * Instantiates a message number set with numeric start and numeric end. For example, 4:10.
     *
     * @param start starting message sequence or UID sequence
     * @param end ending message sequence or UID sequence
     */
    public MessageNumberSet(final long start, final long end) {
        this.start = start;
        this.end = end;
        this.endOpt = EndMessageSpecs.ABSOLUTE_END;
    }

    /**
     * Instantiates a sequence set that starts with given start message number. If isEndsWithLastMessage is true, ends with last message in the
     * mailbox, otherwise only the start message. For example, it generates either message range like 1:* or message sequence as 1.
     *
     * @param start starting message sequence or UID sequence
     * @param isEndsWithLastMessage true if ends with last mail in mailbox; otherwise only the start message number
     */
    public MessageNumberSet(final long start, final boolean isEndsWithLastMessage) {
        this.start = start;
        this.end = start;
        this.endOpt = isEndsWithLastMessage ? EndMessageSpecs.LAST_MESSAGE_END : EndMessageSpecs.ABSOLUTE_END;
    }

    /**
     * Instantiates a sequence that only returns last message, aka, * .
     *
     * @param isLastMessageOnly should always be true
     * @throws ImapAsyncClientException when given isLastMessageOnly is false
     */
    public MessageNumberSet(final boolean isLastMessageOnly) throws ImapAsyncClientException {
        this.start = -1;
        this.end = -1;
        this.endOpt = EndMessageSpecs.LAST_MESSAGE_ONLY;
        if (!isLastMessageOnly) {
            throw new ImapAsyncClientException(ImapAsyncClientException.FailureType.INVALID_INPUT);
        }
    }

    /**
     * Converts an array of integers into an array of MessageNumberSet.
     *
     * @param msgs array of primitive integer data type message number (could be message sequence or UID)
     * @return MessageNumberSet array
     */
    public static MessageNumberSet[] createMessageNumberSets(final int[] msgs) {
        List<MessageNumberSet> v = new ArrayList<MessageNumberSet>();
        int i, j;
        for (i = 0; i < msgs.length; i++) {
            int start = msgs[i];

            // Look for contiguous elements
            for (j = i + 1; j < msgs.length; j++) {
                if (msgs[j] != msgs[j - 1] + 1) {
                    break;
                }
            }
            int end = msgs[j - 1];
            v.add(new MessageNumberSet(start, end));
            i = j - 1; // i gets incremented @ top of the loop
        }
        return v.toArray(new MessageNumberSet[v.size()]);
    }

    /**
     * Converts an array of long into an array of MessageNumberSet.
     *
     * @param msgs array of long data type message number (could be message sequence or UID)
     * @return the string generated that conforms to RFC3501 Message Number syntax
     */
    public static MessageNumberSet[] createMessageNumberSets(final long[] msgs) {
        List<MessageNumberSet> v = new ArrayList<MessageNumberSet>();
        int i, j;
        for (i = 0; i < msgs.length; i++) {
            long start = msgs[i];

            // Look for contiguous elements
            for (j = i + 1; j < msgs.length; j++) {
                if (msgs[j] != msgs[j - 1] + 1) {
                    break;
                }
            }
            long end = msgs[j - 1];
            v.add(new MessageNumberSet(start, end));
            i = j - 1; // i gets incremented @ top of the loop
        }
        return v.toArray(new MessageNumberSet[v.size()]);
    }

    /**
     * Converts an array of MessageNumberSet into an IMAP sequence set.
     *
     * @param msgsets array of MessageNumberSet
     * @return the string generated that conforms to RFC3501 Message Number syntax
     */
    public static String toString(@Nullable final MessageNumberSet[] msgsets) {
        if (msgsets == null || msgsets.length == 0) { // Empty msgset
            return null;
        }

        int i = 0; // msgset index
        final StringBuffer s = new StringBuffer();
        int size = msgsets.length;
        long start, end;

        for (;;) {
            start = msgsets[i].start;
            end = msgsets[i].end;

            if (msgsets[i].endOpt == EndMessageSpecs.LAST_MESSAGE_ONLY) {
                s.append('*');
            } else if (msgsets[i].endOpt == EndMessageSpecs.LAST_MESSAGE_END) {
                s.append(start).append(':').append('*');
            } else if (end > start) {
                s.append(start).append(':').append(end);
            } else { // end == start means only one element
                s.append(start);
            }

            i++; // increment to next round
            if (i >= size) { // No more MessageNumberSet objects
                break;
            } else {
                s.append(',');
            }
        }
        return s.toString();
    }

}
