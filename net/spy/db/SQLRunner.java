// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: SQLRunner.java,v 1.1 2003/07/15 22:51:40 dustin Exp $

package net.spy.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.spy.SpyDB;
import net.spy.SpyObject;

/**
 * Run a SQL script from an InputStream.
 */
public class SQLRunner extends SpyObject {

	private SpyDB db=null;

	/**
	 * Get an instance of SQLRunner.
	 */
	public SQLRunner(SpyDB db) {
		super();
		this.db=db;
	}

	/** 
	 * Run the given script as a transaction.
	 * 
	 * @param is the stream containing the script
	 * @throws SQLException if there's a SQL problem with this script
	 * @throws IOException if there's a problem reading the script
	 */
	public void runScript(InputStream is) throws SQLException, IOException {
		runScript(is, false, false);
	}

	/** 
	 * Run the given script.
	 * 
	 * @param is the stream containing the script
	 * @param autocommit if true, commit after each statement
	 * @param errok if true, ignore SQL exceptions on each statement
	 * @throws SQLException if there's a SQL problem executing the script
	 * @throws IOException if there's a problem reading the script
	 */
	public void runScript(InputStream is, boolean autocommit, boolean errok)
		throws SQLException, IOException {

		// Get a LineNumberReader from this stream so we can process it one
		// line at a time
		LineNumberReader lr=new LineNumberReader(new InputStreamReader(is));

		Connection conn=null;
		boolean successful=false;
		try {
			// Get the connection
			conn=db.getConn();
			// Set the autocommit setting
			conn.setAutoCommit(autocommit);

			// Execute the script
			executeScript(conn, lr, errok);

			// We're finished, commit
			if(!autocommit) {
				conn.commit();
			}
			successful=true;
		} finally {

			// Close the reader
			lr.close();

			// If we weren't successful, but we did at least get a
			// connection, clean it up.
			if(conn!=null) {
				// Stuff to do when we weren't successful
				if(!successful) {
					try {
						conn.rollback();
					} catch(SQLException e) {
						getLogger().warn("Error rolling back", e);
					}
				}
				// Reset the autocommit if it was set to false
				if(!autocommit) {
					try {
						conn.setAutoCommit(true);
					} catch(SQLException e) {
						getLogger().warn("Error resetting autocommit");
					}
				}
			} // successful check
		} // finally block
	}

	private void executeScript(Connection conn, LineNumberReader lr,
		boolean errok) throws SQLException, IOException {

		String curline=null;
		StringBuffer query=new StringBuffer(1024);

		while( (curline=lr.readLine()) != null) {
			curline=curline.trim();

			if(curline.equals(";")) {
				// Execute the current query

				Statement st=conn.createStatement();
				int updated=0;
				long starttime=System.currentTimeMillis();
				try {
					updated=st.executeUpdate(query.toString());
				} catch(SQLException e) {
					if(errok) {
						// log the exception
						getLogger().warn("Ignorning problem executing "
							+ query, e);
					} else {
						throw e;
					}
				}
				long stoptime=System.currentTimeMillis();
				st.close();
				st=null;
				String rows=" rows";
				if(updated == 1) {
					rows=" row";
				}
				getLogger().info("Updated " + updated + rows
					+ " in " + (stoptime-starttime) + "ms");

				// Clear out the string buffer
				query.delete(0, query.length() + 1);
				// This is an assertion, but I want to remain < 1.4
				// compatible
				if(query.length() != 0) {
					throw new Error("Assertion failed:  query.length() == "
						+ query.length());
				}
			} else if(curline.startsWith("--")) {
				// Comment to be logged
				getLogger().info(lr.getLineNumber() + ":  " + curline);
			} else {
				// Get more query
				if(curline.length() > 0) {
					query.append(curline);
					query.append("\n");
				}
			}
		} // All lines
	} // executeScript()

}
