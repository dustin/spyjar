// Copyright (c) 2001  SPY internetworking <dustin@spy.net>
//
// arch-tag: 66DD0326-1110-11D9-9229-000A957659CC

package net.spy.db;

/**
 * Represents NULL data in DB parameters and stuff.
 */
public class DBNull extends Object {
	private int type=-1;

	/**
	 * Get a new null object.
	 */
	public DBNull(int type) {
		super();
		this.type=type;
	}

	/**
	 * Get the data type of this nullness.
	 */
	public int getType() {
		return type;
	}
}


