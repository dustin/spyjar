// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 0973438E-C3AF-4BFB-9112-811A68ECD92E

package net.spy.test;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

import net.spy.util.CloseUtil;

/**
 * Test the various CloseUtil functions.
 */
public class CloseUtilTest extends MockObjectTestCase {

	private Object getSuccess(Class<?> c) {
		Mock m=mock(c);
		m.expects(once()).method("close");
		return(m.proxy());
	}

	private Object getFailure(Class<?> c, Class<? extends Throwable> e)
		throws Exception {
		Mock m=mock(c);
		m.expects(once()).method("close").will(throwException(e.newInstance()));
		return(m.proxy());
	}

	public void testCloseableClose() throws Exception {
		CloseUtil.close((Closeable)getSuccess(Closeable.class));
	}

	public void testCloseableCloseException() throws Exception {
		CloseUtil.close((Closeable)getFailure(Closeable.class,
				IOException.class));
	}

	public void testConnectionClose() throws Exception {
		CloseUtil.close((Connection)getSuccess(Connection.class));
	}

	public void testConnectionCloseException() throws Exception {
		CloseUtil.close((Connection)getFailure(
				Connection.class, SQLException.class));
	}
}
