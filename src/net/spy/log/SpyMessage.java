// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 76B87444-1110-11D9-BAE2-000A957659CC

package net.spy.log;

import java.io.Serializable;

import java.text.SimpleDateFormat;

import java.util.Date;

/**
 * Log message for use in textual logs.
 */
public class SpyMessage extends Object implements Serializable {

	private long timestamp=0;
	private String message=null;

	/**
	 * Get an instance of SpyMessage.
	 */
	public SpyMessage(String message) {
		super();
		timestamp=System.currentTimeMillis();
		this.message=message;
	}

	/**
	 * String representation of this message.
	 */
	public String toString() {
		SimpleDateFormat df=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
		return("[" + df.format(new Date(timestamp)) + "] " + message);
	}

	/**
	 * Testing and what not.
	 */
	public static void main(String args[]) throws Exception {
		SpyMessage sm=new SpyMessage(args[0]);
		System.out.println(sm);
	}

}

