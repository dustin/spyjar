// Copyright (c) 2001  SPY internetworking <dustin@spy.net>

package net.spy.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.StringTokenizer;

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
	public Map<String, String> getRegisteredQueries() {
		return(Collections.unmodifiableMap(registeredQueries));
	}

	/**
	 * Prepare the SQL for execution.
	 */
	@Override
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
		StringBuilder sb=new StringBuilder();

		int totalSize=32;

		// Get the identity hash code.
		int idhc=System.identityHashCode(this);
		String idhcs=Integer.toHexString(idhc);

		// subtract this length
		totalSize-=idhcs.length();

		// Get the name of the class
		String className=getClass().getName();
		String shortClassName=className;
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
		String query=qs.getQuery(getConn(), registeredQueries);

		if(query==null) {
			throw new SQLException("Could not find query for "
				+ getClass().getName());
		}

		setQuery(query);
	}
}
