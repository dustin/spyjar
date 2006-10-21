// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6A38711C-1110-11D9-B129-000A957659CC

package net.spy.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.spy.SpyObject;
import net.spy.db.sp.SelectPrimaryKey;
import net.spy.db.sp.UpdatePrimaryKey;
import net.spy.util.CloseUtil;
import net.spy.util.SpyConfig;

/**
 * Primary key generator.  This is an extensible singleton that provides
 * access to a database-backed set of primary keys along with a fetch-ahead
 * cache of those keys.
 *
 * <p>
 *
 * The default implementation assumes you have the following table
 * (<code>primary_key</code>) in the database the configuration defines:
 *
 * </p>
 * <p>
 *
 * <table border="1">
 *  <tr>
 *    <th colspan="3"><font size="+1">primary_keys</font></th>
 *  </tr>
 *  <tr>
 *    <th>Column Name</th>
 *    <th>Column Type</th>
 *    <th>Column Description</th>
 *  </tr>
 *  <tr>
 *    <td>table_name</td>
 *    <td>varchar</td>
 *    <td>The name of the table (or other resource) for which we are
 *        generating the given primary key.  All table names in this column
 *        must be in lowercase as the input from the user will be
 *        lowercased on key retrieval.
 *    </td>
 *  </tr>
 *  <tr>
 *    <td>primary_key</td>
 *    <td>numeric</td>
 *    <td>The next primary key value issued.</td>
 *  </tr>
 *  <tr>
 *    <td>incr_value</td>
 *    <td>numeric</td>
 *    <td>The amount to increment the primary key each time.</td>
 * </table>
 *
 * </p>
 * <p>
 *
 * Other schemas may exist as long as they fit into the ``update something,
 * select something'' model and they operate as described below.
 *
 * </p>
 */
public class GetPK extends SpyObject {

	private static GetPK instance=null;

	private ConcurrentMap<String, KeyStore> caches=null;

	/** 
	 * Constructor for an extensible Singleton.
	 */
	protected GetPK() {
		super();
		caches=new ConcurrentHashMap<String, KeyStore>();
	}

	/** 
	 * Get the instance of GetPK.
	 * 
	 * @return the instance
	 */
	public static synchronized GetPK getInstance() {
		if(instance == null) {
			instance=new GetPK();
		}
		return instance;
	}

	/**
	 * Set the singleton instance.
	 * @param to the new singleton instance
	 */
	public static synchronized void setInstance(GetPK to) {
		instance=to;
	}

	/** 
	 * Get a primary key from the database described in the given config.
	 * 
	 * @param conf the configuration
	 * @param table the table for which the key is needed
	 * @return the key
	 * @throws SQLException if there's a problem getting the key
	 */
	public BigDecimal getPrimaryKey(SpyConfig conf, String table)
		throws SQLException {

		BigDecimal rv=null;
		SpyDB db=new SpyDB(conf);
		try {
			rv=getPrimaryKey(db, table.toLowerCase(),
				makeDbTableKey(conf, table));
		} finally {
			CloseUtil.close(db);
		}
		return(rv);
	}

	// make the key to be used for identifying this table and connection
	// source
	private String makeDbTableKey (SpyConfig conf, String table) {
		StringBuilder rc=new StringBuilder(512);

		// shove in typical stuff
		rc.append(conf.get("dbSource"));
		rc.append(";");
		rc.append(conf.get("dbConnectionSource"));
		rc.append(";");
		rc.append(conf.get("dbDriverName"));
		rc.append(";");
		rc.append(conf.get("dbUser"));
		rc.append(";");
		rc.append(conf.get("dbPass"));
		rc.append(";");
		rc.append(conf.get("dbPoolName"));
		rc.append(";");

		// just a little conf helper in case we ever require this
		rc.append(conf.get("dbPkKey"));
		rc.append(";");

		// and add the table
		rc.append(table);

		return(rc.toString());
	}

	// Get the key (usually from the cache)
	private BigDecimal getPrimaryKey(SpyDB db, String table, String key) 
		throws SQLException {

		BigDecimal rv=null;
		try {
			KeyStore ks=caches.get(key);
			// If we didn't get the key store, go get it now
			if(ks == null) {
				getKeysFromDB(db, table, key);
				ks=caches.get(key);
				if(ks==null) {
					throw new SQLException("Couldn't get initial keys for "
						+ table);
				}
			}
			rv=ks.nextKey();
		} catch(OverDrawnException ode) {
			// Overdrawn, need to fetch the cache.
			caches.remove(key);
			getKeysFromDB(db, table, key);
			// Get the new key store
			KeyStore ks=caches.get(key);
			try {
				rv=ks.nextKey();
			} catch(OverDrawnException ode2) {
				throw new AssertionError(
						"Primary keys not available after load.");
			}
		}

		return(rv);
	}

