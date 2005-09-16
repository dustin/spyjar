// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 2BE84A2E-8A66-42BE-80BC-FC0859D57B9D

package net.spy.db;

import java.sql.Connection;
import java.sql.SQLException;
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
 * @see SpyDB
 */
public class DatabaseMetaDataQuerySelector extends Object
	implements QuerySelector {

	private SortedMap<String, String> nameMap=null;

	/** 
	 * Get an instance of DatabaseMetaDataQuerySelector.
	 * This should really only be called from QuerySelectorFactory
	 */
	public DatabaseMetaDataQuerySelector() {
		super();
		nameMap=new TreeMap<String, String>();
		initNameMap();
	}

	/** 
	 * Initialize the prefix to name map.
	 */
	protected void initNameMap() {
		registerNameMapping("PostgreSQL", "pgsql");
		registerNameMapping("Oracle", "oracle");
		registerNameMapping("Microsoft SQL Server", "mssql");
		registerNameMapping("MySQL", "mysql");
		registerNameMapping("DB2 UDB for AS/400", "db2");
		registerNameMapping("Informix Dynamic Server", "informix");
		registerNameMapping("INFORMIX-OnLine", "informix");
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
	public String getQuery(Connection conn, Map<String, String> queryMap)
		throws SQLException {
		return(getQuery(conn.getMetaData().getDatabaseProductName(), queryMap));
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

			// If we don't have a key, use the default key
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
