// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6AE74D60-1110-11D9-8842-000A957659CC

package net.spy.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import net.spy.SpyObject;
import net.spy.util.SpyConfig;

/**
 * Connection source for getting DB connections from JNDI sources.
 */
public class JNDIConnectionSource extends SpyObject
	implements ConnectionSource {

	/**
	 * Get an instance of JNDIConnectionSource.
	 */
	public JNDIConnectionSource() {
		super();
	}

	/**
	 * This will get a connection to the JNDI resource.
	 *
	 * The following parameters are required:
	 * <li>
	 *  <ul>dbSource - the JNDI resource containing the connection
	 * </li>
	 *
	 * Additional parameters will be passed to {@link InitialContext} after
	 * being converted to a {@link Hashtable} object.  Note:  It will not
	 * be cast to Hashtable, but instead a new Hashtable instance will be
	 * created with those parameters.  This allows you to get connections
	 * from a JNDI context that doesn't know about {@link SpyConfig}.
	 *
	 * @param conf  The configuration to use for getting the database
	 * source
	 * @return A connection to the JNDI datasource
	 * @throws SQLException  
	 * @see ConnectionSource
	 */
	public Connection getConnection(SpyConfig conf) throws SQLException {
		String source=conf.get("dbSource");

		Connection conn=null;
		try {
			// we need to convert the SpyConfig to a Hashtable since
			// the receiving end may not know what a SpyConfig is, but it
			// should surely know what a Hashtable is as that's what it's
			// expecting per the javadocs.
			@SuppressWarnings("unchecked")
			Hashtable env=new Hashtable(conf);

			Context initial = new InitialContext(env);
			DataSource dsrc = (DataSource)initial.lookup(source);
			conn = dsrc.getConnection();
		} catch(NamingException e) {
			getLogger().warn("Error getting connection from JNDI", e);
			throw new SQLException("Error getting connection from JNDI:  " + e);
		}

		return(conn);
	}

	/**
	 * @see ConnectionSource
	 */
	public void returnConnection(Connection conn) {
		try {
			conn.close();
		} catch(SQLException e) {
			getLogger().warn("Problem closing connection", e);
		}
	}

}
