/*
 * Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
 *
 * $Id: CachedResultSetStub.java,v 1.2 2002/09/24 17:44:07 dustin Exp $
 */

package net.spy.db;

import java.math.BigDecimal;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Types;

import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;

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
			e.printStackTrace();
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
