package com.lafaspot.imapnio.async.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.Folder;

import com.lafaspot.imapnio.async.data.Capability;
import com.lafaspot.imapnio.async.data.ListInfoList;
import com.lafaspot.imapnio.async.exception.ImapAsyncClientException;
import com.lafaspot.imapnio.async.exception.ImapAsyncClientException.FailureType;
import com.sun.mail.iap.ParsingException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.CopyUID;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.ListInfo;
import com.sun.mail.imap.protocol.MailboxInfo;
import com.sun.mail.imap.protocol.UIDSet;


/**
 * This class parses the imap response to the proper Imap object that sun supports.
 */
public class ImapResponseMapper {

    /** APPENDUID keyword. */
    private static final String APPENDUID = "APPENDUID";

    /** EQUAL sign. */
    private static final String EQUAL = "=";

    /** [ char. */
    private static final char L_BRACKET = '[';

    /** ] char. */
    private static final char R_BRACKET = ']';

    /** Inner class instance parser. */
    private ImapResponseParser parser;

    /**
     * Initializes a @{code ImapResponseMapper} object.
     */
    public ImapResponseMapper() {
        parser = new ImapResponseParser();
    }

    /**
     * Method to deserialize IMAPResponse content from given IMAPResponse content String.
     *
     * @param <T> the object to serialize to
     * @param content list of IMAPResponse obtained from server.
     * @param valueType class name that this api will convert to.
     * @return the serialized object
     * @throws ParsingException if underlying input contains invalid content of type for the returned type
     * @throws ImapAsyncClientException when target class to covert to is not supported
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(@Nonnull final IMAPResponse[] content, @Nonnull final Class<T> valueType)
            throws ImapAsyncClientException, ParsingException {
        if (valueType == Capability.class) {
            return (T) parser.parseToCapabilities(content);
        }
        if (valueType == AppendUID.class) {
            return (T) parser.parseToAppendUid(content);
        }
        if (valueType == CopyUID.class) {
            return (T) parser.parseToCopyUid(content);
        }
        if (valueType == MailboxInfo.class) {
            return (T) parser.parseToMailboxInfo(content);
        }
        if (valueType == ListInfoList.class) {
            return (T) parser.parseToListInfoList(content);
        }

        throw new ImapAsyncClientException(FailureType.INVALID_INPUT);
    }

    /**
     * Inner class to perform the parsing of IMAPResponse to various objects.
     */
    private class ImapResponseParser {
        /**
         * Parses the capabilities from a CAPABILITY response or from a CAPABILITY response code attached to (e.g.) an OK response.
         *
         * @param rs the CAPABILITY responses
         * @return a map that has the key (token) and all its values (after = sign), if there is no equal sign, value is same as key
         * @throws ImapAsyncClientException when input IMAPResponse array is not valid
         */
        @Nonnull
        public Capability parseToCapabilities(@Nonnull final IMAPResponse[] rs) throws ImapAsyncClientException {
            String s;
            final Map<String, List<String>> capas = new HashMap<String, List<String>>();
            if (rs.length < 1) {
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT);
            }

            final IMAPResponse r = rs[0];
            while ((s = r.readAtom()) != null) {
                if (s.length() == 0) {
                    if (r.peekByte() == (byte) R_BRACKET) {
                        break;
                    }
                    // Probably found something here that's not an atom. Rather than loop forever or fail completely, we'll try to skip this bogus
                    // capability. This is known to happen with: Netscape Messaging Server 4.03 (built Apr 27 1999) that returns:
                    // * CAPABILITY * CAPABILITY IMAP4 IMAP4rev1 ...
                    // The "*" in the middle of the capability list causes us to loop forever here.
                    r.skipToken();
                } else {
                    final String[] tokens = s.split(EQUAL);
                    final String key = tokens[0];
                    final String value = (tokens.length > 1) ? tokens[1] : null;
                    final String upperCase = key.toUpperCase(Locale.ENGLISH);
                    List<String> values = capas.get(upperCase);
                    if (values == null) {
                        values = new ArrayList<>();
                        capas.put(upperCase, values);
                    }
                    // AUTH key allows more than one pair(ex:AUTH=XOAUTH2 AUTH=PLAIN), parsing value out to List, otherwise add key to list
                    if (value != null) {
                        values.add(value);
                    }
                }
            }
            // making the value list immutable
            for (final Map.Entry<String, List<String>> entry : capas.entrySet()) {
                entry.setValue(Collections.unmodifiableList(entry.getValue()));
            }
            return new Capability(capas);
        }

