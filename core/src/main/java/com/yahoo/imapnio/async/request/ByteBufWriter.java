package com.yahoo.imapnio.async.request;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import javax.annotation.Nonnull;

import com.sun.mail.iap.Protocol;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.util.MailLogger;

import io.netty.buffer.ByteBuf;

/**
 * This class allows writing the data from argument to ByteBuf directly.
 */
final class ByteBufWriter extends Protocol {

    /** Symbol to indicate continue response. */
    private static final String CONTINUE_SYMBOL = "+";

    /** The OutputStream used by ByteBufWriter. */
    private OutputStream outputStream;

    /** Flag to indicate whether literal plus is enabled. */
    private boolean isLiteralPlus;

    /**
     * Creates a ByteBufWriter object.
     *
     * @param buf ByteBuf instance
     * @param isLiteralPlus Flag to indicate whether literal plus is enabled
     * @throws IOException on failure
     */
    ByteBufWriter(@Nonnull final ByteBuf buf, final boolean isLiteralPlus) throws IOException {
        super(null, null, new Properties(), false);
        this.outputStream = new ByteBufOutputStream(buf);
        this.isLiteralPlus = isLiteralPlus;
    }

    /**
     * Never used but must be implemented.
     *
     * @param host IMAP server host
     * @param port IMAP server port
     * @param props properties
     * @param prefix prefix to prepend property keys
     * @param isSSL is this an SSL connection
     * @param logger logger
     * @throws IOException on network failure
     * @throws ProtocolException on IMAP protocol errors
     */
    private ByteBufWriter(final String host, final int port, final Properties props, final String prefix, final boolean isSSL,
            final MailLogger logger) throws IOException, ProtocolException {
        super(host, port, props, prefix, isSSL, logger);
    }

    /**
     * Returns a continuation response in order to avoid {@link com.sun.mail.iap.Argument} blocking on literal method to wait for server continuation.
     *
     * @return a continuation response
     * @throws IOException on network failure
     * @throws ProtocolException on IMAP protocol errors
     */
    @Override
    public Response readResponse() throws IOException, ProtocolException {
        return new IMAPResponse(CONTINUE_SYMBOL);
    }

    @Override
    protected OutputStream getOutputStream() {
        return new DataOutputStream(outputStream);
    }

    @Override
    protected synchronized boolean supportsNonSyncLiterals() {
        return isLiteralPlus;
    }
}

/**
 * ByteBufOutputStream that allows writing to ByteBuf directly.
 */
class ByteBufOutputStream extends OutputStream {
    /** Internal buffer. */
    private ByteBuf buf;

    /**
     * Instantiates ByteBufOutputStream instance with ByteBuf.
     *
     * @param buf ByteBuf instance
     */
    ByteBufOutputStream(@Nonnull final ByteBuf buf) {
        this.buf = buf;
    }

    @Override
    public void write(final int b) {
        buf.writeByte(b);
    }
}
