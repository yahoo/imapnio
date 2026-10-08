package com.yahoo.imapnio.async.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.time.Clock;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.yahoo.imapnio.async.client.ImapAsyncSession.DebugMode;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Checks that the {@link SSLEngine} {@link ImapAsyncClient} puts in the pipeline matches the server certificate against the host the client asked
 * for, not only against the trust store.
 *
 * <p>
 * The Netty connection is mocked. The client engine taken from the mocked pipeline then runs a TLS handshake in memory against a server engine, so
 * no socket is opened. Both keystores hold a self-signed certificate that the client trusts. tls-localhost.p12 is issued for localhost,
 * tls-mismatch.p12 for mismatch.example.com. They were made with:
 *
 * <pre>
 * keytool -J-Dkeystore.pkcs12.legacy -genkeypair -keystore tls-localhost.p12 -storetype PKCS12 -storepass changeit -keypass changeit \
 *     -alias server -keyalg RSA -keysize 2048 -validity 36500 -dname "CN=localhost" -ext "SAN=DNS:localhost"
 * </pre>
 */
public class ImapAsyncClientTlsTest {

    /** Keystore whose certificate is issued for localhost. */
    private static final String LOCALHOST_KEYSTORE = "/tls-localhost.p12";

    /** Keystore whose certificate is issued for mismatch.example.com. */
    private static final String MISMATCH_KEYSTORE = "/tls-mismatch.p12";

    /** Password of both test keystores. */
    private static final char[] PASSWORD = "changeit".toCharArray();

    /** Address the localhost certificate does not name. */
    private static final String ADDRESS = "127.0.0.1";

    /** Size of each in-memory network and application buffer, enough for a whole handshake flight. */
    private static final int BUFFER_SIZE = 64 * 1024;

    /** Upper bound on handshake steps, so that a stuck handshake fails the test instead of hanging it. */
    private static final int MAX_HANDSHAKE_STEPS = 100;

    /** Client context that trusts both test certificates. */
    private SSLContext clientContext;

    /** Server context presenting the localhost certificate. */
    private SSLContext localhostServerContext;

    /** Server context presenting the mismatch.example.com certificate. */
    private SSLContext mismatchServerContext;

    /**
     * Loads the test certificates.
     *
     * @throws GeneralSecurityException will not throw
     * @throws IOException will not throw
     */
    @BeforeClass
    public void beforeClass() throws GeneralSecurityException, IOException {
        final KeyStore localhost = loadKeyStore(LOCALHOST_KEYSTORE);
        final KeyStore mismatch = loadKeyStore(MISMATCH_KEYSTORE);

        final KeyStore trust = KeyStore.getInstance(KeyStore.getDefaultType());
        trust.load(null, null);
        trust.setCertificateEntry("localhost", localhost.getCertificate("server"));
        trust.setCertificateEntry("mismatch", mismatch.getCertificate("server"));
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trust);
        clientContext = SSLContext.getInstance("TLS");
        clientContext.init(null, tmf.getTrustManagers(), null);

