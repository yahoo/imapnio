package com.yahoo.imapnio.async.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * This class models seq-range from RFC 3501. ABNF from RFC3501 is documented below. MessageNumberSet array allows to model sequence-set.
 *
 * <pre>
 * {@code
 * 2.3.1.  Message Numbers
 *
 *    Messages in IMAP4rev1 are accessed by one of two numbers; the unique
 *    identifier or the message sequence number.
 * ============================================================================
 * ABNF:
 * seq-number      = nz-number / "*"
 *                   ; message sequence number (COPY, FETCH, STORE
 *                   ; commands) or unique identifier (UID COPY,
 *                   ; UID FETCH, UID STORE commands).
 *                   ; * represents the largest number in use.  In
 *                   ; the case of message sequence numbers, it is
 *                   ; the number of messages in a non-empty mailbox.
 *                   ; In the case of unique identifiers, it is the
 *                   ; unique identifier of the last message in the
 *                   ; mailbox or, if the mailbox is empty, the
 *                   ; mailbox's current UIDNEXT value.
 *                   ; The server should respond with a tagged BAD
 *                   ; response to a command that uses a message
 *                   ; sequence number greater than the number of
 *                   ; messages in the selected mailbox.  This
 *                   ; includes "*" if the selected mailbox is empty.
 *
 * seq-range       = seq-number ":" seq-number
 *                   ; two seq-number values and all values between
 *                   ; these two regardless of order.
 *                   ; Example: 2:4 and 4:2 are equivalent and indicate
 *                   ; values 2, 3, and 4.
 *                   ; Example: a unique identifier sequence range of
 *                   ; 3291:* includes the UID of the last message in
 *                   ; the mailbox, even if that value is less than 3291.
 *
 * sequence-set    = (seq-number / seq-range) *("," sequence-set)
 *                   ; set of seq-number values, regardless of order.
 *                   ; Servers MAY coalesce overlaps and/or execute the
 *                   ; sequence in any order.
 *                   ; Example: a message sequence number set of
 *                   ; 2,4:7,9,12:* for a mailbox with 15 messages is
 *                   ; equivalent to 2,4,5,6,7,9,12,13,14,15
 *                   ; Example: a message sequence number set of *:4,5:7
 *                   ; for a mailbox with 10 messages is equivalent to
 *                   ; 10,9,8,7,6,5,4,5,6,7 and MAY be reordered and
 *                   ; overlap coalesced to be 4,5,6,7,8,9,10.
 *
 * nz-number       = digit-nz *DIGIT
 *                   ; Non-zero unsigned 32-bit integer
 *                   ; (0 < n < 4,294,967,296)
 *
 * }
 * </pre>
 */
@SuppressWarnings("hideutilityclassconstructor")
public final class MessageNumberSet {

    /**
     * Enum for external use to denote last message.
     */
    public enum LastMessage {
        /** Last message. */
        LAST_MESSAGE
    }

    /**
     * Message sequence type. Whether an ending message is an absolute number or last message, or just last message.
     */
    private enum SequenceType {

        /** Sequence range ends with an absolute message sequence number, for example: 3:10. */
        ABSOLUTE_END,

        /** Ends with the last message in the mailbox, for example: 3:* . */
        LAST_MESSAGE_END,

        /** Only need the last message, aka: * . */
        LAST_MESSAGE_ONLY
    }

    /** Sequence type. */
    private final SequenceType seqType;

    /** Starting message number, could be message sequence or UID. */
    private final long start;

    /** Ending message number, could be message sequence or UID, if it ends same number as start, it means <seq-number>. */
    private final long end;

    /**
     * Instantiates a {@link MessageNumberSet} with specific numeric start and end. For example, 4:10.
     *
     * @param start starting message sequence or UID sequence
     * @param end ending message sequence or UID sequence
     */
    public MessageNumberSet(final long start, final long end) {
        this(start, end, SequenceType.ABSOLUTE_END);
    }

    /**
     * Instantiates a {@link MessageNumberSet} that starts with given start message number and ends with last message.
     *
     * @param start starting message sequence or UID sequence
     * @param lastMsgFlag flag to denote that it is the last message in mailbox
     */
    public MessageNumberSet(final long start, @Nonnull final LastMessage lastMsgFlag) {
        this(start, start, SequenceType.LAST_MESSAGE_END);
    }

    /**
     * Instantiates a sequence that only returns last message, aka, * .
     *
     * @param lastMsgFlag enum to denote whether it is a last message, should always be
     * @throws ImapAsyncClientException when given isLastMessageOnly is false
     */
    public MessageNumberSet(@Nonnull final LastMessage lastMsgFlag) throws ImapAsyncClientException {
        if (lastMsgFlag != LastMessage.LAST_MESSAGE) {
            throw new ImapAsyncClientException(ImapAsyncClientException.FailureType.INVALID_INPUT);
        }
        this.start = -1;
        this.end = -1;
        this.seqType = SequenceType.LAST_MESSAGE_ONLY;
    }

