package org.apache.imapnioclient.command;

import java.io.IOException;

import com.sun.mail.iap.ProtocolException;


/**
 * We extend Sun's IMAP Argument class for two reasons: 1. It doesn't have a reasonable constructor by default. 2. We want to make it easy to convert
 * Argument to a string instead of only writing directly to DataOutputStream/Protocol.
 *
 * Sun's IMAP Argument isn't great but its read/write methods are worthwhile.
 *
 * @author jgross
 */
public class Argument extends com.sun.mail.iap.Argument {
    public Argument() {
    }

    /**
     * Add string.
     */
    public Argument addString(String s) {
        writeString(s);
        return this;
    }

    /**
     * Add string literal.
     */
    public Argument addLiteral(String s) {
        writeAtom(s);
        return this;
    }

    /**
     * Convert arguments to space-separated list. Because of the number of private classes and methods in Argument, we cannot simply copy the logic
     * there - it (sadly) makes more sense to proxy Protocol.
     */
    @Override
    public String toString() {
        String result = "";

        ProxyProtocol proxyProtocol = null;
        try {
            proxyProtocol = new ProxyProtocol();
            write(proxyProtocol);
            result = proxyProtocol.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } finally {
            try {
                if (proxyProtocol != null) {
                    proxyProtocol.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }
}
