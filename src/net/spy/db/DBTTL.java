// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: DBTTL.java,v 1.2 2002/11/07 07:43:55 dustin Exp $

package net.spy.db;

import net.spy.util.TTL;

/**
 * Used to track checked out DB connections to report on connections that
 * have been checked out longer than we expect them to be.
 */
public class DBTTL extends TTL {

	/**
	 * Get an instance of DBTTL.
	 */
	public DBTTL(long ttl) {
		super(ttl);
	}

	/**
	 * Get an instance of DBTTL with an extra object.
	 */
	public DBTTL(long ttl, Object extra) {
		super(ttl, extra);
	}

	/**
	 * String me.
	 */
	public String toString() {
		return("DBTTL:  " + getTTL());
	}

	/** 
	 * Report DB specific message.
	 */
	protected void doReport() {
		// Get the message.
		String msg=getMessageFromBundle("net.spy.db.messages",
			"dbttl.msg", "dbttl.msg.witharg");

		reportWithFormat(msg);
	}

}
