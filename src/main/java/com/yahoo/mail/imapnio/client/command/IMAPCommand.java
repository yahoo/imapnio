/**
 *
 */
package com.yahoo.mail.imapnio.client.command;

import com.sun.mail.iap.Argument;


/**
 * @author kraman
 *
 */
public class IMAPCommand {
    protected String tag;
    protected String command;
    protected Argument args;
    protected String[] capabilities;

    public IMAPCommand(String tag, String command, Argument args, String[] capabilities) {
        this.tag = tag;
        this.command = command;
        this.args = args;
        this.capabilities = capabilities;
    }

    public String getCommand() {
        return command;
    }

    public Argument getArgs() {
        return args;
    }

    public String[] getCapabilities() {
        return capabilities;
    }

    public String getTag() {
        return tag;
    }
}


