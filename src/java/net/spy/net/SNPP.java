// SNPP implementation
//
// Copyright (c) 1999 Dustin Sallings
//
// arch-tag: 78144148-1110-11D9-8C3B-000A957659CC

package net.spy.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import net.spy.SpyObject;
import net.spy.util.SpyUtil;

/**
 * SNPP client.
 */
public class SNPP extends SpyObject {

	private static final int STATUS_TYPE_LENGTH=1;
	private static final int STATUS_LENGTH=3;

	/** 
	 * SNPP default port.
	 */
	public static final int SNPP_PORT=444;

	/** 
	 * Response code indicating a line of multiline help.
	 */
	public static final int MULTILINE_HELP=214;

	/** 
	 * Response code indicating single line help.
	 */
	public static final int SINGLELINE_HELP=218;

	/** 
	 * Response code to acknowldege a quit command.
	 */
	public static final int QUIT_ACK=221;

	/** 
	 * Response code inicating a successful transaction.
	 */
	public static final int OK=250;

	/** 
	 * Response code indicating a fatal error which will be followed by a
	 * terminated connection.
	 */
	public static final int FATAL_ERROR=421;

	/** 
	 * Response code indicating that a command was issued that was not
	 * implemented.
	 */
	public static final int COMMAND_NOT_IMPLEMENTED=500;

	/** 
	 * Response code indicating a command was implemented in duplicate.
	 */
	public static final int DUPLICATE_COMMAND_ENTRY=503;

	/** 
	 * Response code indicating a command failed (invalid pager ID, illegal
	 * parameter, etc...).
	 */
	public static final int ADMIN_FAILURE=550;
	/** 
	 * Response code indicating a command failed (system problem).
	 */
	public static final int SYSTEM_FAILURE=554;

	/** 
	 * Response code indicating too many of something was entered (i.e. too
	 * many pager IDs).
	 */
	public static final int MAX_ENTRIES_EXCEEDED=552;

	/** 
	 * Response code indicating DATA command was accepted and input should
	 * begin.
	 */
	public static final int DATA_OK=354;

	/** 
	 * Response code indicating a message was delivered and is awaiting a reply
	 * ACK.
	 */
	public static final int MSG_AWAITING_ACK=860;

	/** 
	 * Response code indicating a message was delivered and is awaiting a
	 * reply.
	 */
	public static final int MSG_AWAITING_REPLY=861;

	/** 
	 * Response code indicating a message has been delivered.
	 */
	public static final int MSG_DELIVERED=880;

	/** 
	 * Response code indicating a response to a message is available.
	 */
	public static final int RESPONSE=889;

	/** 
	 * Response code indicating a message was queued for delivery.
	 */
	public static final int MSG_QUEUED=960;

	/** 
	 * Message type indicating success.
	 */
	public static final int MTYPE_SUCCESS=2;

	/** 
	 * Message type indicating DATA was accepted and server is ready for input.
	 */
	public static final int MTYPE_DATA_SUCCESS=3;

	/** 
	 * Message type indicating a permanent failure.
	 */
	public static final int MTYPE_PERM_FAILURE=4;

	/** 
	 * Message type indicating a temporary failure.
	 */
	public static final int MTYPE_TEMP_FAILURE=5;

	/** 
	 * Message type indicating an unsuccesful two-way specific transaction.
	 */
	public static final int MTYPE_UNSUCCESSFUL_2WAY=7;

	/** 
	 * Message type indicating a successful two-way transaction.
	 */
	public static final int MTYPE_SUCCESS_2WAY=8;

	/** 
	 * Successful queued transaction.
	 */
	public static final int MTYPE_SUCCESS_QUEUED=9;

	private Socket socket=null;
	private InputStream in=null;
	private OutputStream out=null;
	private BufferedReader din=null;
	private PrintWriter prout=null;

	// 2way support
	private boolean goesBothWays=false;
	private String msgTag=null;

	// Current full line received from the SNPP server.
	private String currentLine=null;
	// Current message received from the SNPP server.
	private String currentMessage=null;
	// Current status received from SNPP server.
	private int currentStatus=-1;
	// Current type of status
	private int currentStatusType=-1;