    /**
     * Instantiates a {@link MessageNumberSet} with start value, end value and SequenceType option.
     *
     * @param start starting message sequence or UID sequence
     * @param end ending message sequence or UID sequence
     * @param seqType ending option either a specific/absolute value, or last message as the end
     */
    private MessageNumberSet(final long start, final long end, @Nonnull final SequenceType seqType) {
        this.start = start < end ? start : end; // ensure smaller at the start
        this.end = start < end ? end : start; // ensure larger at the end
        this.seqType = seqType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hc = 1;
        hc = prime * hc + seqType.name().hashCode();
        hc = prime * hc + Long.hashCode(start);
        hc = prime * hc + Long.hashCode(end);
        return hc;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MessageNumberSet) {
            final MessageNumberSet o = (MessageNumberSet) obj;
            return (o.seqType == seqType) && (start == o.start) && (end == o.end);
        }
        return false;
    }

    /**
     * Converts an array of integers into an array of MessageNumberSet. This is a helper method for callers with int array.
     *
     * @param msgs array of primitive integer data type message number (could be message sequence or UID)
     * @return MessageNumberSet array
     */
    public static MessageNumberSet[] createMessageNumberSets(@Nonnull final int[] msgs) {
        final List<MessageNumberSet> v = new ArrayList<MessageNumberSet>();
        int i = 0;
        while (i < msgs.length) {
            int start = msgs[i];
            // Look for contiguous elements
            int j = i + 1;
            while (j < msgs.length && msgs[j] == msgs[j - 1] + 1) {
                j++;
            }
            int end = msgs[j - 1];
            v.add(new MessageNumberSet(start, end));
            i = j;
        }
        return v.toArray(new MessageNumberSet[v.size()]);
    }

    /**
     * Converts an array of long into array of MessageNumberSet. This is a helper method for callers with long array.
     *
     * @param msgs array of long data type message number (could be message sequence or UID)
     * @return the string generated that conforms to RFC3501 Message Number syntax
     */
    public static MessageNumberSet[] createMessageNumberSets(@Nonnull final long[] msgs) {
        final List<MessageNumberSet> v = new ArrayList<MessageNumberSet>();
        int i = 0;
        while (i < msgs.length) {
            long start = msgs[i];
            // Look for contiguous elements
            int j = i + 1;
            while (j < msgs.length && msgs[j] == msgs[j - 1] + 1) {
                j++;
            }
            long end = msgs[j - 1];
            v.add(new MessageNumberSet(start, end));
            i = j;
        }
        return v.toArray(new MessageNumberSet[v.size()]);
    }

    /**
     * Converts an array of MessageNumberSet into an IMAP RFC3501 sequence-set syntax.
     *
     * @param msgsets array of MessageNumberSet
     * @return the string generated that conforms to IMAP RFC3501 sequence-set syntax
     */
    public static String buildString(@Nullable final MessageNumberSet[] msgsets) {
        if (msgsets == null || msgsets.length == 0) {
            return null;
        }

        // remove duplicates
        final Set<MessageNumberSet> elems = new LinkedHashSet<>(Arrays.asList(msgsets));

        int i = 0; // msgset index
        final StringBuilder s = new StringBuilder();
        final int size = elems.size();
        long start, end;

        for (final MessageNumberSet elem : elems) {
            start = elem.start;
            end = elem.end;

            if (elem.seqType == SequenceType.LAST_MESSAGE_ONLY) {
                s.append('*');
            } else if (elem.seqType == SequenceType.LAST_MESSAGE_END) {
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

    /**
     * Converts an IMAP RFC3501 sequence-set syntax into an array of MessageNumberSet.
     *
     * @param msgNumbers the message numbers string
     * @return the array of MessageNumberSet
     * @throws ImapAsyncClientException will not throw
     */
    public static MessageNumberSet[] buildMessageNumberSets(@Nullable final String msgNumbers) throws ImapAsyncClientException {
        if (msgNumbers == null || msgNumbers.isEmpty()) {
            return null;
        }

        final String[] msgSetStrs = msgNumbers.split(",");
        final MessageNumberSet[] msgSets = new MessageNumberSet[msgSetStrs.length];

        int i = 0; // msgset index
        try {
            for (final String element : msgSetStrs) {
                if (element.contains(":")) {
                    final String[] elements = element.split(":");
                    if (elements.length != 2) {
                        throw new ImapAsyncClientException(ImapAsyncClientException.FailureType.INVALID_INPUT);
                    }
                    if (elements[1].equals("*")) { // Ex: 1:*
                        msgSets[i] = new MessageNumberSet(Long.parseLong(elements[0]), LastMessage.LAST_MESSAGE);
                    } else if (elements[0].equals("*")) { // Ex: *:1
                        msgSets[i] = new MessageNumberSet(Long.parseLong(elements[1]), LastMessage.LAST_MESSAGE);
                    } else { // Ex: 1:2
                        msgSets[i] = new MessageNumberSet(Long.parseLong(elements[0]), Long.parseLong(elements[1]));
                    }
                } else {
                    if (element.equals("*")) { // Ex: *
                        msgSets[i] = new MessageNumberSet(LastMessage.LAST_MESSAGE);
                    } else { // Ex: 1
                        final long num = Long.parseLong(element);
                        msgSets[i] = new MessageNumberSet(num, num);
                    }
                }
                i++;
            }
        } catch (final NumberFormatException ex) {
            throw new ImapAsyncClientException(ImapAsyncClientException.FailureType.INVALID_INPUT);
        }

        return msgSets;
    }
}
