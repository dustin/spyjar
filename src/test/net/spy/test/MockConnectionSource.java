// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: F47D073E-3B67-45E8-8632-5B6043F7FD09

package net.spy.test;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;

import java.sql.Connection;
import java.sql.SQLException;

import org.jmock.Mock;

import net.spy.util.SpyConfig;
import net.spy.db.ConnectionSource;

/** 
 * Base class for mock connection sources.
 */
public abstract class MockConnectionSource extends Object
	implements ConnectionSource {

	private Map mocks=null;

	public MockConnectionSource() {
		super();
		mocks=new HashMap();
	}

	protected void registerMock(Mock m) {
		mocks.put(m.proxy(), m);
	}

	public Mock getMock(Object proxy) {
		return((Mock)mocks.get(proxy));
	}

	public void clearSeenObjects() {
		mocks.clear();
	}

	public Collection getSeenObjects() {
		return(mocks.values());
	}

	/** 
	 * This method will be called with the connection source to set up the
	 * expectations of the source.
	 */
	protected abstract void setupMock(Mock connMock, SpyConfig conf);

	public Connection getConnection(SpyConfig conf) throws SQLException {
		Mock connMock=new Mock(Connection.class);

		setupMock(connMock, conf);

		registerMock(connMock);

		return((Connection)connMock.proxy());
	}

	public void returnConnection(Connection conn) {
		try {
			conn.close();
		} catch(SQLException e) {
			throw new RuntimeException(
				"jmock threw a SQLException on close", e);
		}
	}

	/** 
	 * Verify all of the connections that were handed out.
	 */
	public void verifyConnections() throws Exception {
		for(Iterator i=getSeenObjects().iterator(); i.hasNext(); ) {
			Mock m=(Mock)i.next();
			m.verify();
		}
		clearSeenObjects();
	}
}
