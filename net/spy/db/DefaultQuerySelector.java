// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// $Id: DefaultQuerySelector.java,v 1.2 2002/10/30 08:16:34 dustin Exp $

package net.spy.db;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import java.sql.Connection;

import net.spy.SpyConfig;

/**
 * Default implementation of query selector.
 *
 * This implementation of QuerySelector works by finding the best match for
 * the given Connection by package name.  If given a SpyConfig, it looks
 * for either an entry called <code>queryName</code> or a prefix match
 * against the driver name as specified for SpyDB.
 *
 * @see net.spy.SpyDB
 */
public class DefaultQuerySelector extends Object implements QuerySelector {

	private SortedMap nameMap=null;

	/** 
	 * Get an instance of DefaultQuerySelector.
	 * This should really only be called from QuerySelectorFactory
	 */
	public DefaultQuerySelector() {
		super();
		nameMap=new TreeMap();
		initNameMap();
	}

	/** 
	 * Initialize the prefix to name map.
	 */
	protected void initNameMap() {
		registerNameMapping("org.postgresql.", "pgsql");
	}

	/** 
	 * Register a prefix -&gt; name mapping.
	 * 
	 * @param prefix the prefix to match for a name
	 * @param name the name
	 */
	protected void registerNameMapping(String prefix, String name) {
		nameMap.put(prefix, name);
	}

	/** 
	 * @see QuerySelector
	 */
	public String getQuery(Connection conn, Map queryMap) {
		return(getQuery(conn.getClass().getName(), queryMap));
	}

	/** 
	 * @see QuerySelector
	 */
	public String getQuery(SpyConfig conf, Map queryMap) {
		String rv=null;

		String tmp=conf.get("queryName");
		if (tmp != null) {
			rv=(String)queryMap.get(tmp);
			if (rv==null) {
				rv=(String)queryMap.get(DEFAULT_QUERY);
			}
		} else {
			tmp=conf.get("dbDriverName");
			if(tmp!=null) {
				rv=getQuery(tmp, queryMap);
			}
		}

		return (rv);
	}

	/** 
	 * Attempt to get a query in the given map by a name.
	 * 
	 * @param name the name against which to search
	 * @param queryMap the map containing the queries
	 * @return the query, or null if one cannot be found
	 */
	protected String getQuery(String name, Map queryMap) {
		String rv=null;

		// First, check to see if the named query is in the map
		rv=(String)queryMap.get(name);
		if(rv == null) {
			// Next, check to see if the name is in our translation map
			String tmp=(String)nameMap.get(name);

			// If we didn't get a key directly, try a fuzzy search in the Map
			if(tmp == null) {
				SortedMap h=nameMap.headMap(name);
				String key=(String)h.lastKey();
				if(name.startsWith(key)) {
					tmp=(String)nameMap.get(key);
				}
			} // not in the name map

			// If we still don't have a key, use the default key
			if(tmp == null) {
				tmp=DEFAULT_QUERY;
			}
			rv=(String)queryMap.get(tmp);

			// If there wasn't a match, try default
			if(rv == null) {
				tmp=DEFAULT_QUERY;
				rv=(String)queryMap.get(tmp);
			}
		} // not in query Map

		return (rv);
	}

}