        /**
         * Parses APPEND response to a AppendUID instance.
         *
         * @param rs the APPEND responses
         * @return AppendUID instance
         * @throws ImapAsyncClientException when input value is not valid
         */
        @Nullable
        public AppendUID parseToAppendUid(@Nonnull final IMAPResponse[] rs) throws ImapAsyncClientException {
            if (rs.length < 1) {
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT);
            }
            final IMAPResponse r = rs[0];
            if (!r.isOK()) {
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT);
            }
            byte b;
            while ((b = r.readByte()) > 0 && b != (byte) L_BRACKET) {
                // eat chars till [
            }

            if (b == 0) {
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT);
            }
            final String s = r.readAtom();
            if (!s.equalsIgnoreCase(APPENDUID)) {
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT);
            }
            final long uidvalidity = r.readLong();
            final long uid = r.readLong();
            return new AppendUID(uidvalidity, uid);
        }

        /**
         * Parses COPY or MOVE command responses to a CopyUID instance.
         *
         * @param rr the COPY responses
         * @return COPYUID instance built from copy command responses
         * @throws ImapAsyncClientException when input value is not valid
         */
        @Nullable
        public CopyUID parseToCopyUid(@Nonnull final IMAPResponse[] rr) throws ImapAsyncClientException {
            // For copy response, it is at the last response, for move command response, it is the first response
            for (int i = rr.length - 1; i >= 0; i--) {
                final Response r = rr[i];
                if (r == null || !r.isOK()) {
                    continue;
                }
                byte b;
                while ((b = r.readByte()) > 0 && b != (byte) L_BRACKET) {
                    // eat chars till [
                }

                if (b == 0) {
                    continue;
                }
                final String s = r.readAtom();
                if (!s.equalsIgnoreCase("COPYUID")) { // expunge response from MOVE, for ex: 2 EXPUNGE
                    continue;
                }
                final long uidvalidity = r.readLong();
                final String src = r.readAtom();
                final String dst = r.readAtom();
                return new CopyUID(uidvalidity, UIDSet.parseUIDSets(src), UIDSet.parseUIDSets(dst));
            }
            throw new ImapAsyncClientException(FailureType.INVALID_INPUT); // when rr length is 0
        }

        /**
         * Parses the SELECT or EXAMINE responses to a MailboxInfo instance.
         *
         * @param rr the list of responses from SELECT or EXAMINE, this input r array should contain the tagged/final one
         * @return MailboxInfo instance
         * @throws ParsingException when encountering parsing exception
         * @throws ImapAsyncClientException when input is invalid
         */
        public MailboxInfo parseToMailboxInfo(@Nonnull final IMAPResponse[] rr) throws ParsingException, ImapAsyncClientException {
            if (rr.length < 1) {
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT);
            }
            final MailboxInfo minfo = new MailboxInfo(rr);
            final Response lastResp = rr[rr.length - 1]; // final response

            if (lastResp.isTagged() && lastResp.isOK()) { // command succesful
                if (lastResp.toString().indexOf("READ-ONLY") != -1) {
                    minfo.mode = Folder.READ_ONLY;
                } else {
                    minfo.mode = Folder.READ_WRITE;
                }
            }
            return minfo;
        }

        /**
         * Parses the LIST or LSUB responses to a @{code ListInfo} list. List responses example:
         *
         * <pre>
         * * LIST () "/" INBOX
         * * LIST (\NoSelect) "/" "Public mailboxes"
         * </pre>
         *
         * @param r the list of responses from SELECT, the input responses array should contain the tagged/final one
         * @return list of ListInfo objects
         * @throws ParsingException when encountering parsing exception
         * @throws ImapAsyncClientException when input value is not valid
         */
        @Nullable
        public ListInfoList parseToListInfoList(@Nonnull final IMAPResponse[] r) throws ParsingException, ImapAsyncClientException {
            final Response response = r[r.length - 1];
            if (!response.isOK()) {
                throw new ImapAsyncClientException(FailureType.INVALID_INPUT);
            }

            // command succesful reaching here
            final List<ListInfo> v = new ArrayList<ListInfo>();
            for (int i = 0, len = r.length - 1; i < len; i++) {
                final IMAPResponse ir = r[i];
                v.add(new ListInfo(ir));
            }

            // could be an empty list if the search criteria ends up no result. Ex:
            // a002 LIST "" "*t3*"
            // a002 OK LIST completed
            return new ListInfoList(v);
        }
    }
}
