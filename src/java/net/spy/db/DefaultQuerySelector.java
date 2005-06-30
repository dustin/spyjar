// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 69674B1C-1110-11D9-A825-000A957659CC

package net.spy.db;

import java.sql.Connection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import net.spy.util.SpyConfig;

/**
 * Default implementation of query selector.
 *
 * This implementation of QuerySelector works by finding the best match for
 * the given Connection by package name.  If given a SpyConfig, it looks
 * for either an entry called <code>queryName</code> or a prefix match
 * against the driver name as specified for SpyDB.
 *
 * <p>
 *
 * The following driver prefixes are mapped to their respective names in
 * this implementation:
 *
 * <table border="1">
 *  <tr>
 *   <th>Driver class prefix</th><th>Query Name</th><th>Driver Provider</th>
 *  </tr>
 *  <tr>
 *   <td>org.postgresql.</td><td>pgsql</td><td>http://www.postgresql.org</td>
 *  </tr>
 *  <tr>
 *   <td>oracle.jdbc.driver.</td><td>oracle</td><td>http://www.oracle.com</td>
 *  </tr>
 *  <tr>
 *   <td>weblogic.jdbc.oci.</td><td>oracle</td><td>http://www.bea.com</td>
 *  </tr>
 *  <tr>
 *   <td>com.ashna.jturbo.</td><td>mssql</td><td>http://www.newatlanta.com/</td>
 *  </tr>
 * </table>
 *
 * </p>
 *
 * @see net.spy.SpyDB
 */
public class DefaultQuerySelector extends Object implements QuerySelector {

	private SortedMap<String, String> nameMap=null;

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
		// Postgres
		registerNameMapping("org.postgresql.", "pgsql");

		// Oracle
		registerNameMapping("oracle.jdbc.driver.", "oracle");

		// Weblogic's Oracle jDrvier
		registerNameMapping("weblogic.jdbc.oci.", "oracle");

		// Micro$oft MSSQL from New Atlanta
		registerNameMapping("com.ashna.jturbo.", "mssql");
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
	public String getQuery(Connection conn, Map<String, String> queryMap) {
		return(getQuery(conn.getClass().getName(), queryMap));
	}

	/** 
	 * @see QuerySelector
	 */
	public String getQuery(SpyConfig conf, Map<String, String> queryMap) {
		String rv=null;

		// Find out if there's an explicit mapping in the config
		String tmp=conf.get("queryName");

		if (tmp != null) {
			// If there is, use it.
			rv=queryMap.get(tmp);
		} else {
			// If there's not, base it on the driver name
			tmp=conf.get("dbDriverName");
			if(tmp!=null) {
				rv=getQuery(tmp, queryMap);
			}
		}

		// If we didn't find anything, use the default.
		if (rv==null) {
			rv=queryMap.get(DEFAULT_QUERY);
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
	protected String getQuery(String name, Map<String, String> queryMap) {
		String rv=null;

		// First, check to see if the named query is in the map
		rv=queryMap.get(name);
		if(rv == null) {
			// Next, check to see if the name is in our translation map
			String tmp=nameMap.get(name);

			// If we didn't get a key directly, try a fuzzy search in the Map
			if(tmp == null) {
				try {
					SortedMap<String, String> h=nameMap.headMap(name);
					String key=h.lastKey();
					if(name.startsWith(key)) {
						tmp=nameMap.get(key);
					}
				} catch(NoSuchElementException e) {
					// It wasn't there, and nothing like it was there, move
					// along
				}
			} // not in the name map

			// If we still don't have a key, use the default key
			if(tmp == null) {
				tmp=DEFAULT_QUERY;
			}
			rv=queryMap.get(tmp);

			// If there wasn't a match, try default
			if(rv == null) {
				tmp=DEFAULT_QUERY;
				rv=queryMap.get(tmp);
			}
		} // not in query Map

		return (rv);
	}

}