        localhostServerContext = serverContext(localhost);
        mismatchServerContext = serverContext(mismatch);
    }

    /**
     * A trusted certificate issued for the host the client connects to is accepted.
     *
     * @throws URISyntaxException will not throw
     * @throws SSLException will not throw
     */
    @Test
    public void testMatchingCertificateConnects() throws URISyntaxException, SSLException {
        final SSLEngine client = createClientEngine("localhost", null, new ImapAsyncSessionConfig());
        handshake(client, localhostServerContext);
    }

    /**
     * A trusted certificate issued for another host is refused.
     *
     * @throws URISyntaxException will not throw
     */
    @Test
    public void testMismatchedCertificateFails() throws URISyntaxException {
        final SSLEngine client = createClientEngine("localhost", null, new ImapAsyncSessionConfig());
        assertHandshakeFails(client, mismatchServerContext);
    }

    /**
     * A trusted certificate issued for another host is refused when SNI names are given too.
     *
     * @throws URISyntaxException will not throw
     */
    @Test
    public void testMismatchedCertificateWithSniFails() throws URISyntaxException {
        final SSLEngine client = createClientEngine("localhost", Collections.singletonList("localhost"), new ImapAsyncSessionConfig());
        assertHandshakeFails(client, mismatchServerContext);
    }

    /**
     * With SNI, the certificate is matched against the SNI host name, so a client may connect by address and still verify the server's name.
     *
     * @throws URISyntaxException will not throw
     * @throws SSLException will not throw
     */
    @Test
    public void testSniNameIsVerifiedWhenConnectingByAddress() throws URISyntaxException, SSLException {
        final SSLEngine client = createClientEngine(ADDRESS, Collections.singletonList("localhost"), new ImapAsyncSessionConfig());
        handshake(client, localhostServerContext);
    }

    /**
     * Without SNI, connecting by address checks the certificate against that address.
     *
     * @throws URISyntaxException will not throw
     */
    @Test
    public void testConnectingByAddressWithoutSniFails() throws URISyntaxException {
        final SSLEngine client = createClientEngine(ADDRESS, null, new ImapAsyncSessionConfig());
        assertHandshakeFails(client, localhostServerContext);
    }

    /**
     * Without a caller SSLContext, the engine built by Netty verifies the host name by default.
     *
     * @throws URISyntaxException will not throw
     */
    @Test
    public void testDefaultContextVerifiesHostname() throws URISyntaxException {
        final SSLEngine client = createClientEngine("localhost", null, new ImapAsyncSessionConfig(), null);
        Assert.assertEquals(client.getSSLParameters().getEndpointIdentificationAlgorithm(), "HTTPS", "Host name should be verified.");
    }

    /**
     * Runs createSession with the client context that trusts both test certificates.
     *
     * @param host host name or address to put in the server URI
     * @param sniNames SNI names, or null
     * @param config session configuration
     * @return the client engine
     * @throws URISyntaxException will not throw
     */
    private SSLEngine createClientEngine(final String host, final List<String> sniNames, final ImapAsyncSessionConfig config)
            throws URISyntaxException {
        return createClientEngine(host, sniNames, config, clientContext);
    }

    /**
     * Runs createSession over a mocked connection that succeeds at once and returns the engine of the {@link SslHandler} it adds.
     *
     * @param host host name or address to put in the server URI
     * @param sniNames SNI names, or null
     * @param config session configuration
     * @param sslContext caller SSLContext, or null for the one the client builds
     * @return the client engine
     * @throws URISyntaxException will not throw
     */
    private SSLEngine createClientEngine(final String host, final List<String> sniNames, final ImapAsyncSessionConfig config,
            final SSLContext sslContext) throws URISyntaxException {
        final Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        final ChannelFuture connectFuture = Mockito.mock(ChannelFuture.class);
        final Channel channel = Mockito.mock(Channel.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(bootstrap.connect(Mockito.anyString(), Mockito.anyInt())).thenReturn(connectFuture);
        Mockito.when(connectFuture.isSuccess()).thenReturn(true);
        Mockito.when(connectFuture.channel()).thenReturn(channel);
        Mockito.when(channel.pipeline()).thenReturn(pipeline);
        Mockito.when(connectFuture.addListener(Mockito.any(GenericFutureListener.class))).thenAnswer(new Answer<ChannelFuture>() {
            @Override
            public ChannelFuture answer(final InvocationOnMock invocation) throws Throwable {
                ((GenericFutureListener) invocation.getArguments()[0]).operationComplete(connectFuture);
                return connectFuture;
            }
        });

        final ImapAsyncClient client = new ImapAsyncClient(Clock.systemUTC(), bootstrap, Mockito.mock(EventLoopGroup.class),
                Mockito.mock(Logger.class));
        client.createSession(new URI("imaps", null, host, 993, null, null, null), config, null, sniNames, DebugMode.DEBUG_OFF, "tlsTest",
                sslContext);

        final ArgumentCaptor<ChannelHandler> handlerCaptor = ArgumentCaptor.forClass(ChannelHandler.class);
        Mockito.verify(pipeline).addFirst(Mockito.eq(ImapAsyncClient.SSL_HANDLER), handlerCaptor.capture());
        return ((SslHandler) handlerCaptor.getValue()).engine();
    }

    /**
     * Asserts that the handshake fails because the client refuses the server certificate.
     *
     * @param client client engine
     * @param serverContext context of the server to handshake with
     */
    private void assertHandshakeFails(final SSLEngine client, final SSLContext serverContext) {
        try {
            handshake(client, serverContext);
            Assert.fail("Handshake should fail.");
        } catch (final SSLHandshakeException e) {
            Assert.assertTrue(e.getCause() instanceof CertificateException, "Unexpected cause: " + e.getCause());
        } catch (final SSLException e) {
            Assert.fail("Unexpected failure.", e);
        }
    }

    /**
     * Runs a TLS handshake between the client engine and a new server engine, passing records through memory.
     *
     * @param client client engine
     * @param serverContext context of the server to handshake with
     * @throws SSLException when either side refuses the handshake
     */
    private void handshake(final SSLEngine client, final SSLContext serverContext) throws SSLException {
        final SSLEngine server = serverContext.createSSLEngine();
        server.setUseClientMode(false);
        final ByteBuffer clientToServer = ByteBuffer.allocate(BUFFER_SIZE);
        final ByteBuffer serverToClient = ByteBuffer.allocate(BUFFER_SIZE);
        final ByteBuffer app = ByteBuffer.allocate(BUFFER_SIZE);
        client.beginHandshake();
        server.beginHandshake();
        for (int i = 0; i < MAX_HANDSHAKE_STEPS; i++) {
            if (isDone(client) && isDone(server)) {
                return;
            }
            step(client, serverToClient, clientToServer, app);
            step(server, clientToServer, serverToClient, app);
        }
        Assert.fail("Handshake did not finish.");
    }

    /**
     * Moves one engine one step forward.
     *
     * @param engine the engine
     * @param in records from the peer, in write mode
     * @param out records to the peer, in write mode
     * @param app application data sink
     * @throws SSLException when the engine refuses the handshake
     */
    private void step(final SSLEngine engine, final ByteBuffer in, final ByteBuffer out, final ByteBuffer app) throws SSLException {
        final HandshakeStatus status = engine.getHandshakeStatus();
        if (status == HandshakeStatus.NEED_TASK) {
            Runnable task;
            while ((task = engine.getDelegatedTask()) != null) {
                task.run();
            }
        } else if (status == HandshakeStatus.NEED_WRAP) {
            engine.wrap(ByteBuffer.allocate(0), out);
        } else if (status == HandshakeStatus.NEED_UNWRAP) {
            in.flip();
            engine.unwrap(in, app);
            in.compact();
            app.clear();
        }
    }

    /**
     * @param engine the engine
     * @return true if the engine has finished its handshake
     */
    private boolean isDone(final SSLEngine engine) {
        final HandshakeStatus status = engine.getHandshakeStatus();
        return status == HandshakeStatus.NOT_HANDSHAKING || status == HandshakeStatus.FINISHED;
    }

    /**
     * @param keyStore keystore holding the server key and certificate
     * @return a server context presenting that certificate
     * @throws GeneralSecurityException will not throw
     */
    private SSLContext serverContext(final KeyStore keyStore) throws GeneralSecurityException {
        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, PASSWORD);
        final SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);
        return ctx;
    }

    /**
     * @param resource keystore resource
     * @return the loaded keystore
     * @throws GeneralSecurityException will not throw
     * @throws IOException will not throw
     */
    private KeyStore loadKeyStore(final String resource) throws GeneralSecurityException, IOException {
        final KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            ks.load(in, PASSWORD);
        }
        return ks;
    }
}