	/** 
	 * Get the DBSP required for updating the primary key table.
	 *
	 * <p>
	 *
	 * A subclass may override this to change the behavior of the first
	 * part of the ``fetch from db'' stage.  The DBSP returned will take
	 * exactly one parameter:  <code>table_name</code> and will be called
	 * via executeUpdate.  The update must update exactly <i>one</i> row.
	 * Any more or fewer will cause the process to fail and an exception
	 * will be thrown.
	 *
	 * </p>
	 * <p>
	 *
	 * For an example implementation, please see {@link UpdatePrimaryKey}
	 * (this is the default).
	 *
	 * </p>
	 * 
	 * @param conn the connection to use (already in a transaction)
	 * @return the required DBSP
	 * @throws SQLException if there's a problem getting the DBSP
	 */
	protected DBSP getUpdateDBSP(Connection conn) throws SQLException {
		return(new UpdatePrimaryKey(conn));
	}

	/** 
	 * Get the DBSP required for selecting primary key information back out
	 * of the primary key table.
	 *
	 * <p>
	 *
	 * A subclass may override this method to change the behavior of the
	 * select statement that finds the range of results for a table.  The
	 * DBSP returned will take exactly one parameter:
	 * <code>table_name</code> and return a result set containing at least
	 * the following two columns:
	 *
	 * <ul>
	 *  <li><code>first_key</code> - the first key in the range</li>
	 *  <li><code>last_key</code> - the last key in the range</li>
	 * </ul>
	 *
	 * The ResultSet must contain exactly <i>one</i> row.  Any more or
	 * fewer will cause the process to fail and an exception will be
	 * thrown.
	 *
	 * </p>
	 * <p>
	 *
	 * For an example implementation, please see {@link SelectPrimaryKey}
	 * (this is the default).
	 *
	 * <p>
	 * 
	 * @param conn the connection to use (already in a transaction)
	 * @return the required DBSP
	 * @throws SQLException if there's a problem getting the DBSP
	 */
	protected DBSP getSelectDBSP(Connection conn) throws SQLException {
		return(new SelectPrimaryKey(conn));
	}

	// get keys from a database
	private void getKeysFromDB(SpyDB db, String table, String key)
		throws SQLException {

		Connection conn=null;
		boolean complete=false;

		try {
			conn=db.getConn();
			conn.setAutoCommit(false);

			// Update the table first
			DBSP dbsp=getUpdateDBSP(conn);
			dbsp.set("table_name", table);
			int changed=0;
			try {
				changed=dbsp.executeUpdate();
			} finally {
				dbsp.close();
			}
			// Make sure one row got updated
			if(changed==0) {
				throw new SQLException("Incorrect row count for " + table
					+ " (got " + changed + ") - "
					+ "This usually means the primary key table does not have "
					+ table + " or there is a case mismatch.");
			} else if(changed>1) {
				throw new SQLException(
					"Did not update the correct number of rows for "
						+ table + " (got " + changed + ")");
			}

			// Now, fetch it again
			BigDecimal start=null;
			BigDecimal end=null;

			dbsp=getSelectDBSP(conn);
			ResultSet rs=null;
			try {
				dbsp.set("table_name", table);
				rs=dbsp.executeQuery();
				if(!rs.next()) {
					throw new SQLException(
						"No results returned for primary key");
				}
				// Get the beginning and ending of the range
				start=rs.getBigDecimal("first_key");
				end=rs.getBigDecimal("last_key");
				if(rs.next()) {
					throw new SQLException(
						"Too many results returned for primary key");
				}
			} finally {
				// clean up
				if(rs!=null) {
					rs.close();
				}
				dbsp.close();
			}

			KeyStore ks=new KeyStore(start, end);
			getLogger().debug("Got a new keystore for %s:  %s", table, ks);
			// Note that this may blindly remove an existing keystore.  That's
			// considered OK as it'll just be an unexpected burn.
			caches.put(key, ks);

			complete=true;

		} finally {
			if(conn!=null) {
				if(complete) {
					try {
						conn.commit();
					} catch(SQLException e) {
						getLogger().error("Problem committing", e);
					}
				} else {
					try {
						conn.rollback();
					} catch(SQLException e) {
						getLogger().error("Problem rolling back", e);
					}
				}

				// Set autocommit back
				try {
					conn.setAutoCommit(true);
				} catch(SQLException e) {
					getLogger().error("Problem resetting autocommit", e);
				}
			} // got a connection
		} // finally block
	} // getKeysFromDB

}
