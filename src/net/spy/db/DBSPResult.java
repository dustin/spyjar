// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: DBSPResult.java,v 1.1 2003/03/11 09:10:07 dustin Exp $

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
