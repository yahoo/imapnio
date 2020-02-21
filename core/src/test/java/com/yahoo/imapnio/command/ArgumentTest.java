package com.yahoo.imapnio.command;

import java.io.IOException;

import javax.mail.Flags;
import javax.mail.internet.MimeUtility;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchException;
import javax.mail.search.SubjectTerm;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.imap.protocol.SearchSequence;

/**
 * Unit test for {@link Argument}.
 */
public class ArgumentTest {

    /**
     * Tests constructor and toString() method with null character set.
     *
     * @throws IOException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testConstructorAndToStringNullCharset() throws SearchException, IOException {

        final SearchSequence searchSeq = new SearchSequence();
        // message id
        final String msgIds = "1:5";

        // charset
        final String charset = null;

        // SearchTerm
        final Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        final FlagTerm term = new FlagTerm(flags, true);
        final Argument args = new Argument();
        args.append(searchSeq.generateSequence(term, null));

        args.writeAtom(msgIds);

        final String searchStr = args.toString();
        Assert.assertEquals(searchStr, "DELETED SEEN 1:5", "result mismatched.");
    }

    /**
     * Tests constructor and toString() method with null character set.
     *
     * @throws IOException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testConstructorAndToStringNoneNullCharset() throws SearchException, IOException {

        final SearchSequence searchSeq = new SearchSequence();
        // message id
        final String msgIds = "1:5";

        // charset
        final String charset = "UTF-8";

        // SearchTerm
        final SubjectTerm term = new SubjectTerm("ΩΩ");
        final Argument args = new Argument();
        args.append(searchSeq.generateSequence(term, MimeUtility.javaCharset(charset)));

        args.writeAtom(msgIds);

        final String searchStr = args.toString();
        Assert.assertEquals(searchStr, "SUBJECT {4+}\r\nￎﾩￎﾩ 1:5", "result mismatched.");
    }
}