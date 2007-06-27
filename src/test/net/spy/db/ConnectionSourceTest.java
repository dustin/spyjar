// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>

package net.spy.db;

import java.util.concurrent.Callable;

import junit.framework.TestCase;
import net.spy.test.SyncThread;
import net.spy.util.SpyConfig;

/**
 * Test the connection source factory.
 */
public class ConnectionSourceTest extends TestCase {

	private ConnectionSourceFactory csf=null;

	/**
	 * Get an instance of ConnectionSourceTest.
	 */
	public ConnectionSourceTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		csf=ConnectionSourceFactory.getInstance();
	}

	@Override
	protected void tearDown() throws Exception {
		ConnectionSourceFactory.setInstance(null);
		super.tearDown();
	}

	private SpyConfig makeConfig(String className) {
		SpyConfig rv=new SpyConfig();
		rv.put("dbConnectionSource", className);
		return(rv);
	}

	private void checkGoodSource(String s) {
		// Validate calling twice with the same class name returns the same
		// instance.
		assertSame(csf.getConnectionSource(makeConfig(s)),
			csf.getConnectionSource(makeConfig(s)));
	}

	/** 
	 * Test the defined connection source types.
	 */
	public void testDefinedSources() throws Exception {
		checkGoodSource("net.spy.db.JDBCConnectionSource");
		checkGoodSource("net.spy.db.JNDIConnectionSource");
		checkGoodSource("net.spy.db.ObjectPoolConnectionSource");
	}

	/** 
	 * Validate a bad source won't work.
	 */
	public void testBadSource() throws Exception {
		String testSrc="net.spy.db.UndefinedConnectionSource";
		try {
			csf.getConnectionSource(makeConfig(testSrc));
			fail("Shouldn't be able to load this");
		} catch(RuntimeException e) {
			// Expected
			assertNotNull(e.getMessage());
			assertTrue(e.getMessage().startsWith(
				"Cannot initialize connection source: " + testSrc));
		}

		// Try it again, just because
		try {
			csf.getConnectionSource(makeConfig(testSrc));
			fail("Shouldn't be able to load this the second time");
		} catch(RuntimeException e) {
			// Expected
			assertNotNull(e.getMessage());
			assertTrue(e.getMessage().startsWith(
				"Cannot initialize connection source: " + testSrc));
		}
	}

	public void testSingleton() throws Throwable {
		ConnectionSourceFactory.setInstance(null);
		int n=SyncThread.getDistinctResultCount(50, new Callable<Object>() {
			public Object call() {
				return ConnectionSourceFactory.getInstance();
			}});
		assertEquals(1, n);
	}

}
