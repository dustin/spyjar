// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: B9EACADC-407D-454E-AEAE-3EC3272046B4

package net.spy.db;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

import junit.framework.TestCase;
import net.spy.test.db.ClobTest;
import net.spy.test.db.DeleteTest;
import net.spy.test.db.DumpTestTable;
import net.spy.test.db.ImplTest;
import net.spy.test.db.ThreeColumnTest;
import net.spy.util.SpyConfig;

public class FileDBTest extends TestCase {

	private FileDriver fd=null;
	private String url=FileDriver.URL_PREFIX + "/x";
	private String url2=FileDriver.URL_PREFIX + "/x2";
	private SpyConfig conf=null;
	private SpyConfig conf2=null;
	private SpyConfig poolConf=null;

	@Override
	protected void setUp() throws Exception {
		Class.forName("net.spy.db.FileDriver");
		fd=(FileDriver)DriverManager.getDriver(url);

		// Config using JDBC directly.
		conf=new SpyConfig();
		conf.put("dbConnectionSource", "net.spy.db.JDBCConnectionSource");
		conf.put("dbDriverName", "net.spy.db.FileDriver");
		conf.put("dbSource", url);
		conf.put("dbUser", "username");
		conf.put("dbPass", "password");

		conf2=new SpyConfig();
		conf2.put("dbConnectionSource", "net.spy.db.JDBCConnectionSource");
		conf2.put("dbDriverName", "net.spy.db.FileDriver");
		conf2.put("dbSource", url2);
		conf2.put("dbUser", "username");
		conf2.put("dbPass", "password");

		// Config using the DB pool
		poolConf=new SpyConfig();
		poolConf.put("dbDriverName", "net.spy.db.FileDriver");
		poolConf.put("dbSource", url);
		poolConf.put("dbUser", "username");
		poolConf.put("dbPass", "password");
		poolConf.put("dbPoolName", "TestPool");

		// Register up a couple of queries.
		DumpTestTable dtt=new DumpTestTable(conf);
		ThreeColumnTest ttt=new ThreeColumnTest(conf);
		DeleteTest dt=new DeleteTest(conf);

		fd.registerQuery(url, dtt, new Object[0], getPath("dumptest.txt"));
		fd.registerQuery(url, ttt,
			new Object[]{new Integer(1), new Integer(2), "string"},
			getPath("threecol.txt"));
		fd.registerQuery(url, ttt,
			new Object[]{new Integer(1), new Integer(3), "string"},
			getPath("threecol2.txt"));
		fd.registerQuery(url, ttt,
			new Object[]{null, new Integer(3), null},
			getPath("threecol.txt"));
		fd.registerUpdate(url, dt, new Object[]{new Integer(11)}, 11);
		fd.registerUpdate(url, dt, new Object[]{new Integer(13)}, 13);
		
		fd.registerUpdate(url2, dt, new Object[]{new Integer(26)}, 26);

		fd.registerUpdate(url, new ClobTest(conf), new Object[]{"test"}, 1);
	}

	@Override
	protected void tearDown() {
		fd.clearQueries();
	}

	private URL getPath(String p) throws Exception {
		return(new URL("file://" + System.getProperty("basedir")
			+ "/src/test/net/spy/test/db/" + p));
	}

	public void testFileDB() throws Exception {
		fd.registerQuery(url,
			"select count(*) from table", getPath("counttest.txt"));
		Connection conn=DriverManager.getConnection(url);
		PreparedStatement pst=conn.prepareStatement(
			"select count(*) from table");
		ResultSet rs=pst.executeQuery();
		assertTrue(rs.next());
		assertEquals(8325, rs.getInt("count"));
		assertFalse(rs.next());
		rs.close();
		conn.close();
	}

	public void testLimitedFileDB() throws Exception {
		fd.registerQuery(url,
				"select * from something", getPath("resulttest.txt"));
		Connection conn=DriverManager.getConnection(url);
		PreparedStatement pst=conn.prepareStatement(
			"select * from something");
		pst.setMaxRows(2);
		ResultSet rs=pst.executeQuery();
		int i=0;
		while(rs.next()) {
			i++;
		}
		assertEquals(2, i);
		rs.close();
		conn.close();
	}

	public void testMissingUpdate() throws Exception {
		Connection conn=DriverManager.getConnection(url);
		try {
			PreparedStatement pst=conn.prepareStatement("delete from blah");
			pst.executeUpdate();
			fail("Should not run unregistered query:  delete from blah");
		} catch(SQLException e) {
			assertTrue(e.getMessage().startsWith("No mapping registered "));
		} finally {
			conn.close();
		}
	}
	
