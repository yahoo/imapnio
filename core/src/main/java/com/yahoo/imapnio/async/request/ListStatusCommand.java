package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines IMAP LIST-STATUS Command, RFC5819. This extension is part of LIST-EXTENDED extension, RFC5258.
 *
 * This command is specialized to support LIST-STATUS. It does not support list-select-opts, nor other RETURN options.
 *
 * @see <a href="https://tools.ietf.org/html/rfc5819#page-2" target="_blank">LIST-STATUS extension</a>
 * @see <a href="https://tools.ietf.org/html/rfc5258#page-19" target="_blank">LIST-EXTENDED extension</a>
 *
 *      <blockquote>
 *
 *      <pre>
 *
 * list = "LIST" [SP list-select-opts] SP mailbox SP mbox-or-pat
 *             [SP list-return-opts]
 *
 * return-option =/ status-option
 *
 * status-option = "STATUS" SP "(" status-att *(SP status-att) ")"
 *                 ;; This ABNF production complies with
 *                 ;; &lt;option-extension&gt; syntax.
 *
 *      </pre>
 *
 *      </blockquote>
 */
public class ListStatusCommand extends ImapRequestAdapter {

    /** Byte array for CR and LF, keeping the array local so it cannot be modified by others. */
    private static final byte[] CRLF_B = { '\r', '\n' };

    /** Double right parenthesis. */
    private static final byte[] DOUBLE_RP_B = { ')', ')' };

    /** List command name. */
    private static final String LIST_SP = "LIST ";

    /** Byte array for above command. */
    private static final byte[] LIST_SP_B = LIST_SP.getBytes(StandardCharsets.US_ASCII);

    /** RETURN literal and Left Parenthesis. */
    private static final String SP_RETURN_LP = " RETURN (";

    /** Byte array for above. */
    private static final byte[] SP_RETURN_LP_B = SP_RETURN_LP.getBytes(StandardCharsets.US_ASCII);

    /** RETURN literal and Left Parenthesis. */
    private static final String SP_STATUS_LP = "STATUS (";

    /** Byte array for above. */
    private static final byte[] SP_STATUS_LP_B = SP_STATUS_LP.getBytes(StandardCharsets.US_ASCII);

    /** Always enclose with double quotes of each pattern. */
    private static final boolean FORCE_DOUBLE_QUOTES = true;

    /** An empty String array to indicate no other returned option required. */
    private static final String[] NO_OTHER_RETURNED_OPTIONS = {};

    /** Reference name. */
    private String ref;

    /** Multiple mailbox patterns, this is supported in RFC5258. */
    private String[] multiPatterns;

    /** Other returned options. */
    private String[] otherReturnOptions;

    /** Status data item names. */
    private String[] items;

    /**
     * Initializes a {@link ListStatusCommand} with ref, multi-mailbox and list of status items. Here multi-mailbox is allowed.
     *
     * @param ref the reference string
     * @param multiPatterns list of pattern specified in RFC5258, LIST-EXTEND capability. For ex, INBOX or Sent/%
     * @param items list of items for status to be returned.
     * @throws ImapAsyncClientException if given input is invalid
     */
    public ListStatusCommand(@Nonnull final String ref, @Nonnull final String[] multiPatterns, @Nonnull final String[] items)
            throws ImapAsyncClientException {
        this(ref, multiPatterns, NO_OTHER_RETURNED_OPTIONS, items);
    }

    /**
     * Initializes a {@link ListStatusCommand} with ref, multi-mailbox, other returned options and list of status items.
     *
     * @param ref the reference string
     * @param multiPatterns list of pattern specified in RFC5258, LIST-EXTEND capability. For ex, INBOX or Sent/%
     * @param otherReturnOptions allow callers to pass other return options, beside STATUS
     * @param items list of items for status to be returned.
     * @throws ImapAsyncClientException if given input is invalid
     */
    public ListStatusCommand(@Nonnull final String ref, @Nonnull final String[] multiPatterns, @Nonnull final String[] otherReturnOptions,
            @Nonnull final String[] items) throws ImapAsyncClientException {
        this.ref = ref;
        this.multiPatterns = multiPatterns;
        this.otherReturnOptions = otherReturnOptions;
        this.items = items;
        if (multiPatterns.length == 0 || items.length == 0) {
            throw new ImapAsyncClientException(ImapAsyncClientException.FailureType.INVALID_INPUT);
        }
    }

    @Override
    public void cleanup() {
        this.ref = null;
        this.multiPatterns = null;
        this.otherReturnOptions = null;
        this.items = null;
    }

    @Override
    public ByteBuf getCommandLineBytes() throws ImapAsyncClientException {

        final String ref64 = BASE64MailboxEncoder.encode(ref);

        int listOfMboxSizeOrPatternSize = 0;
        // estimate size of the patterns
        for (int i = 0; i < multiPatterns.length; i++) {
            listOfMboxSizeOrPatternSize += 2 * multiPatterns[i].length();
        }

        final int len = 2 * ref64.length() + listOfMboxSizeOrPatternSize + ImapClientConstants.PAD_LEN;
        final ByteBuf bytebuf = Unpooled.buffer(len);
        // LIST
        bytebuf.writeBytes(LIST_SP_B);

        // ref
        final ImapArgumentFormatter formatter = new ImapArgumentFormatter();
        formatter.formatArgument(ref64, bytebuf, false);
        bytebuf.writeByte(ImapClientConstants.SPACE);

        // mbox-or-pat
        bytebuf.writeByte(ImapClientConstants.L_PAREN);
        for (int i = 0; i < multiPatterns.length; i++) {
            // if multi patterns, we double quote each one, for 1 pattern, it will base on the content, not "forced"
            formatter.formatArgument(BASE64MailboxEncoder.encode(multiPatterns[i]), bytebuf, FORCE_DOUBLE_QUOTES);

            if (i != multiPatterns.length - 1) {
                bytebuf.writeByte(ImapClientConstants.SPACE);
            }
        }
        bytebuf.writeByte(ImapClientConstants.R_PAREN);

        // return keyword
        bytebuf.writeBytes(SP_RETURN_LP_B); // " RETURN ("

        // other returned options than STATUS
        for (int i = 0; i < otherReturnOptions.length; i++) {
            formatter.formatArgument(otherReturnOptions[i], bytebuf, false);
            bytebuf.writeByte(ImapClientConstants.SPACE);
        }

        // status option
        bytebuf.writeBytes(SP_STATUS_LP_B); // "STATUS ("
        for (int i = 0; i < items.length; i++) {
            formatter.formatArgument(items[i], bytebuf, false);

            if (i < items.length - 1) { // do not add space for last item
                bytebuf.writeByte(ImapClientConstants.SPACE);
            }
        }
        bytebuf.writeBytes(DOUBLE_RP_B); // "))"

        bytebuf.writeBytes(CRLF_B);
        return bytebuf;
    }

    @Override
    public ImapRFCSupportedCommandType getCommandType() {
        return ImapRFCSupportedCommandType.LIST_STATUS;
    }
}
