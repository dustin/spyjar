// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: DBTest.java,v 1.5 2002/09/13 05:49:09 dustin Exp $

package net.spy.test;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import java.math.BigDecimal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.spy.db.ConnectionSource;
import net.spy.db.DBSP;
import net.spy.db.ObjectPoolConnectionSource;
import net.spy.db.SpyCacheDB;
import net.spy.SpyConfig;
import net.spy.SpyDB;

import net.spy.test.db.DumpTestTable;
import net.spy.test.db.GetTestByNumber;
import net.spy.test.db.CallTestFunc;
import net.spy.test.db.ThreeColumnTest;

/**
 * Test various DB functionality.
 */
public class DBTest extends TestCase {

	private SpyConfig conf=null;

	/**
	 * Get an instance of DBTest.
	 */
	public DBTest(String name) {
		super(name);
		conf=new SpyConfig(new java.io.File("test.conf"));
	}

	/**
	 * Get this test suite.
	 */
	public static Test suite() {
		return new TestSuite(DBTest.class);
	}

	/**
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	private void checkRow(ResultSet rs) throws SQLException {
		int id=rs.getInt("id");
		String test_vc=rs.getString("test_vc");
		Date d=rs.getDate("test_d");
		Time t=rs.getTime("test_t");
		Timestamp ts=rs.getTimestamp("test_ts");
		BigDecimal n=rs.getBigDecimal("test_n");
		int i=rs.getInt("test_i");
		boolean iWasNull=rs.wasNull();
		float test_f=rs.getFloat("test_f");
		boolean fWasNull=rs.wasNull();
		boolean test_b=rs.getBoolean("test_b");
		boolean bWasNull=rs.wasNull();

		if(test_vc.equals("full1") || test_vc.equals("full2")) {
			assertNotNull(d);
			assertNotNull(t);
			assertNotNull(ts);
			assertNotNull(n);
			assertEquals(iWasNull, false);
			assertEquals(fWasNull, false);
			assertEquals(bWasNull, false);
		} else if(test_vc.equals("nulldate")) {
			assertNull(d);
			assertNotNull(t);
			assertNotNull(ts);
			assertNotNull(n);
			assertEquals(iWasNull, false);
			assertEquals(fWasNull, false);
			assertEquals(bWasNull, false);
		} else if(test_vc.equals("nullbool")) {
			assertNotNull(d);
			assertNotNull(t);
			assertNotNull(ts);
			assertNotNull(n);
			assertEquals(iWasNull, false);
			assertEquals(fWasNull, false);
			assertEquals(bWasNull, true);
		} else if(test_vc.equals("nullnums")) {
			assertNotNull(d);
			assertNotNull(t);
			assertNotNull(ts);
			assertNotNull(n);
			assertEquals(iWasNull, true);
			assertEquals(fWasNull, true);
			assertEquals(bWasNull, false);
		} else {
			fail("Unexpected result type:  " + test_vc);
		}
	}

	/**
	 * Test a regular DB thingy.
	 */
	public void testDBNoCache() throws SQLException {
		SpyCacheDB db=new SpyCacheDB(conf);

		ResultSet rs=db.executeQuery("select * from testtable");
		assertTrue(! (rs instanceof net.spy.db.CachedResultSet));
		while(rs.next()) {
			checkRow(rs);
		}
		rs.close();
		db.close();
	}

	/**
	 * Test a DB connection over multiple pools.
	 */
	public void testMultiplePools() throws SQLException {
		SpyCacheDB db=new SpyCacheDB(conf);

		// Do the query
		ResultSet rs=db.executeQuery("select * from testtable");
		// Verify the pool name
		assertEquals("TestPool", getPoolName(db));
		assertTrue(! (rs instanceof net.spy.db.CachedResultSet));
		while(rs.next()) {
			checkRow(rs);
		}
		rs.close();
		db.close();

		// Do the same thing on a different pool.
		SpyConfig conf2=(SpyConfig)conf.clone();
		conf2.setProperty("dbPoolName", "TestPoolTwo");
		db=new SpyCacheDB(conf2);
		// Do the query
		rs=db.executeQuery("select * from testtable");
		// Verify the pool name
		assertEquals("TestPoolTwo", getPoolName(db));
		assertTrue(! (rs instanceof net.spy.db.CachedResultSet));
		while(rs.next()) {
			checkRow(rs);
		}
		rs.close();
		db.close();

		// Do the it on the first pool again
		db=new SpyCacheDB(conf);

		// Do the query
		rs=db.executeQuery("select * from testtable");
		// Verify the pool name
		assertEquals("TestPool", getPoolName(db));
		assertTrue(! (rs instanceof net.spy.db.CachedResultSet));
		while(rs.next()) {
			checkRow(rs);
		}
		rs.close();
		db.close();
	}

