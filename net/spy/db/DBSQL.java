// Copyright (c) 2001  SPY internetworking <dustin@spy.net>
//
// $Id: DBSQL.java,v 1.3 2002/10/30 08:16:34 dustin Exp $

package net.spy.db;

import java.util.Map;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.SQLException;

import net.spy.SpyConfig;

/**
 * Superclass for dynamic SQL calls.
 */
public abstract class DBSQL extends DBSP {

	private Map registeredQueries=null;

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
	 * Prepare the SQL for execution.
	 */
	protected void prepare() throws SQLException {
		if(registeredQueries!=null) {
			selectQuery();
		}
		// Make sure all the arguments are there.
		checkArgs();
		applyArgs(getArguments());
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
		}

		if(query==null) {
			throw new SQLException("Could not find query for "
				+ getClass().getName());
		}

		setQuery(query);
	}
}
