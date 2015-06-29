package com.yahoo.mail.imapnio.client.command;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import com.sun.mail.iap.Protocol;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.util.MailLogger;

/**
 * jgross
 */
public class ProxyProtocol extends Protocol {
    private OutputStream outputStream;

    protected ProxyProtocol() throws IOException {
        super(null, null, new Properties(), false);

        outputStream = new OutputStreamProxy();
    }

    /**
     * Never used but must be implemented.
     *
     * @param host
     * @param port
     * @param props
     * @param prefix
     * @param isSSL
     * @param logger
     * @throws IOException
     * @throws ProtocolException
     */
    private ProxyProtocol(String host, int port, Properties props, String prefix, boolean isSSL, MailLogger logger) throws IOException,
            ProtocolException {
        super(host, port, props, prefix, isSSL, logger);
    }

    public void close() throws IOException {
        outputStream.close();
        outputStream = null;
    }

    /**
     * We wrap our internal outputStream as a DataOutputStream and return that for Argument to write to. The Protocol interface is really bad: this
     * actually has to be a DataOutputStream, not an OutputStream, thus the wrapping.
     *
     * @return
     */
    @Override
    protected OutputStream getOutputStream() {
        return new DataOutputStream(outputStream);
    }

    @Override
    public String toString() {
        return outputStream.toString();
    }
}

class OutputStreamProxy extends OutputStream {
    String result = "";

    @Override
    public void write(int b) throws IOException {
        result = result.concat(String.valueOf((char) b));
    }

    @Override
    public String toString() {
        return result;
    }
}