	// Deterine the pool name for a given pool.
	private String getPoolName(SpyDB db)
	{
		String rv=null;

		try {
			// Arg types for the method
			Class argTypes[]=new Class[0];
			Object args[]=new Object[0];
			// Find the source
			Method sm=SpyDB.class.getDeclaredMethod("getSource", argTypes);
			// Grant ourselves access to it
			sm.setAccessible(true);
			// Get the connection source from the field
			ConnectionSource source=(ConnectionSource)sm.invoke(db, args);
			assertTrue(source instanceof ObjectPoolConnectionSource);

			ObjectPoolConnectionSource opcs=(ObjectPoolConnectionSource)source;
			// Find the field
			Field pnf=opcs.getClass().getDeclaredField("poolName");
			// Grant ourselves access to it
			pnf.setAccessible(true);
			// Get the value
			rv=(String)pnf.get(opcs);

			// System.err.println("poolName is " + rv);
		} catch(Exception e) {
			e.printStackTrace();
			fail("Couldn't get pool name for DB object:  " + e);
		}

		return(rv);
	}

	/**
	 * Test a cached query.
	 */
	public void testDBCache() throws SQLException {
		SpyCacheDB db=new SpyCacheDB(conf);

		ResultSet rs=db.executeQuery("select * from testtable", 30);
		assertTrue(rs instanceof net.spy.db.CachedResultSet);
		while(rs.next()) {
			checkRow(rs);
		}
		rs.close();
		db.close();
	}

	/**
	 * Test an SPT with no cache.
	 */
	public void testSPTNoCache() throws SQLException {
		DumpTestTable db=new DumpTestTable(conf);
		ResultSet rs=db.executeQuery();
		assertTrue(! (rs instanceof net.spy.db.CachedResultSet));
		while(rs.next()) {
			checkRow(rs);
		}
		rs.close();
		db.close();
	}

	/**
	 * Test an SPT with cache.
	 */
	public void testSPTWithCache() throws SQLException {
		DumpTestTable db=new DumpTestTable(conf);
		db.setCacheTime(30);
		ResultSet rs=db.executeQuery();
		assertTrue(rs instanceof net.spy.db.CachedResultSet);
		while(rs.next()) {
			checkRow(rs);
		}
		rs.close();
		db.close();
	}

	/** 
	 * Test an SPT with early-binding numeric parameters.
	 */
	public void testSPTNoCacheWithEarlyBindingNumberParam()
		throws SQLException {

		GetTestByNumber db=new GetTestByNumber(conf);
		db.setTestN(new BigDecimal(1234567));
		ResultSet rs=db.executeQuery();
		assertTrue(! (rs instanceof net.spy.db.CachedResultSet));
		int nrows=0;
		while(rs.next()) {
			nrows++;
			checkRow(rs);
		}
		rs.close();
		db.close();

		assertEquals("Incorrect number of rows returned for number match",
			nrows, 2);
	}


	/** 
	 * Test an SPT with numeric parameters.
	 */
	public void testSPTNoCacheWithNumberParam() throws SQLException {
		GetTestByNumber db=new GetTestByNumber(conf);
		db.set("test_n", new BigDecimal(1234567));
		ResultSet rs=db.executeQuery();
		assertTrue(! (rs instanceof net.spy.db.CachedResultSet));
		int nrows=0;
		while(rs.next()) {
			nrows++;
			checkRow(rs);
		}
		rs.close();
		db.close();

		assertEquals("Incorrect number of rows returned for number match",
			nrows, 2);
	}

