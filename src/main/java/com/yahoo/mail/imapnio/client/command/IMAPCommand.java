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
    protected  CommandType type;

    public IMAPCommand(String tag, String command, Argument args, String[] capabilities) {
        this.tag = tag;
        this.command = command;
        this.args = args;
        this.capabilities = capabilities;
        //type = CommandType.valueOf(command);
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
    
    public CommandType getType() {
    	return type;
    }
    
    enum CommandType {
    	AUTHENTICATE_XOAUTH2("AUTHENTICATE XOAUTH2"),
    	LOGIN("LOGIN"),
    	IDLE("IDLE"),
    	LOGOUT("LOGOUT"),
    	CAPABILITY("CAPABILITY"),
    	STATUS("STATUS"),
    	SELECT("SELECT");
    	
    	private final String command;
    	CommandType(String c) {
    		this.command = c;
    	}
    };
}


