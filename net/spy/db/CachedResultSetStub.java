/*
 * Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
 *
 * $Id: CachedResultSetStub.java,v 1.4 2003/08/05 09:01:03 dustin Exp $
 */

package net.spy.db;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This object represents a cached java.sql.ResultSet.  It will hopefully
 * only contain small results.
 */
public class CachedResultSetStub extends GenericResultSetStub {

	private int copies=0;

	/**
	 * Magically transform the passed in ResultSet to a CachedResultSet
	 *
	 * @param rs the ResultSet we want to magically transform
	 *
	 * @exception SQLException if the ResultSet somehow fails us.
	 */
	public CachedResultSetStub(ResultSet rs) throws SQLException {
		super(rs);
	}

	/**
	 * Make a copy of this object.
	 */
	public CachedResultSetStub newCopy() {
		CachedResultSetStub rv=null;
		try {
			copies++;
			rv=(CachedResultSetStub)clone();
		} catch(CloneNotSupportedException e) {
			// The exceptions this thing throws, well, aren't
			getLogger().error("So, " + getClass().getName()
				+ " seems to think that clone isn't supported.", e);
		}
		return(rv);
	}

	/** 
	 * How many copies have been made of this result set?
	 * 
	 * @return the number of copies
	 */
	public int numCopies() {
		return(copies);
	}
}
