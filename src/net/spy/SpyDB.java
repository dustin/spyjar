/*
 * Copyright (c) 1999  Dustin Sallings <dustin@spy.net>
 *
 * arch-tag: 3F2CA2C4-1110-11D9-BBBB-000A957659CC
 */

package net.spy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.StringTokenizer;

import net.spy.db.ConnectionSource;
import net.spy.db.DBInitException;

/**
 * SpyDB is an abstraction of both net.spy.pool and java.sql.
 */

public class SpyDB extends SpyObject {

	// The actual database connection from the PooledObject.
	private Connection conn=null;

	// Our configuration.
	private SpyConfig conf = null;

	// Is this thing closed?
	private boolean isClosed=false;

	// The connection source.
	private ConnectionSource source=null;

	// Exceptions that occur during initialization.
	private DBInitException initializationException=null;

	/** 
	 * Initialization type for SpyDB initialized from a config.
	 */
	protected static final int INIT_FROM_CONFIG=1;

	/** 
	 * Initialization type for SpyDB initialized from a Connection.
	 */
	protected static final int INIT_FROM_CONN=2;

	private int initType=0;

	/**
	 * Create a SpyDB object based on the description found in the passed
	 * in SpyConfig object.
	 *
	 * <p>
	 * 
	 * The configuration may vary greatly depending on the connector.  The
	 * only configuration option for SpyDB itself is
	 * <i>dbConnectionSource</i> which specifies the name of the class that
	 * implements {@link ConnectionSource} that will be providing
	 * connections for this SpyDB instance.  The default is
	 * {@link net.spy.db.ObjectPoolConnectionSource
	    net.spy.db.ObjectPoolConnectionSource}.
	 * You will also need to provide any additional parameters that are
	 * required by the ConnectionSource in this config.
	 *
	 * @param c SpyConfig object describing how to connect.
	 */
	public SpyDB(SpyConfig c) {
		super();
		this.conf=c;

		initType=INIT_FROM_CONFIG;
		initialize();
	}

	/**
	 * Get a SpyDB object wrapping the given connection.
	 *
	 * @param c the connection to wrap.
	 */
	public SpyDB(Connection c) {
		super();
		this.conn=c;
		initType=INIT_FROM_CONN;
	}

	/** 
	 * Get the type of initialization that created this object.
	 * 
	 * @return either INIT_FROM_CONFIG or INIT_FROM_CONN
	 */
	public int getInitType() {
		return(initType);
	}

	/**
	 * Execute a query and return a resultset, will establish a database
	 * connection if necessary.
	 *
	 * @param query SQL query to execute.
	 *
	 * @exception SQLException an exception is thrown if the connection fails,
	 * or the SQL query fails.
	 */
	public ResultSet executeQuery(String query) throws SQLException {
		Connection c=getConn();
		Statement st = c.createStatement();
		ResultSet rs = st.executeQuery(query);
		return(rs);
	}

	/**
	 * Execute a query that doesn't return a ResultSet, such as an update,
	 * delete, or insert.
	 *
	 * @param query SQL query to execute.
	 *
	 * @exception SQLException an exception is thrown if the connection fails,
	 * or the SQL query fails.
	 */
	public int executeUpdate(String query) throws SQLException {
		int rv=0;
		Connection c=getConn();
		Statement st = c.createStatement();
		rv=st.executeUpdate(query);
		return(rv);
	}

	/**
	 * Prepare a statement.
	 *
	 * @param query SQL query to prepare.
	 *
	 * @exception SQLException thrown if something bad happens.
	 */
	public PreparedStatement prepareStatement(String query)
		throws SQLException {

		Connection c=getConn();
		PreparedStatement pst = c.prepareStatement(query);
		return(pst);
	}

	/**
	 * Prepare a callable statement.
	 *
	 * @param query SQL query to prepare for call.
	 *
	 * @exception SQLException thrown if something bad happens.
	 */
	public CallableStatement prepareCall(String query)
		throws SQLException {

		Connection c=getConn();
		CallableStatement cst = c.prepareCall(query);
		return(cst);
	}

