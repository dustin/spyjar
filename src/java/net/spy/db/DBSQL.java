// Copyright (c) 2001  SPY internetworking <dustin@spy.net>
//
// arch-tag: 68A71B25-1110-11D9-92A5-000A957659CC

package net.spy.db;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.StringTokenizer;

import java.sql.Connection;
import java.sql.SQLException;

import net.spy.util.SpyConfig;

/**
 * Superclass for dynamic SQL calls.
 */
public abstract class DBSQL extends DBSP {

	private Map<String, String> registeredQueries=null;

	/**
	 * Get a DBSQL object with the given DBConfig.
	 */
	public DBSQL(SpyConfig conf) throws SQLException {
		super(conf);
	}

	/**
	 * Get a DBSQL object with the given Connection.
	 */
	public DBSQL(Connection conn) throws SQLException {
		super(conn);
	}

	/** 
	 * Register a named query.
	 * 
	 * @param name the name of the query (i.e. pgsql)
	 * @param query the query
	 */
	protected void registerQuery(String name, String query) {
		if(registeredQueries==null) {
			registeredQueries=new HashMap();
		}
		registeredQueries.put(name, query);
	}

	/** 
	 * Set the Map of registered queries.
	 * 
	 * @param to the Map of registered queries.
	 */
	protected void setRegisteredQueryMap(Map<String, String> to) {
		registeredQueries=to;
	}

	/** 
	 * Get the registered queries.
	 * @return an unmodifiable Map showing the registered queries.
	 */
	public Map getRegisteredQueries() {
		return(Collections.unmodifiableMap(registeredQueries));
	}

	/**
	 * Prepare the SQL for execution.
	 */
	protected void prepare() throws SQLException {
		if(getPreparedStatement() == null) {
			if(registeredQueries!=null) {
				selectQuery();
			}
		}
		// Make sure all the arguments are there.
		checkArgs();
		applyArgs(getArguments());
	}

	/** 
	 * Generate (and set) a new cursor name.
	 */
	protected void generateCursorName() throws SQLException {
		StringBuffer sb=new StringBuffer();

		int totalSize=32;

		// Get the identity hash code.
		int idhc=System.identityHashCode(this);
		String idhcs=Integer.toHexString(idhc);

		// subtract this length
		totalSize-=idhcs.length();

		// Get the name of the class
		String className=getClass().getName();
		String shortClassName=null;
		StringTokenizer st=new StringTokenizer(className, ".");
		// Get the last token
		while(st.hasMoreTokens()) {
			shortClassName=st.nextToken();
		}

		// totalSize says how many we'll take, figure out what we can do.
		if(shortClassName.length() < totalSize) {
			sb.append(shortClassName);
		} else {
			// Get just enough characters
			sb.append(shortClassName.substring(
				(shortClassName.length() - totalSize)));
		}

		// Append the hash
		sb.append(idhcs);

		setCursorName(sb.toString());
	}

	private void selectQuery() throws SQLException {
		QuerySelector qs=QuerySelectorFactory.getQuerySelector();
		String query=null;
		switch(getInitType()) {
			case INIT_FROM_CONFIG:
				query=qs.getQuery(getConfig(), registeredQueries);
				break;
			case INIT_FROM_CONN:
				query=qs.getQuery(getConn(), registeredQueries);
				break;
			default:
				throw new SQLException("Unknown init type:  " + getInitType());
		}

		if(query==null) {
			throw new SQLException("Could not find query for "
				+ getClass().getName());
		}

		setQuery(query);
	}
}
