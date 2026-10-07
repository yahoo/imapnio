package com.yahoo.imapnio.async.request;

import javax.annotation.Nonnull;
import javax.mail.Flags;

import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException.FailureType;

/**
 * This class checks the imap command arguments that are written to the command line bare, and builds the flag list.
 *
 * <p>
 * An {@code atom} cannot hold any control character: {@code atom-specials} names {@code CTL} outright, and productions such as
 * {@code flag-keyword}, {@code capability}, {@code fetch-att}, {@code status-att} and {@code sequence-set} are atoms with no quoted or
 * literal alternative to fall back on, so {@link #validateAtom} refuses every one of them.
 * </p>
 *
 * <p>
 * Writing an {@code astring}, which may take the atom, quoted or literal form, belongs to {@link ImapCommandLineBuilder} instead, since
 * choosing the literal form can split the command line.
 * </p>
 */
public class ImapArgumentFormatter {

    /** Highest code point that is an ASCII control character, ie. the last one before SPACE. */
    private static final char LAST_CONTROL_CHAR = 0x1F;

    /** The DEL control character, the one control character above SPACE. */
    private static final char DEL_CHAR = 0x7F;

    /** The atom-specials other than CTL, which is checked by code point rather than by lookup. */
    private static final String ATOM_SPECIALS = "(){ %*\"\\]";

    /** Literal. */
    private static final String SEEN = "\\Seen";

    /** Literal. */
    private static final String RECENT = "\\Recent";

    /** Literal. */
    private static final String FLAGGED = "\\Flagged";

    /** Literal. */
    private static final String DRAFT = "\\Draft";

    /** Literal. */
    private static final String DELETED = "\\Deleted";

    /** Literal. */
    private static final String ANSWERED = "\\Answered";