	/**
	 * Get a new SNPP object connected to host:port
	 *
	 * @param host SNPP host to connect to
	 * @param port SNPP port number
	 * @param timeout SO_TIMEOUT in milliseconds
	 *
	 * @exception IOException Thrown if the various input and output
	 * streams cannot be established.
	 *
	 * @exception UnknownHostException Thrown if the SNPP server hostname
	 * cannot be resolved.
	 */
	public SNPP(String host, int port, int timeout)
		throws IOException, UnknownHostException {
		socket = new Socket(host, port);

		if(timeout>0) {
			socket.setSoTimeout(timeout);
		}

		in=socket.getInputStream();
		din = new BufferedReader(new InputStreamReader(in));
		out=socket.getOutputStream();
		prout=new PrintWriter(out);

		getaline();
	}

	/**
	 * Get a new SNPP object connected to host:port
	 *
	 * @param host SNPP host to connect to
	 * @param port SNPP port number
	 *
	 * @exception IOException Thrown if the various input and output
	 * streams cannot be established.
	 *
	 * @exception UnknownHostException Thrown if the SNPP server hostname
	 * cannot be resolved.
	 */
	public SNPP(String host, int port)
		throws IOException, UnknownHostException {
		this(host, port, 0);
	}

	/**
	 * Current full line received from the SNPP server.
	 */
	public String getCurrentline() {
		return(currentLine);
	}

	/**
	 * Current message received from the SNPP server.
	 */
	public String getCurrentmessage() {
		return(currentMessage);
	}

	/**
	 * Current status received from SNPP server.
	 */
	public int getCurrentStatus() {
		return(currentStatus);
	}

	/** 
	 * Current status type received from SNPP server.
	 */
	public int getCurrentStatusType() {
		return(currentStatusType);
	}

	/**
	 * Put this into 2way mode.
	 *
	 * @exception Exception when the 2way command fails
	 */
	public void twoWay() throws Exception {
		cmd("2way");
		goesBothWays=true;
	}

	/**
	 * sets the pager ID
	 *
	 * @param id snpp pager id
	 *
	 * @exception Exception when the page command fails
	 */
	public void pagerID(String id) throws Exception {
		cmd("page " + id);
	}

	/**
	 * sets the message to send
	 *
	 * @param msg snpp message
	 *
	 * @exception Exception when the command fails
	 */
	public void message(String msg) throws Exception {
		String tmp="";
		String atmp[]=SpyUtil.split("\r\n", msg);
		for(int i=0; i<atmp.length; i++) {
			tmp+=atmp[i] + " ";
		}
		cmd("mess " + tmp);
	}

	/**
	 * sets the message to send, keeps newlines and all that
	 *
	 * @param msg snpp message
	 *
	 * @exception Exception when the command fails, possibly because DATA
	 * is not supported
	 */
	public void data(String msg) throws Exception {
		try {
			cmd("data");
		} catch(Exception e) {
			if(currentStatusType != MTYPE_DATA_SUCCESS) {
				throw e;
			}
		}
		cmd(msg + "\r\n.");
	}

	/**
	 * gets the message tag on a 2way page
	 *
	 * @return the tag, or null if there is no tag
	 */
	public String getTag() {
		return(msgTag);
	}

	/**
	 * Send a simple page.
	 *
	 * @param id SNPP recipient ID.
	 * @param msg msg to send.
	 *
	 * @exception Exception Thrown if any of the commands required to send
	 * the page threw an exception.
	 */
	public void sendpage(String id, String msg) throws Exception {
		// Reset so this thing can be called more than once.
		cmd("rese");
		if(goesBothWays) {
			twoWay();
		}
		pagerID(id);
		message(msg);
		// My pager server supports priority, so we'll ignore any errors
		// with this one.
		try {
			cmd("priority high");
		} catch(Exception e) {
			// This is a nonstandard command and is likely to throw an
			// exception.
			if(getLogger().isDebugEnabled()) {
				getLogger().debug("Failed to set priority");
			}
		}
		send();
	}

	/**
	 * send is handled separately in case it's a two-way transaction.
	 *
	 * @exception Exception Thrown if the send command fails.
	 */
	public void send() throws Exception {
		cmd("send");
		if(goesBothWays) {
			// If it looks 2way, we get the stuff
			if(currentStatusType == MTYPE_SUCCESS_2WAY) {
				String a[]=SpyUtil.split(" ", currentMessage);
				msgTag=a[0] + " " + a[1];
			}
		}
	}

