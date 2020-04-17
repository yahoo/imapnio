package com.yahoo.imapnio.async.client;

import java.io.IOException;
import java.time.Clock;
import java.util.List;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.client.ImapAsyncSession.DebugMode;
import com.yahoo.imapnio.async.data.Capability;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.internal.ImapAsyncSessionImpl;
import com.yahoo.imapnio.async.response.ImapResponseMapper;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

/**
 * Unit test for {@link ImapAsyncCreateSessionResponse}.
 */
public class ImapAsyncCreateSessionResponseTest {

    /** Dummy session id. */
    private static final int SESSION_ID = 123456;

    /** Clock instance. */
    private Clock clock;

    /**
     * Sets up instances for before method.
     */
    @BeforeMethod
    public void beforeMethod() {
        clock = Mockito.mock(Clock.class);
        Mockito.when(clock.millis()).thenReturn(1L);
    }

    /**
     * Tests ImapAsyncResponse constructor and getters.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testImapAsyncCreateSessionResponse() throws IOException, ProtocolException, ImapAsyncClientException {
        final IMAPResponse imapResponse = new IMAPResponse(
                "* OK [CAPABILITY IMAP4rev1 SASL-IR AUTH=PLAIN AUTH=XOAUTH2 AUTH=OAUTHBEARER ID MOVE NAMESPACE] IMAP4rev1 Hello");
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        final Channel channel = Mockito.mock(Channel.class);

        final Logger logger = Mockito.mock(Logger.class);

        final String sessCtx = "Sauroposeidon@tallerthan.tree";

        final ImapAsyncSession session = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline, sessCtx);

        final ImapAsyncCreateSessionResponse respOut = new ImapAsyncCreateSessionResponse(session, imapResponse);

        Assert.assertEquals(respOut.getSession(), session, "getSession() result mismatched.");
        // verify Capability
        final IMAPResponse greeting = respOut.getServerGreeting();
        Assert.assertEquals(greeting, imapResponse, "getServerGreeting() result mismatched.");
        final ImapResponseMapper mapper = new ImapResponseMapper();
        final IMAPResponse[] content = { greeting };

        final Capability capa = mapper.readValue(content, Capability.class);

        // verify the result
        Assert.assertNotNull(capa, "result should never return null.");
        Assert.assertTrue(capa.hasCapability("IMAP4rev1".toUpperCase()), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("SASL-IR"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("ID"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("MOVE"), "One capability missed.");
        Assert.assertTrue(capa.hasCapability("NAMESPACE"), "One capability missed.");
        final List<String> authValues = capa.getCapability("AUTH");
        Assert.assertNotNull(authValues, "AUTH values missed.");
        Assert.assertEquals(authValues.size(), 3, "One Auth value missed");
        Assert.assertEquals(authValues.get(0), "PLAIN", "One Auth value missed");
        Assert.assertEquals(authValues.get(1), "XOAUTH2", "One Auth value missed");
        Assert.assertEquals(authValues.get(2), "OAUTHBEARER", "One Auth value missed");
    }

    /**
     * Tests ImapAsyncResponse constructor when greeting does not have capability.
     *
     * @throws IOException will not throw
     * @throws ProtocolException will not throw
     */
    @Test
    public void testImapAsyncCreateSessionResponseNoCapability() throws IOException, ProtocolException {
        final IMAPResponse imapResponse = new IMAPResponse("* OK IMAP4rev1 Hello");

        final Logger logger = Mockito.mock(Logger.class);
        final ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        final Channel channel = Mockito.mock(Channel.class);

        final String sessCtx = "Sauroposeidon@tallerthan.tree";

        final ImapAsyncSession session = new ImapAsyncSessionImpl(clock, channel, logger, DebugMode.DEBUG_ON, SESSION_ID, pipeline, sessCtx);
        final ImapAsyncCreateSessionResponse respOut = new ImapAsyncCreateSessionResponse(session, imapResponse);
        Assert.assertEquals(respOut.getSession(), session, "getSession() result mismatched.");
        final IMAPResponse greeting = respOut.getServerGreeting();
        Assert.assertNotNull(greeting, "getServerGreeting() result mismatched.");
        Assert.assertEquals(greeting, imapResponse, "getServerGreeting() result mismatched.");
        Assert.assertFalse(greeting.keyEquals("CAPABILITY"), "getServerGreeting() result mismatched.");
    }
}