    /**
     * Ensures the given value carries no control character.
     *
     * <p>
     * This is the floor every value written bare into a command line has to clear, whatever its production: a CR or LF would end the line,
     * and NUL and SOH separate the fields inside the SASL payloads. It says nothing about the shape of the value, so a production with a
     * shape of its own is checked by one of the methods below instead.
     * </p>
     *
     * @param src the value to check
     * @param name the name of the argument, used in the failure detail
     * @return the value, so callers can check inline
     * @throws ImapAsyncClientException when the value carries a control character
     */
    String validateNoControlChars(@Nonnull final String src, @Nonnull final String name) throws ImapAsyncClientException {
        for (int i = 0; i < src.length(); i++) {
            final char b = src.charAt(i);
            if (b <= LAST_CONTROL_CHAR || b == DEL_CHAR) {
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT, new StringBuilder(name).append(' ').append(describe(b, i)).toString());
            }
        }
        return src;
    }

    /**
     * Ensures the given value can be written as an {@code atom}.
     *
     * <p>
     * {@code atom-specials} is {@code "(" / ")" / "{" / SP / CTL / list-wildcards / quoted-specials / resp-specials}, so besides the control
     * characters an atom cannot hold a parenthesis, a left brace, a space, {@code %}, {@code *}, a double quote, a backslash or {@code ]}.
     * Productions such as {@code flag-keyword}, {@code capability}, {@code status-att} and {@code return-option} are atoms with no quoted or
     * literal alternative, so a value that is not one cannot be sent at all; letting it through would let a caller add a token of their own to
     * the command.
     * </p>
     *
     * @param src the value to check
     * @param name the name of the argument, used in the failure detail
     * @return the value, so callers can check inline
     * @throws ImapAsyncClientException when the value cannot be written as an atom
     */
    String validateAtom(@Nonnull final String src, @Nonnull final String name) throws ImapAsyncClientException {
        validateNoControlChars(src, name);
        for (int i = 0; i < src.length(); i++) {
            final char b = src.charAt(i);
            if (ATOM_SPECIALS.indexOf(b) >= 0) {
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT, new StringBuilder(name).append(" argument contains '").append(b)
                        .append("' at index ").append(i).append(", which atom-specials keeps out of an atom.").toString());
            }
        }
        return src;
    }

    /**
     * Ensures the given value can be written as a {@code sequence-set} or a {@code uid-set}.
     *
     * <p>
     * Both are built from numbers, {@code :} for a range, {@code ,} between ranges and {@code *} for the largest number in use. They are not
     * atoms, since {@code *} is a list-wildcard that {@code atom-specials} keeps out of one, so they get a rule of their own rather than being
     * held to the atom rule and failing on a perfectly good {@code 1:*}.
     * </p>
     *
     * @param src the value to check
     * @param name the name of the argument, used in the failure detail
     * @return the value, so callers can check inline
     * @throws ImapAsyncClientException when the value holds anything else
     */
    String validateSequenceSet(@Nonnull final String src, @Nonnull final String name) throws ImapAsyncClientException {
        for (int i = 0; i < src.length(); i++) {
            final char b = src.charAt(i);
            if ((b < '0' || b > '9') && b != ':' && b != ',' && b != '*') {
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT, new StringBuilder(name).append(" argument contains '").append(b)
                        .append("' at index ").append(i).append("; a sequence set holds only digits, ':', ',' and '*'.").toString());
            }
        }
        return src;
    }

    /**
     * Ensures the given value can be written as the body of a {@code fetch-att} list.
     *
     * <p>
     * A fetch-att is not an atom either: {@code BODY[HEADER.FIELDS (DATE FROM)]} holds spaces, brackets and parentheses quite legitimately.
     * What it cannot do is close a group it never opened, which is how a value like {@code FLAGS) (BODY} escapes the list the caller was given
     * and adds arguments of its own to the command. The delimiters are therefore required to balance.
     * </p>
     *
     * @param src the value to check
     * @param name the name of the argument, used in the failure detail
     * @return the value, so callers can check inline
     * @throws ImapAsyncClientException when a control character is present or the delimiters do not balance
     */
    String validateFetchAtt(@Nonnull final String src, @Nonnull final String name) throws ImapAsyncClientException {
        validateNoControlChars(src, name);
        int parens = 0;
        int brackets = 0;
        for (int i = 0; i < src.length(); i++) {
            final char b = src.charAt(i);
            if (b == '(') {
                parens++;
            } else if (b == ')') {
                parens--;
            } else if (b == '[') {
                brackets++;
            } else if (b == ']') {
                brackets--;
            }
            if (parens < 0 || brackets < 0) {
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT, new StringBuilder(name).append(" argument closes at index ")
                        .append(i).append(" a group it did not open.").toString());
            }
        }
        if (parens != 0 || brackets != 0) {
            throw new ImapAsyncClientException(FailureType.INVALID_INPUT,
                    new StringBuilder(name).append(" argument leaves a group open.").toString());
        }
        return src;
    }

    /**
     * Describes a rejected character by code point and position.
     *
     * <p>
     * The character itself is named, but the value it came from is deliberately left out: this text is carried in the exception message and so
     * reaches logs, and a rejected argument may be a password.
     * </p>
     *
     * @param b the offending character
     * @param i the index it was found at
     * @return the description
     */
    private static String describe(final char b, final int i) {
        return new StringBuilder("argument contains an illegal control character (0x").append(Integer.toHexString(b)).append(") at index ")
                .append(i).append('.').toString();
    }

    /**
     * Creates an IMAP flag_list from the given Flags object.
     *
     * @param flags the flags
     * @return the flag list string
     * @throws ImapAsyncClientException when a user flag is not a valid {@code flag-keyword}
     */
    String buildFlagString(@Nonnull final Flags flags) throws ImapAsyncClientException {
        final StringBuilder sb = new StringBuilder();
        sb.append(ImapClientConstants.L_PAREN); // start of flag_list

        Flags.Flag[] sf = flags.getSystemFlags(); // get the system flags
        boolean first = true;
        for (int i = 0; i < sf.length; i++) {
            String s;
            Flags.Flag f = sf[i];
            if (f == Flags.Flag.ANSWERED) {
                s = ANSWERED;
            } else if (f == Flags.Flag.DELETED) {
                s = DELETED;
            } else if (f == Flags.Flag.DRAFT) {
                s = DRAFT;
            } else if (f == Flags.Flag.FLAGGED) {
                s = FLAGGED;
            } else if (f == Flags.Flag.RECENT) {
                s = RECENT;
            } else if (f == Flags.Flag.SEEN) {
                s = SEEN;
            } else {
                continue; // skip it
            }
            if (first) {
                first = false;
            } else {
                sb.append(ImapClientConstants.SPACE);
            }
            sb.append(s);
        }

        String[] uf = flags.getUserFlags(); // get the user flag strings
        for (int i = 0; i < uf.length; i++) {
            if (first) {
                first = false;
            } else {
                sb.append(ImapClientConstants.SPACE);
            }
            // flag-keyword is an atom, so a user flag has no form able to carry a control character
            sb.append(validateAtom(uf[i], "user flag"));
        }

        sb.append(ImapClientConstants.R_PAREN); // terminate flag_list
        return sb.toString();
    }
}
