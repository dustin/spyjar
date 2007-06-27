// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>

package net.spy.db;

import java.sql.ResultSet;

/**
 * ResultSet returned by DBSPs.
 */
public abstract class DBSPResult extends ProxyResultSet {

	/**
	 * Get an instance of DBSPResult.
	 */
	public DBSPResult(ResultSet rs) {
		super(rs);
	}

}
