/**
 *
 */
package com.lafaspot.imapnio.command;

import com.sun.mail.iap.Argument;


/**
 * @author kraman
 *
 */
public class ImapCommand {

    /** IMAP command tag. */
    protected String tag;

    /** IMAP command. */
    protected String command;

    /** Command arguments. */
    protected Argument args;

    /** capabilities needed for this command. */
    protected String[] capabilities;

    /** command type. */
    protected  CommandType type;

    /**
     * Builds an IMAP command.
     * @param tag IMAP tag
     * @param command actual IMAP command
     * @param args command arguments
     * @param capabilities capabilities needed for this command
     */
    public ImapCommand(final String tag, final String command, final Argument args, final String[] capabilities) {
        this.tag = tag;
        this.command = command;
        this.args = args;
        this.capabilities = capabilities;
        //type = CommandType.valueOf(command);
    }

    /**
     * Returns the command string.
     * @return command
     */
    public String getCommand() {
        return command;
    }

    /**
     * Returns the command arguments.
     * @return command arguments
     */
    public Argument getArgs() {
        return args;
    }

    /**
     * Capabilities needed for command.
     * @return capabilities
     */
    public String[] getCapabilities() {
        return capabilities;
    }

    /**
     * Returns the IMAP tag used by this commnad.
     * @return IMAP tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Returns command type.
     * @return command type
     */
    public CommandType getType() {
    	return type;
    }

    /**
     * Defines the types of commands supported.
     * @author kraman
     *
     */
    enum CommandType {
        /** OAUTH command. */
    	AUTHENTICATE_XOAUTH2("AUTHENTICATE XOAUTH2"),
    	/** LOGIN command. */
    	LOGIN("LOGIN"),
    	/** IDLE command. */
    	IDLE("IDLE"),
    	/** LOGOUT command. */
    	LOGOUT("LOGOUT"),
    	/** CAPABILITY command. */
    	CAPABILITY("CAPABILITY"),
    	/** STATUS command. */
    	STATUS("STATUS"),
    	/** SELECT command. */
    	SELECT("SELECT");
    	
    	/** the command. */
    	private final String command;
    	
    	/**
    	 * Constructs a command type enum.
    	 * @param c command
    	 */
    	CommandType(final String c) {
    		this.command = c;
    	}
    };
}