	/**
	 * Check for a response from a 2way message.
	 *
	 * @param tag the message tag to look up.
	 * @return the response message, or NULL if it's not ready
	 * @exception Exception when the msta command fails, or we're not doing
	 * 2way.
	 */
	public String getResponse(String tag) throws Exception {
		String ret=null;
		if(goesBothWays) {
			cmd("msta " + tag);
			if(currentStatus == RESPONSE) {
				String tmp=currentMessage;
				tmp=tmp.substring(tmp.indexOf(" ")).trim();
				tmp=tmp.substring(tmp.indexOf(" ")).trim();
				tmp=tmp.substring(tmp.indexOf(" ")).trim();
				ret=tmp;
			}
		} else {
			throw new Exception("I don't go both ways.");
		}
		return(ret);
	}

	/**
	 * Check for a response from a 2way message.
	 *
	 * @return the response message, or NULL if it's not ready
	 *
	 * @exception Exception when the msta command fails, or we're not doing
	 * 2way.
	 */
	public String getResponse() throws Exception {
		if(msgTag == null) {
			throw new Exception("No msg tag received, have you done a "
				+ "2way page yet?");
		}
		return(getResponse(msgTag));
	}

	/**
	 * adds a response to the SNPP message.
	 *
	 * @param response the canned response to add
	 *
	 * @exception Exception when we're not in a 2way transaction, or the
	 * command fails.
	 */
	public void addResponse(String response) throws Exception {
		if(!goesBothWays) {
			throw new Exception("I don't go both ways.");
		}
		cmd("mcre " + response);
	}

	/**
	 * Send an SNPP command.
	 *
	 * @param command command to send.  It's sent literally to the SNPP
	 * server.
	 *
	 * @exception Exception Thrown if the command does not return an ``OK''
	 * status from the SNPP server.
	 */
	public void cmd(String command) throws Exception {
		if(getLogger().isDebugEnabled()) {
			getLogger().debug(">> " + command);
		}
		prout.print(command + "\r\n");
		prout.flush();
		getaline();
		if(!ok()) {
			throw new Exception(currentMessage + " (" + command + ")");
		}
	}

	/**
	 * Close the connection to the SNPP server.
	 */
	public void close() {
		if(socket!=null) {
			try {
				cmd("quit");
			} catch(Exception e) {
				// Don't care, we tried...
				getLogger().warn("Exception while quitting", e);
			} finally {
				try {
					socket.close();
				} catch(IOException e) {
					// Don't care anymore
					getLogger().warn("Exception while quitting", e);
				}
			}
			// Go ahead and set s to null anyway.
			socket=null;
		}
	}

	protected void finalize() throws Throwable {
		if(getLogger().isDebugEnabled()) {
			getLogger().debug("Finalizing...");
		}
		close();
		super.finalize();
	}

	// Return whether the current status number is within an OK range.
	private boolean ok() {
		boolean rv = false;
		if(currentStatusType == MTYPE_SUCCESS) {
			rv = true;
		}
		// Specific stuff for two-way
		if(goesBothWays && !rv) {
			if(currentStatusType == MTYPE_SUCCESS_2WAY) {
				// delivered, processing or final
				rv=true;
			} else if(currentStatusType == MTYPE_SUCCESS_QUEUED) {
				// Queued transaction
				rv=true;
			}
		}
		return(rv);
	}

	// Return a line from the SNPP server.
	private void getaline() throws IOException {
		// Get the line
		currentLine = din.readLine();

		// make sure we read something
		if(currentLine==null) {
			throw new IOException("Read returned null, disconnected?");
		}

		if(getLogger().isDebugEnabled()) {
			getLogger().debug("<< " + currentLine);
		}

		// Extract the message
		currentMessage = currentLine.substring(STATUS_LENGTH+1);

		// Calculate the status number and type
		String stmp = currentLine.substring(0, STATUS_LENGTH);
		currentStatus = Integer.parseInt(stmp);
		stmp = currentLine.substring(0, STATUS_TYPE_LENGTH);
		currentStatusType = Integer.parseInt(stmp);
	}

}