	/**
	 * Get a connection out of the pool.  A given SpyDB object can only
	 * maintain a single database connection, so if multiple connections
	 * from the pool are needed, multiple SpyDB objects will be required.
	 *
	 * @exception SQLException An exception may be thrown if a database
	 * connection cannot be obtained.
	 */
	public Connection getConn() throws SQLException {
		if(conn==null) {
			// log("New connection");
			getDBConn();
		}
		return(conn);
	}

	/**
	 * Free an established database connection.  The connection is whatever
	 * connection has already been instablished by this instance of the
	 * object.  If a connection has not been established, this does
	 * nothing.
	 */
	public void freeDBConn() {
		// Don't touch it unless it came from a ConnectionSource
		if(source!=null) {
			// If there's a source, this came from something that will want
			// to hear that we're done with it.
			if(conn!=null) {
				try {
					source.returnConnection(conn);
				} catch(SQLException e) {
					getLogger().warn("Problem returning connection to pool", e);
				}
			}
		}
		isClosed=true;
	}

	/**
	 * Free an established database connection - alias to freeDBConn()
	 */
	public void close() {
		freeDBConn();
	}

	private void initialize() {
		String connectionClassName=conf.get("dbConnectionSource",
			"net.spy.db.ObjectPoolConnectionSource");
		// Backwards compatibilify 
		if(connectionClassName == null) {
			String tmp=conf.get("dbPoolType");
			if(tmp!=null) {
				if(tmp.equals("jndi")) {
					connectionClassName="net.spy.db.JNDIConnectionSource";
				} else if(tmp.equals("none")) {
					connectionClassName="net.spy.db.JDBCConnectionSource";
				} else {
					// My pool
					connectionClassName="net.spy.db.ObjectPoolConnectionSource";
				}
			}
		}
		// OK, we now know *how* we're going to get connections, let's get
		// the source object.
		try {
			Class connectionSourceClass=Class.forName(connectionClassName);
			source=(ConnectionSource)connectionSourceClass.newInstance();
		} catch(Exception e) {
			e.fillInStackTrace();
			getLogger().error("Problem initializing spydb", e);
			initializationException=new DBInitException(
				"Initialization exception:  " + e);
			initializationException.fillInStackTrace();
		}
		init();
	}

	/**
	 * Initialize SpyDB.  This allows any subclasses to perform further
	 * initialization.
	 */
	protected void init() {
	}

	// This is a debug routine
	private void log(String msg) {
		// System.err.println("DB:  " + msg);
	}

	// Actually dig up a DB connection
	private void getDBConn() throws SQLException {
		// Different behavior whether we're using a pool or not

		if(initializationException!=null) {
			throw initializationException;
		}

		// Get the connection from the source.
		conn=source.getConnection(conf);
	}

	/**
	 * Make a string safe for usage in a SQL query, quoting apostrophies,
	 * etc...
	 *
	 * @param in the string that needs to be quoted
	 *
	 * @return a new, quoted string
	 */
	public static String dbquoteStr(String in) {
		// Quick...handle null
		if(in == null) {
			return(null);
		}

		String sout = null;
		if(in.indexOf('\'') < 0) {
			sout = in;
		} else {
			StringBuffer sb=new StringBuffer(in.length());
			StringTokenizer st = new StringTokenizer(in, "\'", true);
			while(st.hasMoreTokens()) {
				String part = st.nextToken();
				sb.append(part);
				// If this is a quote, add another one
				if(part.equals("'")) {
					sb.append('\'');
				}
			}
			sout=sb.toString();
		}
		return(sout);
	}

	/**
	 * Has close() been called?
	 */
	public boolean isClosed() {
		return(isClosed);
	}

	/** 
	 * Get the source of connections.
	 * 
	 * @return a ConnectionSource instance, or null if this SpyDB instance
	 * 			was created from a config
	 */
	protected ConnectionSource getSource() {
		return(source);
	}

	/** 
	 * Get the configuration from which this SpyDB was instatiated.
	 * 
	 * @return the config, or null if the SpyDB was created from a
	 * 			connection
	 */
	protected SpyConfig getConfig() {
		return(conf);
	}

}
