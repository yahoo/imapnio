package com.yahoo.imapnio.async.request;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.yahoo.imapnio.async.data.QResyncParameter;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * This class defines imap select command request from client.
 */
public class SelectFolderCommand extends OpenFolderActionCommand {

    /** Command name. */
    private static final String SELECT = "SELECT";

    /**
     * Initializes a @{code SelectCommand}.
     *
     * @param folderName folder name to select
     */
    public SelectFolderCommand(@Nonnull final String folderName) {
        super(SELECT, folderName);
    }

    /**
     * Initializes a @{code SelectCommand}.
     *
     * @param folderName folder name to select
     * @param qResyncParameter qresync parameter
     */
    public SelectFolderCommand(@Nonnull final String folderName, @Nonnull final QResyncParameter qResyncParameter) {
        super(SELECT, folderName, qResyncParameter);
    }

    @Override
    public ImapCommandType getCommandType() {
        return ImapCommandType.SELECT_FOLDER;
    }
}
