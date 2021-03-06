/*
 * Copyright (c) 1999  Dustin Sallings <dustin@spy.net>
 */

package net.spy.db;

import java.io.Closeable;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;

import net.spy.SpyObject;
import net.spy.util.SpyConfig;

/**
 * SpyDB is an abstraction of both net.spy.pool and java.sql.
 */
public class SpyDB extends SpyObject implements Closeable {

	// The actual database connection from the PooledObject.
	private Connection conn;

	// Our configuration.
	private SpyConfig conf;

	// Is this thing closed?
	private boolean isClosed=false;

	// The connection source.
	private ConnectionSource source;

	// Exceptions that occur during initialization.
	private DBInitException initializationException;

	/**
	 * Initialization type for SpyDB initialized from a config.
	 */
	protected static final int INIT_FROM_CONFIG=1;

	/**
	 * Initialization type for SpyDB initialized from a Connection.
	 */
	protected static final int INIT_FROM_CONN=2;

	private final int initType;

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
		source=ConnectionSourceFactory.getInstance().getConnectionSource(c);
		init();
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
		init();
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
				source.returnConnection(conn);
				conn=null;
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

	/**
	 * Initialize SpyDB.  This allows any subclasses to perform further
	 * initialization.
	 */
	protected void init() {
		// Subclass initialization
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
			StringBuilder sb=new StringBuilder(in.length());
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
	public ConnectionSource getSource() {
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
