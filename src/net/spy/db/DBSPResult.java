// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6844B69D-1110-11D9-A179-000A957659CC

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
