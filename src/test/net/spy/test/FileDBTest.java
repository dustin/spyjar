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
import net.spy.test.db.DumpTestTable;
import net.spy.test.db.ThreeColumnTest;
import net.spy.util.SpyConfig;

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

		mapQueries(dtt, new Object[0], getPath("dumptest.txt"));
		mapQueries(ttt, new Object[]{new Integer(1), new Integer(2), "string"},
			getPath("threecol.txt"));
		mapQueries(ttt, new Object[]{new Integer(1), new Integer(3), "string"},
			getPath("threecol2.txt"));
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

	private void mapQueries(DBSQL dtt, Object args[], File path) {
		for(Iterator i=dtt.getRegisteredQueries().values().iterator();
			i.hasNext();) {
			String query=(String)i.next();
			fd.registerQuery(query, args, path);
		}
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
