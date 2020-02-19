package com.yahoo.imapnio.command;

import java.io.IOException;

import com.sun.mail.iap.ProtocolException;

/**
 * We extend Sun's IMAP Argument class for two reasons: 1. It doesn't have a reasonable constructor by default. 2. We want to make it easy to convert
 * Argument to a string instead of only writing directly to DataOutputStream/Protocol.
 *
 * Sun's IMAP Argument isn't great but its read/write methods are worthwhile.
 *
 * @author kraman
 */
public class Argument extends com.sun.mail.iap.Argument {
    /**
     * Creates a IMAP Argument object.
     */
    public Argument() {
    }

    /**
     * Add string.
     *
     * @param s
     *            argument string
     * @return this Argument object
     */
    public Argument addString(final String s) {
        writeString(s);
        return this;
    }

    /**
     * Add string literal.
     *
     * @param s
     *            argument literal
     * @return this Argument object
     */
    public Argument addLiteral(final String s) {
        writeAtom(s);
        return this;
    }

    /**
     * Convert arguments to space-separated list. Because of the number of private classes and methods in Argument, we cannot simply copy the logic
     * there - it (sadly) makes more sense to proxy Protocol.
     *
     * @return string version of the argument
     */
    @Override
    public String toString() {
        String result = "";

        ProxyProtocol proxyProtocol = null;
        try {
            proxyProtocol = new ProxyProtocol();
            write(proxyProtocol);
            result = proxyProtocol.toString();
        } catch (final IOException | ProtocolException e) {
            e.printStackTrace();
        } finally {
            try {
                if (proxyProtocol != null) {
                    proxyProtocol.close();
                }
            } catch (final IOException e) {
                // TODO: remove this code e.printStackTrace();
            }
        }

        return result;
    }
}
