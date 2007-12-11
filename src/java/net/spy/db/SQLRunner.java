// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>

package net.spy.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import net.spy.SpyObject;

/**
 * Run a SQL script from an InputStream.
 *
 * <p>
 *  SQL scripts run by this class are slightly special over normal SQL
 *  scripts in that they require a particular structure.
 * </p>
 * <p>
 *  Lines beginning with a SQL comment (--) are logged at info level.
 *  Queries are executed when a semicolon is encountered on a line all by
 *  itself.  Empty lines are ignored.  Everything else is concatenated to
 *  form the next query to execute.  Queries that return a result set will
 *  throw a SQLException.
 * </p>
 */
public class SQLRunner extends SpyObject {

	private final Connection connection;
	private int timeout=0;

	/**
	 * Get an instance of SQLRunner.
	 */
	public SQLRunner(Connection conn) {
		super();
		this.connection=conn;
	}

	/**
	 * Set the query timeout (in seconds).
	 */
	public void setTimeout(int to) {
		this.timeout=to;
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

		boolean successful=false;
		boolean origAutoCommit=true;
		try {
			// Set the autocommit setting
			origAutoCommit=connection.getAutoCommit();
			if(origAutoCommit != autocommit) {
				connection.setAutoCommit(autocommit);
			}

			// Execute the script
			executeScript(lr, errok);

			// We're finished, commit
			if(!autocommit) {
				connection.commit();
			}
			successful=true;
		} finally {

			// Close the reader
			lr.close();

			// If we weren't successful, but we did at least get a
			// connection, clean it up.
			if(connection!=null) {
				// Stuff to do when we weren't successful
				if(!successful) {
					try {
						connection.rollback();
					} catch(SQLException e) {
						getLogger().warn("Error rolling back", e);
					}
				}
				// Reset the autocommit if it was set to false
				if(origAutoCommit != autocommit) {
					try {
						connection.setAutoCommit(origAutoCommit);
					} catch(SQLException e) {
						getLogger().warn("Error resetting autocommit");
					}
				}
			} // successful check
		} // finally block
	}

	private void executeScript(LineNumberReader lr, boolean errok)
		throws SQLException, IOException {

		String curline=null;
		StringBuilder query=new StringBuilder(1024);

		while( (curline=lr.readLine()) != null) {
			curline=curline.trim();

			if(curline.equals(";")) {
				// Execute the current query

				Statement st=connection.createStatement();
				st.setQueryTimeout(timeout);
				int affected=0;
				long starttime=System.currentTimeMillis();
				try {
					affected=st.executeUpdate(query.toString());
				} catch(SQLException e) {
					if(errok) {
						// log the exception
						getLogger().warn("Ignoring problem executing %s",
								query, e);
					} else {
						throw e;
					}
				} finally {
					st.close();
				}
				long stoptime=System.currentTimeMillis();
				st=null;
				getLogger().info("Affected %d %s in %dms", affected,
						affected==1?"row":"row", (stoptime-starttime));

				// Clear out the string buffer
				query.delete(0, query.length() + 1);
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