	/** 
	 * Test an SPT with numeric parameters.
	 */
	public void testSPTNoCacheWithCoercedNumberParam() throws SQLException {
		GetTestByNumber db=new GetTestByNumber(conf);
		db.setCoerced("test_n", "1234567");
		ResultSet rs=db.executeQuery();
		assertTrue(! (rs instanceof net.spy.db.CachedResultSet));
		int nrows=0;
		while(rs.next()) {
			nrows++;
			checkRow(rs);
		}
		rs.close();
		db.close();

		assertEquals("Incorrect number of rows returned for number match",
			nrows, 2);
	}

	/** 
	 * Test DBCP functionality.
	 */
	public void testDBCP() throws SQLException {
		CallTestFunc gtf=new CallTestFunc(conf);
		gtf.set("num", 5);
		ResultSet rs=gtf.executeQuery();
		assertTrue("Too few results", rs.next());
		int rv=rs.getInt(1);
		assertEquals(rv, 6);
		assertTrue("Too many results", (!rs.next()));
		rs.close();
		gtf.close();
	}

	// Common stuff for testThreeColumns
	private void threeColumnTester(
		DBSP db, int first, int second, String third) throws SQLException {
	
		db.set("first", first);
		db.set("second", second);
		db.set("third", third);

		ResultSet rs=db.executeQuery();
		if(!rs.next()) {
			fail("No results returned");
		}

		assertEquals("first", rs.getInt("first"), first);
		assertEquals("second", rs.getInt("second"), second);
		assertEquals("third", rs.getString("third"), third);

		if(rs.next()) {
			fail("Too many results returned.");
		}

		rs.close();
	}

	/** 
	 * Test a DBSP call with three columns.
	 */
	public void testThreeColumns() throws SQLException {
		ThreeColumnTest db=new ThreeColumnTest(conf);

		threeColumnTester(db, 24, 7, "three sixty-five");
		threeColumnTester(db, 8, 10, "or wallet sized");
		threeColumnTester(db, 9, 5, "what a way to make a livin'");

		db.close();
	}

	// Common stuff for testThreeColumnsShuffled
	private void threeColumnTesterShuffled(
		DBSP db, int first, int second, String third) throws SQLException {
	
		db.set("third", third);
		db.set("second", second);
		db.set("first", first);

		ResultSet rs=db.executeQuery();
		if(!rs.next()) {
			fail("No results returned");
		}

		assertEquals("first", rs.getInt("first"), first);
		assertEquals("second", rs.getInt("second"), second);
		assertEquals("third", rs.getString("third"), third);

		if(rs.next()) {
			fail("Too many results returned.");
		}

		rs.close();
	}

	/** 
	 * Test a DBSP call with three columns with out of sequence parameters.
	 */
	public void testThreeColumnsShuffled() throws SQLException {
		ThreeColumnTest db=new ThreeColumnTest(conf);

		threeColumnTesterShuffled(db, 24, 7, "three sixty-five");
		threeColumnTesterShuffled(db, 8, 10, "or wallet sized");
		threeColumnTesterShuffled(db, 9, 5, "what a way to make a livin'");

		db.close();
	}
	
	// Common stuff for testThreeColumnsEarlyBinding
	private void threeColumnTesterEarlyBinding(
		ThreeColumnTest db, int first, int second, String third)
		throws SQLException {
	
		db.setFirst(first);
		db.setSecond(second);
		db.setThird(third);

		ResultSet rs=db.executeQuery();
		if(!rs.next()) {
			fail("No results returned");
		}

		assertEquals("first", rs.getInt("first"), first);
		assertEquals("second", rs.getInt("second"), second);
		assertEquals("third", rs.getString("third"), third);

		if(rs.next()) {
			fail("Too many results returned.");
		}

		rs.close();
	}

	/** 
	 * Test a DBSP call with three columns with early-binding parameters.
	 */
	public void testThreeColumnsEarlyBinding() throws SQLException {
		ThreeColumnTest db=new ThreeColumnTest(conf);

		threeColumnTesterEarlyBinding(db, 24, 7, "three sixty-five");
		threeColumnTesterEarlyBinding(db, 8, 10, "or wallet sized");
		threeColumnTesterEarlyBinding(db, 9, 5, "what a way to make a livin'");

		db.close();
	}

}