	public void testMissingSPT() throws Exception {
		// This same query is run succesfully later with the other config
		DeleteTest dt=new DeleteTest(conf);
		try {
			dt.setSomeColumn(26);
			int rv=dt.executeUpdate();
			fail("Should not run unregistered spt, returned " + rv);
		} catch(SQLException e) {
			assertTrue("Unexpected message:  " + e.getMessage(),
				e.getMessage().startsWith("No mapping registered "));
		} finally {
			dt.close();
		}
	}

	public void testUpdate() throws Exception {
		fd.registerUpdate(url, "delete from blah", new Object[0], 13);
		Connection conn=DriverManager.getConnection(url);
		PreparedStatement pst=conn.prepareStatement("delete from blah");
		assertEquals(13, pst.executeUpdate());
		pst.close();
		conn.close();
	}

	public void testSPTUpdate() throws Exception {
		DeleteTest dt=new DeleteTest(conf);
		dt.setSomeColumn(11);
		assertEquals(11, dt.executeUpdate());
		// Run it again
		assertEquals(11, dt.executeUpdate());
		// Try it at 13
		dt.setSomeColumn(13);
		assertEquals(13, dt.executeUpdate());
		// Try it again at 11
		dt.setSomeColumn(11);
		assertEquals(11, dt.executeUpdate());
		
		// try the other config
		dt.close();
		
		dt=new DeleteTest(conf2);
		dt.setSomeColumn(26);
		assertEquals(26, dt.executeUpdate());
	}
	
	public void testBigSPTUpdate() throws Exception {
		java.sql.Date d=new java.sql.Date(System.currentTimeMillis());
		Time t=new Time(System.currentTimeMillis());
		Timestamp ts=new Timestamp(System.currentTimeMillis());
		ImplTest it=new ImplTest(conf);
		fd.registerUpdate(url, it, new Object[] {
			Boolean.TRUE, d, new Double("1.23"), new Float("1.234"),
			new Integer(13), new Long("123"), new BigDecimal("6.8790"),
			new BigDecimal("76.6470"), new Integer(6), new Integer(2),
			"other", "string", t, ts}, 187);
		
		it.setParam1(true);
		it.setParam2(d);
		it.setParam3(1.23);
		it.setParam4(1.234f);
		it.setParam5(13);
		it.setParam6(123);
		it.setParam7(new BigDecimal("6.879"));
		it.setParam8(new BigDecimal("76.647"));
		it.setParam9((short)6);
		it.setParam10((byte)2);
		it.setParam11("other");
		it.setParam12("string");
		it.setParam13(t);
		it.setParam14(ts);
		
		assertEquals(187, it.executeUpdate());
	}

	public void testBadArguments() throws Exception {
		ThreeColumnTest ttt=new ThreeColumnTest(conf);
		try {
			ResultSet rs=ttt.executeQuery();
			fail("Expected to fail on query without params, got " + rs);
		} catch(SQLException e) {
			assertNotNull(e.getMessage());
		}

		ttt.setFirst(1);
		ttt.setSecond(2);
		ttt.setThird("wrongstring");
		try {
			ResultSet rs=ttt.executeQuery();
			fail("Expected to fail on query with different args, got " + rs);
		} catch(SQLException e) {
			assertNotNull(e.getMessage());
		}
	}

	public void testSPTWithoutArgs() throws Exception {
		DumpTestTable dtt=new DumpTestTable(conf);
		ResultSet rs=dtt.executeQuery();
		assertTrue(rs.next());
		assertEquals("valone", rs.getString("something"));
		assertTrue(rs.next());
		assertEquals("valtwo", rs.getString("something"));
		assertFalse(rs.next());
		rs.close();
		dtt.close();
	}
	
	private void assertThreeColumnOne(ResultSet rs) throws Exception {
		assertTrue(rs.next());
		assertEquals(1, rs.getInt("first"));
		assertEquals(2, rs.getInt("second"));
		assertEquals("three", rs.getString("third"));
		assertTrue(rs.next());
		assertEquals(2, rs.getInt("first"));
		assertEquals(4, rs.getInt("second"));
		assertEquals("six", rs.getString("third"));
		assertTrue(rs.next());
		assertEquals(3, rs.getInt("first"));
		assertEquals(6, rs.getInt("second"));
		assertEquals("nine", rs.getString("third"));
		assertFalse(rs.next());
	}
	
	/**
	 * Validate queries with null parameters.
	 */
	public void testSPTWithNull() throws Exception {
		ThreeColumnTest ttt=new ThreeColumnTest(conf);
		ttt.setFirst((Integer)null);
		ttt.setSecond(3);
		ttt.setThird(null);
		
		ResultSet rs=ttt.executeQuery();
		assertThreeColumnOne(rs);
		rs.close();
		
		ttt.close();
	}

