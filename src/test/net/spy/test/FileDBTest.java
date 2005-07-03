// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: B9EACADC-407D-454E-AEAE-3EC3272046B4

package net.spy.test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.spy.db.FileDriver;

import junit.framework.TestCase;

public class FileDBTest extends TestCase {
	
	protected void setUp() throws Exception {
		Class.forName("net.spy.db.FileDriver");
	}

	private File getPath(String p) {
		return(new File(System.getProperty("basedir")
			+ "/src/test/net/spy/test/db/" + p));
	}
	
	public void testFileDB() throws Exception {
		FileDriver fd=(FileDriver)DriverManager.getDriver(
			FileDriver.URL_PREFIX + "/x");
		fd.registerQuery("select count(*) from table", getPath("counttest.txt"));
		Connection conn=DriverManager.getConnection(FileDriver.URL_PREFIX + "/x");
		PreparedStatement pst=conn.prepareStatement("select count(*) from table");
		ResultSet rs=pst.executeQuery();
		assertTrue(rs.next());
		assertEquals(8325, rs.getInt("count"));
		assertFalse(rs.next());
		rs.close();
		conn.close();
	}

}
