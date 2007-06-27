// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>

package net.spy.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.spy.util.SpyConfig;

import org.jmock.Mock;

/** 
 * Base class for mock connection sources.
 */
public abstract class MockConnectionSource extends Object
	implements ConnectionSource {

	private Map<Object, Mock> mocks=null;

	public MockConnectionSource() {
		super();
		mocks=new HashMap<Object, Mock>();
	}

	protected void registerMock(Mock m) {
		mocks.put(m.proxy(), m);
	}

	public Mock getMock(Object proxy) {
		return mocks.get(proxy);
	}

	public void clearSeenObjects() {
		mocks.clear();
	}

	public Collection<?> getSeenObjects() {
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
		for(Iterator<?> i=getSeenObjects().iterator(); i.hasNext(); ) {
			Mock m=(Mock)i.next();
			m.verify();
		}
		clearSeenObjects();
	}
}