	public void testSPTWithArgs() throws Exception {
		ThreeColumnTest ttt=new ThreeColumnTest(conf);

		// Set us up the arguments
		ttt.setFirst(1);
		ttt.setSecond(2);
		ttt.setThird("string");

		ResultSet rs=ttt.executeQuery();
		assertThreeColumnOne(rs);
		rs.close();

		// Second run, different arguments.

		ttt.setFirst(1);
		ttt.setSecond(3);
		ttt.setThird("string");

		rs=ttt.executeQuery();
		assertTrue(rs.next());
		assertEquals(11, rs.getInt("first"));
		assertEquals(12, rs.getInt("second"));
		assertEquals("not a number", rs.getString("third"));
		assertTrue(rs.next());
		assertEquals(12, rs.getInt("first"));
		assertEquals(14, rs.getInt("second"));
		assertEquals("nut a nomber", rs.getString("third"));
		assertTrue(rs.next());
		assertEquals(13, rs.getInt("first"));
		assertEquals(16, rs.getInt("second"));
		assertEquals("noant hjieg", rs.getString("third"));
		assertFalse(rs.next());
		rs.close();

		ttt.close();
	}
	
	/**
	 * Test the SPT result set implementation.
	 */
	public void testSPTResult() throws Exception {
		ThreeColumnTest ttt=new ThreeColumnTest(conf);

		// Set us up the arguments
		ttt.setFirst(1);
		ttt.setSecond(2);
		ttt.setThird("string");
		ThreeColumnTest.Result rs=ttt.getResult();
		
		assertTrue(rs.next());
		assertEquals(1, rs.getFirst());
		assertEquals(2, rs.getSecond());
		assertEquals("three", rs.getThird());
		assertTrue(rs.next());
		assertEquals(2, rs.getFirst());
		assertEquals(4, rs.getSecond());
		assertEquals("six", rs.getString("third"));
		assertTrue(rs.next());
		assertEquals(3, rs.getFirst());
		assertEquals(6, rs.getSecond());
		assertEquals("nine", rs.getThird());
		assertFalse(rs.next());
		rs.close();
	}

	private void cacheTest(Integer a1, Integer a2, String s) throws Exception {
		ThreeColumnTest ttt=new ThreeColumnTest(poolConf);
		ttt.setFirst(a1);
		ttt.setSecond(a2);
		ttt.setThird(s);
		// Set a cache time so we'll get cached results
		ttt.setCacheTime(5000);
		ResultSet rs=ttt.executeQuery();
		assertThreeColumnOne(rs);
		assertTrue(rs instanceof CachedResultSet);
		assertEquals(1, ((CachedResultSet)rs).numCopies());
		
		// Run it again
		ResultSet rs2=ttt.executeQuery();
		assertFalse(rs == rs2);
		assertThreeColumnOne(rs2);
		assertTrue(rs2 instanceof CachedResultSet);
		assertEquals(2, ((CachedResultSet)rs2).numCopies());	
		ttt.close();
	}

	/**
	 * Test the cached DB result set.
	 */
	public void testCachedResult() throws Exception {
		cacheTest(new Integer(1), new Integer(2), "string");
	}

	public void testCachedResultWithNull() throws Exception {
		cacheTest(null, new Integer(3), null);
	}

	public void testAnyMatcher() throws Exception {
		DeleteTest dt=new DeleteTest(conf);
		fd.clearQueries();
		fd.registerUpdate(url, dt, new Object[]{
			new FileDriver.AnyParamMatcher()}, 13);

		dt.setSomeColumn(11);
		assertEquals(13, dt.executeUpdate());
		// Run it again
		assertEquals(13, dt.executeUpdate());
		// Try it at 19
		dt.setSomeColumn(19);
		assertEquals(13, dt.executeUpdate());
		// Try it again at 13
		dt.setSomeColumn(13);
		assertEquals(13, dt.executeUpdate());

		dt.close();
	}

	public void testClassMatcher() throws Exception {
		DeleteTest dt=new DeleteTest(conf);
		fd.clearQueries();
		fd.registerUpdate(url, dt, new Object[]{
			new FileDriver.ClassParamMatcher(Integer.class)}, 7);

		dt.setSomeColumn(19);
		assertEquals(7, dt.executeUpdate());
		// Run it again
		assertEquals(7, dt.executeUpdate());
		dt.setSomeColumn(119);
		assertEquals(7, dt.executeUpdate());

		dt.close();
	}

	public void testClob() throws Exception {
		ClobTest ct=new ClobTest(conf);
		ct.setClob("test");
		assertEquals(1, ct.executeUpdate());
		ct.close();
	}

}