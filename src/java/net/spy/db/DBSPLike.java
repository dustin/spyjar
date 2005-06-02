// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 67D90AD6-1110-11D9-B767-000A957659CC

package net.spy.db;

import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * Interface describing the basic DBSP features.
 *
 * This is used primarily as a superinterface for any abstract spts.
 */
public interface DBSPLike {

	/** 
	 * Execute a query.
	 * 
	 * @return the Results from the query
	 * @throws SQLException if there's a problem executing the query
	 */
	ResultSet executeQuery() throws SQLException;

	/** 
	 * Execute an update.
	 * 
	 * @return the number of rows affected by this update
	 * @throws SQLException if there's a problem executing the query
	 */
	int executeUpdate() throws SQLException;

	/** 
	 * Set the cache time for this cachable query.
	 * 
	 * @param time time (in seconds) results should be valid
	 */
	void setCacheTime(long time);

	/** 
	 * Get the cache time configured for this call.
	 * 
	 * @return time (in seconds) results should be valid
	 */
	long getCacheTime();

	/** 
	 * Set the timeout for this query.
	 *
	 * @see Statement#setQueryTimeout(int)
	 */
	void setQueryTimeout(int timeout);

	/** 
	 * Get the query timeout.
	 *
	 * @see Statement#getQueryTimeout()
	 */
	int getQueryTimeout();

	/** 
	 * Get rid of this object.
	 */
	void close();

}
