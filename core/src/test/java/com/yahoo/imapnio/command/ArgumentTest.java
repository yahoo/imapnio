package com.yahoo.imapnio.command;

import java.io.IOException;

import javax.mail.Flags;
import javax.mail.internet.MimeUtility;
import javax.mail.search.AndTerm;
import javax.mail.search.BodyTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.NotTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.SearchException;
import javax.mail.search.SubjectTerm;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.imap.protocol.SearchSequence;
import com.yahoo.imapnio.async.data.ExtendedModifiedSinceTerm;
import com.yahoo.imapnio.async.request.ExtendedSearchSequence;

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

    /**
     * Tests constructor and toString() method with null character set and extended search sequence.
     *
     * @throws IOException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testConstructorModifiedSince() throws SearchException, IOException {

        final ExtendedSearchSequence searchSeq = new ExtendedSearchSequence();
        final ExtendedModifiedSinceTerm extendedModifiedSinceTerm = new ExtendedModifiedSinceTerm(1L);
        final Argument args = new Argument();
        args.append(searchSeq.generateSequence(extendedModifiedSinceTerm));

        final String searchStr = args.toString();
        Assert.assertEquals(searchStr, "MODSEQ 1", "generateSequence() mismatched.");
    }

    /**
     * Tests constructor and toString() method with null character set and extended search sequence with Or And search terms.
     *
     * @throws IOException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testConstructorOrAnd() throws SearchException, IOException {

        final ExtendedSearchSequence searchSeq = new ExtendedSearchSequence();
        final BodyTerm bodyTerm = new BodyTerm("test");
        final AndTerm andTerm = new AndTerm(bodyTerm, bodyTerm);
        final OrTerm orTerm = new OrTerm(andTerm, andTerm);
        final Argument args = new Argument();
        args.append(searchSeq.generateSequence(orTerm, null));

        final String searchStr = args.toString();
        Assert.assertEquals(searchStr, "OR (BODY test BODY test) (BODY test BODY test)", "generateSequence() mismatched.");
    }

    /**
     * Tests constructor and toString() method with null character set and extended search sequence with Not And Modified Since terms.
     *
     * @throws IOException will not throw
     * @throws SearchException will not throw
     */
    @Test
    public void testConstructorNotAndModifiedSince() throws SearchException, IOException {

        final ExtendedSearchSequence searchSeq = new ExtendedSearchSequence();
        final ExtendedModifiedSinceTerm extendedModifiedSinceTerm = new ExtendedModifiedSinceTerm(1L);
        final BodyTerm bodyTerm = new BodyTerm("test");
        final AndTerm andTerm = new AndTerm(bodyTerm, extendedModifiedSinceTerm);
        final NotTerm notTerm = new NotTerm(andTerm);
        final Argument args = new Argument();
        args.append(searchSeq.generateSequence(notTerm));

        final String searchStr = args.toString();
        Assert.assertEquals(searchStr, "NOT (BODY test MODSEQ 1)", "generateSequence() mismatched.");
    }
}
