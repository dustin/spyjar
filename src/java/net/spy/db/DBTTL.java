// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 690A8B46-1110-11D9-B5AD-000A957659CC

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
	@Override
	public String toString() {
		return("DBTTL:  " + getTTL());
	}

	/** 
	 * Report DB specific message.
	 */
	@Override
	protected void doReport() {
		// Get the message.
		String msg=getMessageFromBundle("net.spy.db.messages",
			"dbttl.msg", "dbttl.msg.witharg");

		reportWithFormat(msg);
	}

}
