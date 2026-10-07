package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.mail.Flags;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

/**
 * Checks that ordinary IMAP usage is untouched by the argument validation.
 *
 * <p>
 * The validation refuses arguments the protocol cannot carry, and each production is held to its own shape rather than to one shared rule.
 * Both of those can be got wrong in the direction of refusing something legitimate, so the shapes a real client sends every day are pinned
 * here: a password full of punctuation, a mailbox name needing modified UTF-7, the wildcards LIST exists to use, a sequence set with the
 * wildcard that atom-specials keeps out of an atom, and a fetch item carrying spaces, brackets and nested parentheses.
 * </p>
 */
public class CommandArgumentCompatibilityTest {

    /** A message set to attach the fetch and store cases to. */
    private static final MessageNumberSet[] SET = { new MessageNumberSet(1, 5) };

    /**
     * Returns the command line a command produces.
     *
     * @param cmd the command
     * @return the command line
     * @throws ImapAsyncClientException when the command refuses one of its arguments
     */
    private String line(final ImapRequest cmd) throws ImapAsyncClientException {
        return cmd.getCommandLineBytes().toString(StandardCharsets.US_ASCII);
    }

    /**
     * Tests that a password may hold any printable punctuation, including the characters that force quoting and escaping.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testCredentialsWithPunctuation() throws ImapAsyncClientException {
        Assert.assertEquals(line(new LoginCommand("alice", "p@ss w0rd!#$%^&*()")), "LOGIN alice \"p@ss w0rd!#$%^&*()\"\r\n",
                "A password holding spaces and punctuation should be quoted, not refused.");
        Assert.assertEquals(line(new LoginCommand("alice", "pa\"ss\\word")), "LOGIN alice \"pa\\\"ss\\\\word\"\r\n",
                "A password holding a quote and a backslash should be escaped.");
        Assert.assertEquals(line(new LoginCommand("alice@yahoo.com", "s3cret")), "LOGIN alice@yahoo.com s3cret\r\n",
                "An ordinary login should need no quoting at all.");
    }

    /**
     * Tests the mailbox names a client actually sends, including one that needs modified UTF-7.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testMailboxNames() throws ImapAsyncClientException {
        Assert.assertEquals(line(new SelectFolderCommand("INBOX/Archive/2026")), "SELECT INBOX/Archive/2026\r\n", "Mismatched.");
        Assert.assertEquals(line(new SelectFolderCommand("Sent Items")), "SELECT \"Sent Items\"\r\n", "A space should quote the name.");
        Assert.assertEquals(line(new SelectFolderCommand("R&D")), "SELECT R&-D\r\n", "An ampersand should be escaped for modified UTF-7.");
        Assert.assertEquals(line(new SelectFolderCommand("\u53d7\u4fe1\u7bb1")), "SELECT &U9dP4Xux-\r\n", "Non-ascii should become modified UTF-7.");
        Assert.assertEquals(line(new RenameFolderCommand("Old Name", "New Name")), "RENAME \"Old Name\" \"New Name\"\r\n", "Mismatched.");
    }

    /**
     * Tests that the LIST wildcards survive, since listing is what they exist for.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testListWildcards() throws ImapAsyncClientException {
        Assert.assertEquals(line(new ListCommand("", "*")), "LIST \"\" \"*\"\r\n", "LIST should still be able to ask for everything.");
        Assert.assertEquals(line(new ListCommand("", "%")), "LIST \"\" \"%\"\r\n", "LIST should still be able to ask for one level.");
        Assert.assertEquals(line(new ListCommand("", "INBOX/*")), "LIST \"\" \"INBOX/*\"\r\n", "Mismatched.");
    }

    /**
     * Tests every shape a sequence set takes, including the wildcard that atom-specials keeps out of an atom.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testSequenceSetShapes() throws ImapAsyncClientException {
        Assert.assertEquals(line(new UidExpungeCommand("42")), "UID EXPUNGE 42\r\n", "Mismatched.");
        Assert.assertEquals(line(new UidExpungeCommand("1:*")), "UID EXPUNGE 1:*\r\n", "An open range should be accepted.");
        Assert.assertEquals(line(new UidExpungeCommand("*")), "UID EXPUNGE *\r\n", "A bare wildcard should be accepted.");
        Assert.assertEquals(line(new UidExpungeCommand("1,3,5:9,20:*")), "UID EXPUNGE 1,3,5:9,20:*\r\n", "Mismatched.");
        Assert.assertEquals(line(new UidExpungeCommand("4294967295")), "UID EXPUNGE 4294967295\r\n", "Mismatched.");
    }

    /**
     * Tests the fetch items a client sends, which hold spaces, brackets, dots, angle brackets and nested parentheses.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testFetchItemShapes() throws ImapAsyncClientException {
        Assert.assertEquals(line(new FetchCommand(SET, "UID FLAGS INTERNALDATE RFC822.SIZE")),
                "FETCH 1:5 (UID FLAGS INTERNALDATE RFC822.SIZE)\r\n", "Mismatched.");
        Assert.assertEquals(line(new FetchCommand(SET, "BODY[]")), "FETCH 1:5 (BODY[])\r\n", "Mismatched.");
        Assert.assertEquals(line(new FetchCommand(SET, "BODY.PEEK[HEADER.FIELDS (SUBJECT FROM DATE)]")),
                "FETCH 1:5 (BODY.PEEK[HEADER.FIELDS (SUBJECT FROM DATE)])\r\n", "Nested parentheses should be accepted.");
        Assert.assertEquals(line(new FetchCommand(SET, "BODY.PEEK[TEXT]<0.2048>")), "FETCH 1:5 (BODY.PEEK[TEXT]<0.2048>)\r\n",
                "A partial should be accepted.");
        Assert.assertEquals(line(new FetchCommand(SET, "BODY[1.2.MIME]")), "FETCH 1:5 (BODY[1.2.MIME])\r\n", "Mismatched.");
    }

    /**
     * Tests the atom arguments, including a capability holding the equals sign that atom-specials does not exclude.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testAtomArguments() throws ImapAsyncClientException {
        Assert.assertEquals(line(new StatusCommand("INBOX", new String[] { "MESSAGES", "RECENT", "UIDNEXT", "UIDVALIDITY", "UNSEEN" })),
                "STATUS INBOX (MESSAGES RECENT UIDNEXT UIDVALIDITY UNSEEN)\r\n", "Mismatched.");
        Assert.assertEquals(line(new EnableCommand(new String[] { "QRESYNC", "UTF8=ACCEPT" })), "ENABLE QRESYNC UTF8=ACCEPT\r\n",
                "'=' is not an atom-special, so a capability may hold one.");
    }

    /**
     * Tests the flags a client sets, both the system ones and the keywords.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testFlags() throws ImapAsyncClientException {
        final Flags system = new Flags();
        system.add(Flags.Flag.SEEN);
        system.add(Flags.Flag.ANSWERED);
        Assert.assertEquals(line(new StoreFlagsCommand("1:5", system, FlagsAction.ADD, false)), "STORE 1:5 +FLAGS (\\Answered \\Seen)\r\n",
                "Mismatched.");

        final Flags keywords = new Flags();
        keywords.add("$Forwarded");
        Assert.assertEquals(line(new StoreFlagsCommand("1:5", keywords, FlagsAction.ADD, false)), "STORE 1:5 +FLAGS ($Forwarded)\r\n",
                "'$' is not an atom-special, so a keyword may start with one.");
    }

    /**
     * Tests APPEND, whose date holds spaces, and ID, whose values do too.
     *
     * @throws ImapAsyncClientException will not throw
     */
    @Test
    public void testAppendAndId() throws ImapAsyncClientException {
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        Assert.assertTrue(line(new AppendCommand("Drafts", flags, new Date(1552413335000L), "hi".getBytes())).startsWith("APPEND Drafts (\\Seen) \""),
                "An append should still quote its internal date.");

        final Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("name", "Yahoo Mail");
        params.put("version", "1.0.0");
        Assert.assertEquals(line(new IdCommand(params)), "ID (\"name\" \"Yahoo Mail\" \"version\" \"1.0.0\")\r\n",
                "An ID value holding a space should be quoted, not refused.");
    }
}
