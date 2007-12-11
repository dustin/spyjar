// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>

package net.spy.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.spy.SpyObject;

/**
 * Query selection by database product name.
 *
 * This implementation of QuerySelector works by finding the best match for
 * the given Connection by DatabaseMetaData's getDatabaseProductName().
 *
 * <p>
 *
 * The following product names are mapped to their respective names in
 * this implementation:
 *
 * <table border="1">
 *  <tr>
 *   <th>Driver driver name</th><th>Query Name</th>
 *  </tr>
 *  <tr>
 *   <td>PostgreSQL</td><td>pgsql</td>
 *  </tr>
 *  <tr>
 *   <td>Oracle</td><td>oracle</td>
 *  </tr>
 *  <tr>
 *   <td>Microsoft SQL Server</td><td>mssql</td>
 *  </tr>
 *  <tr>
 *   <td>MySQL</td><td>mysql</td>
 *  </tr>
 *  <tr>
 *   <td>DB2 UDB for AS/400</td><td>db2</td>
 *  </tr>
 *  <tr>
 *   <td>Informix Dynamic Server</td><td>informix</td>
 *  </tr>
 *  <tr>
 *   <td>INFORMIX-OnLine</td><td>informix</td>
 *  </tr>
 * </table>
 *
 * </p>
 *
 * @see SpyDB
 */
public class DatabaseMetaDataQuerySelector extends SpyObject
	implements QuerySelector {

	private final SortedMap<String, String> nameMap;

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
				getLogger().debug("Unknown driver:  %s", name);
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
