// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6B85CEEA-1110-11D9-B887-000A957659CC

package net.spy.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;
import java.util.WeakHashMap;

import net.spy.SpyObject;
import net.spy.pool.JDBCPoolFiller;
import net.spy.pool.ObjectPool;
import net.spy.pool.PoolException;
import net.spy.pool.PooledObject;
import net.spy.util.SpyConfig;

/**
 * Connection source to retrieve connections from an ObjectPool.
 *
 * The configuration passed into getConnection() requires the following
 * parameters:
 *
 * <ul>
 *  <li>dbDriverName - Driver to load (i.e. org.postgresql.Driver)</li>
 *  <li>dbSource - dbUser</li>
 *  <li>dbUser - Database username</li>
 *  <li>dbPass - Database password</li>
 * </ul>
 *
 * The following parameters are optional:
 *
 * <ul>
 *  <li>dbPoolName - default: <i>db</i></li>
 *  <li>dbMinConns - minimum number of connections - default 1</li>
 *  <li>dbStartConns - minimum number of connections - default 1</li>
 *  <li>dbYellowLine - the pool's ``yellow line'' percentage
 *      - default 75</li>
 *  <li>dbMaxConns - maximum number of connections - default 5</li>
 *  <li>dbMaxLifeTime - maximum connection lifetime in milliseconds -
 *      default 6 hours</li>
 *  <li>dbPingOnCheckout - if true, ping on object pool checkout
 *      default: true</li>
 * </ul>
 */
public class ObjectPoolConnectionSource extends SpyObject
	implements ConnectionSource {

	private static final int DEFAULT_MAX_CONNS=5;

	// This is the object pool from which connections will be retrieved
	private static ObjectPool pool=null;

	private final Map<Connection, PooledObject> objects;

	// The name of the pool referenced by this instance.
	private String poolName=null;

	/**
	 * Get an instance of SpyPoolConnectionSource.
	 */
	public ObjectPoolConnectionSource() {
		super();
		objects=new WeakHashMap<Connection, PooledObject>();
	}

	/**
	 * @see ConnectionSource
	 */
	public Connection getConnection(SpyConfig conf) throws SQLException {
		// Get the pool name
		poolName=conf.get("dbPoolName");

		// Verify the pool is properly initialized
		initialize(conf);

		Connection conn=null;
		try {
			// Snatch the pebble from my hand.
			conn=getConn(poolName);
		} catch(PoolException pe) {
			getLogger().warn("Could not get a DB connection", pe);
			throw new DBInitException("Could not get a DB connection:  " + pe);
		}
		return(conn);
	}

	// Do the pool work.
	private Connection getConn(String name)
		throws SQLException, PoolException {

		Connection rv=null;
		PooledObject object=pool.getObject(name);
		rv=(Connection)object.getObject();
		objects.put(rv, object);

		return(rv);
	}

	/**
	 * @see ConnectionSource
	 */
	public void returnConnection(Connection conn) {
		PooledObject object=objects.get(conn);
		if(object==null) {
			throw new NullPointerException("Object is null, already returned?");
		}
		object.checkIn();
		objects.remove(conn);
	}

	// Perform one-time initialization for a config.
	private void initialize(SpyConfig gConf)
		throws SQLException {

		synchronized(ObjectPoolConnectionSource.class) {
			// Get a copy of the config, we'll be mangling it a bit
			SpyConfig conf = null;
			if(pool==null) {
				// Clone the config
				conf=(SpyConfig)gConf.clone();
				// Create the ObjectPool
				pool=new ObjectPool(conf);
			} // No pools exist

			// Make sure the ObjectPool has the pool we seek
			if(!pool.hasPool(poolName)) {
				// if we don't have a config yet, clone it
				if(conf==null) {
					conf=(SpyConfig)gConf.clone();
				}
				try {
					createPool(conf);
				} catch(PoolException pe) {
					getLogger().error("Problem initializing pool", pe);
					throw new SQLException("Error initializing pool:  "
						+ pe);
				} // Catch the PoolException
			} // pool doesn't exist
		} // Synchronized block
	}

	// Create the pool described in this config.
	private void createPool(SpyConfig conf)
		throws SQLException, PoolException {

		// get the normalized config
		SpyConfig normConf=getNormalizedConfig(poolName, conf);

		// Get the pool filler.
		JDBCPoolFiller pf=new JDBCPoolFiller(poolName, normConf);

		// Create the pool.
		pool.createPool(poolName, pf);
	}

	// Get a config with all of the expected values for a DB pool
	private SpyConfig getNormalizedConfig(String name, SpyConfig conf)
		throws SQLException {

		// Will return a new instance of SpyConfig.
		SpyConfig rv=new SpyConfig();

		// If a name is given, all properties should be prefixed with that name
		String prefix="";
		if(name!=null) {
			prefix=name+".";
		}

		// Minimum connections in the pool.
		int minConns=conf.getInt("dbMinConns", 1);
		rv.put(prefix + "min", "" + minConns);

		// Start connections
		int startConns=conf.getInt("dbStartCons", minConns);
		rv.put(prefix + "start", "" + startConns);

		// Yellow line percentage
		int yellowLine=conf.getInt("dbYellowLine", -1);
		if(yellowLine>0) {
			rv.put(prefix + "yellow_line", "" + yellowLine);
		}

		// Maximum number of connections in the pool.
		int maxConns=conf.getInt("dbMaxConns", DEFAULT_MAX_CONNS);
		rv.put(prefix + "max", "" + maxConns);

		// Maximum age of any item in the pool
		String tmp=conf.get("dbMaxLifeTime");
		if(tmp==null) {
			tmp="86400";
		}
		rv.put(prefix+"max_age", tmp);

		// ping on checkout flag
		tmp=conf.get("dbPingOnCheckout", "true");
		rv.put(prefix + "pingOnCheckout", tmp);

		// Driver name
		tmp=conf.get("dbDriverName");
		if(tmp==null) {
			throw new SQLException(
				"dbDriverName not given, invalid configuration.");
		}
		rv.put(prefix + "dbDriverName", tmp);

		// JDBC URL
		tmp=conf.get("dbSource");
		if(tmp==null) {
			throw new SQLException(
				"dbSource not given, invalid configuration.");
		}
		rv.put(prefix + "dbSource", tmp);

		// DB username
		tmp=conf.get("dbUser");
		if(tmp==null) {
			throw new SQLException(
				"dbUser not given, invalid configuration.");
		}
		rv.put(prefix + "dbUser", tmp);

		// DB username
		tmp=conf.get("dbPass");
		if(tmp==null) {
			throw new SQLException(
				"dbPass not given, invalid configuration.");
		}
		rv.put(prefix + "dbPass", tmp);

		// Add the db options
		for(Enumeration<?> e=conf.propertyNames(); e.hasMoreElements();) {
			String pname=(String)e.nextElement();
			if(pname.startsWith("dboption.")) {
				String ovalue=conf.get(pname);
				rv.put(prefix + pname, ovalue);
			} // found a dboption
		} // All properties

		return(rv);
	}

}
