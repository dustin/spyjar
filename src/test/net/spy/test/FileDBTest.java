// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: B9EACADC-407D-454E-AEAE-3EC3272046B4

package net.spy.test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import net.spy.db.DBSQL;
import net.spy.db.FileDriver;
import net.spy.util.SpyConfig;

import net.spy.test.db.DumpTestTable;
import net.spy.test.db.ThreeColumnTest;
import net.spy.test.db.DeleteTest;

import junit.framework.TestCase;

public class FileDBTest extends TestCase {

	private FileDriver fd=null;
	private String url=FileDriver.URL_PREFIX + "/x";
	private SpyConfig conf=null;

	protected void setUp() throws Exception {
		Class.forName("net.spy.db.FileDriver");
		fd=(FileDriver)DriverManager.getDriver(url);
		
		conf=new SpyConfig();
		conf.put("dbConnectionSource", "net.spy.db.JDBCConnectionSource");
		conf.put("dbDriverName", "net.spy.db.FileDriver");
		conf.put("dbSource", url);
		conf.put("dbUser", "username");
		conf.put("dbPass", "password");

		// Register up a couple of queries.
		DumpTestTable dtt=new DumpTestTable(conf);
		ThreeColumnTest ttt=new ThreeColumnTest(conf);
		DeleteTest dt=new DeleteTest(conf);

		fd.registerQuery(dtt, new Object[0], getPath("dumptest.txt"));
		fd.registerQuery(ttt,
			new Object[]{new Integer(1), new Integer(2), "string"},
			getPath("threecol.txt"));
		fd.registerQuery(ttt,
			new Object[]{new Integer(1), new Integer(3), "string"},
			getPath("threecol2.txt"));
		fd.registerUpdate(dt, new Object[]{new Integer(11)}, 11);
		fd.registerUpdate(dt, new Object[]{new Integer(13)}, 13);
	}

	protected void tearDown() {
		fd.clearQueries();
	}

	private File getPath(String p) {
		return(new File(System.getProperty("basedir")
			+ "/src/test/net/spy/test/db/" + p));
	}

	public void testFileDB() throws Exception {
		fd.registerQuery("select count(*) from table",
			getPath("counttest.txt"));
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

	public void testUpdate() throws Exception {
		fd.registerUpdate("delete from blah", new Object[0], 13);
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

	public void testSPTWithArgs() throws Exception {
		ThreeColumnTest ttt=new ThreeColumnTest(conf);

		// Set us up the arguments
		ttt.setFirst(1);
		ttt.setSecond(2);
		ttt.setThird("string");

		ResultSet rs=ttt.executeQuery();
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

}
