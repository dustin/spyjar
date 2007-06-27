// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>

package net.spy.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Select the most appropriate query for the given DBSQL instance.
 */
public interface QuerySelector {

	/** 
	 * Name of the default query in the map.
	 */
	static final String DEFAULT_QUERY="-default-";

	/** 
	 * Get the query for the given connection.
	 * 
	 * @param conn the connection that wants the query
	 * @param queryMap the Map of queries by name
	 * @return the query (as a String), null if a suitable query can't be found
	 */
	String getQuery(Connection conn, Map<String, String> queryMap)
		throws SQLException;

}
