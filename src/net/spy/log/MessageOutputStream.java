// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: MessageOutputStream.java,v 1.2 2002/11/20 06:20:01 dustin Exp $

package net.spy.log;

import java.io.IOException;

import java.net.InetAddress;

/**
 * Send SpyMessages when stuff goes to this OutputStream.
 */
public class MessageOutputStream extends LineGettingOutputStream {

	private MCastLog mcl=null;

	/**
	 * Get an instance of MessageOutputStream.
	 */
	public MessageOutputStream(MCastLog mcl) {
		super();
		this.mcl=mcl;
	}

	/** 
	 * Send this chunk as a SpyMessage.
	 * 
	 * @param chunk 
	 * @throws IOException 
	 */
	protected void processChunk(String chunk) throws IOException {
		SpyMessage msg=new SpyMessage(chunk);
		mcl.sendMessage(msg);
	}

	/**
	 * Clean up.
	 */
	public void close() throws IOException {
		mcl.close();
		super.close();
	}

	/**
	 * Test.
	 */
	public static void main(String args[]) throws Exception {
		MCastLog mcl=new MCastLog(
			InetAddress.getByName("227.227.227.227"), 3432);
		MessageOutputStream mos=new MessageOutputStream(mcl);
		mos.setErr();
		mos.setOut();
		System.out.println("Testing at " + new java.util.Date());
	}

}
