// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6C28DADA-1110-11D9-A280-000A957659CC

package net.spy.db;

import java.util.Map;

import java.sql.Connection;

import net.spy.SpyConfig;

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
	String getQuery(Connection conn, Map queryMap);

	/** 
	 * Get the query for the given config.
	 * 
	 * @param conf a SpyConfig containing enough information to find a query
	 * @param queryMap the Map of queries by name
	 * @return the query (as a String), null if a suitable query can't be found
	 */
	String getQuery(SpyConfig conf, Map queryMap);

}
