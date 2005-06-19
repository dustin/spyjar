// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 9E0E68F2-C8C3-4EDB-B04D-E0D107E8ECD7

package net.spy.test;

import java.io.InputStream;
import java.io.FileInputStream;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

import net.spy.db.SQLRunner;

/**
 * Test the SQL Runner.
 */
public class SQLRunnerTest extends MockObjectTestCase {

	private static final String QUERY1="update some_table set val=1\n";
	private static final String QUERY2="update other_table set val=1\n"
		+ "otherval=2\n";

	private Mock getSuccessfulMockFor(String query) {
		Mock rv=mock(Statement.class);
		rv.expects(once()).method("setQueryTimeout")
			.with(eq(37));
		rv.expects(once()).method("executeUpdate")
			.with(eq(query))
			.will(returnValue(1));
		rv.expects(once()).method("close");
		return(rv);
	}

	private String getScriptPath() {
		String path=(String)System.getProperties().get("basedir")
			+ "/src/test/net/spy/test/test.sql";
		return(path);
	}

	private void runSuccessfulTest(boolean withAutocommit) throws Exception {
		Mock connMock=mock(Connection.class);
		boolean origAutoCommit=false;

		connMock.expects(once()).method("getAutoCommit")
			.will(returnValue(origAutoCommit));

		if(withAutocommit != origAutoCommit) {
			connMock.expects(once()).method("setAutoCommit")
				.with(eq(withAutocommit));
		}
		connMock.expects(atLeastOnce()).method("createStatement")
			.will(onConsecutiveCalls(
				returnValue(getSuccessfulMockFor(QUERY1).proxy()),
				returnValue(getSuccessfulMockFor(QUERY2).proxy())));
		if(!withAutocommit) {
			connMock.expects(once()).method("commit");
		}
		if(withAutocommit != origAutoCommit) {
			connMock.expects(once()).method("setAutoCommit")
				.with(eq(origAutoCommit));
		}

		InputStream f=new FileInputStream(getScriptPath());
		SQLRunner sr=new SQLRunner((Connection)connMock.proxy());
		sr.setTimeout(37);
		sr.runScript(f, withAutocommit, false);
		f.close();
	}

	/** 
	 * Test a successful script execution with autocommit on.
	 */
	public void testSuccessfulScriptWithAutocommit() throws Exception {
		runSuccessfulTest(true);
	}

	/** 
	 * Test a successful script with autocommit off.
	 */
	public void testSuccessfulScriptWithoutAutocommit() throws Exception {
		runSuccessfulTest(false);
	}

	/** 
	 * Test invocations over a plain unhappy Connection.
	 */
	public void testFailingEarlyFailingOften() throws Exception {
		Mock connMock=mock(Connection.class);

		connMock.expects(once()).method("getAutoCommit")
			.will(throwException(new SQLException("bug off1")));
		connMock.expects(once()).method("rollback")
			.will(throwException(new SQLException("bug off2")));
		// XXX:  This should probably not be invoked in the error case
		connMock.expects(once()).method("setAutoCommit")
			.with(eq(true))
			.will(throwException(new SQLException("bug off3")));

		// Run the script
		InputStream f=new FileInputStream(getScriptPath());
		SQLRunner sr=new SQLRunner((Connection)connMock.proxy());
		try {
			sr.runScript(f);
		} catch(SQLException e) {
			assertEquals("bug off1", e.getMessage());
		}
		f.close();
	}

	private Statement getBadStatement(String query) {
		Mock badMock=mock(Statement.class);
		badMock.expects(once()).method("setQueryTimeout")
			.with(eq(37));
		badMock.expects(once()).method("executeUpdate")
			.with(eq(QUERY2))
			.will(throwException(new SQLException("bug off")));
		badMock.expects(once()).method("close");
		return((Statement)badMock.proxy());
	}

	/** 
	 * Test a single failing query.
	 */
	public void testFailingQuery() throws Exception {
		Mock connMock=mock(Connection.class);
		boolean origAutoCommit=true;

		// Set up the connection
		connMock.expects(once()).method("getAutoCommit")
			.will(returnValue(origAutoCommit));

		connMock.expects(once()).method("setAutoCommit")
			.with(eq(false));
		connMock.expects(atLeastOnce()).method("createStatement")
			.will(onConsecutiveCalls(
				returnValue(getSuccessfulMockFor(QUERY1).proxy()),
				returnValue(getBadStatement(QUERY2))));
		connMock.expects(once()).method("rollback");
		connMock.expects(once()).method("setAutoCommit")
			.with(eq(true));

		InputStream f=new FileInputStream(getScriptPath());
		SQLRunner sr=new SQLRunner((Connection)connMock.proxy());
		sr.setTimeout(37);
		try {
			sr.runScript(f);
			fail("Expected an exception out of this invocation");
		} catch(SQLException e) {
			assertEquals("bug off", e.getMessage());
		}
		f.close();
	}

	/** 
	 * Test a single failing query with the errok flag.  The whole script will
	 * be considered a success here, but an individual query will fail to
	 * complete.
	 */
	public void testFailingQueryWithErrOk() throws Exception {
		Mock connMock=mock(Connection.class);
		boolean origAutoCommit=false;

		// Set up the connection
		connMock.expects(once()).method("getAutoCommit")
			.will(returnValue(origAutoCommit));

		connMock.expects(once()).method("setAutoCommit")
			.with(eq(true));
		connMock.expects(atLeastOnce()).method("createStatement")
			.will(onConsecutiveCalls(
				returnValue(getSuccessfulMockFor(QUERY1).proxy()),
				returnValue(getBadStatement(QUERY2))));
		connMock.expects(once()).method("setAutoCommit")
			.with(eq(false));

		InputStream f=new FileInputStream(getScriptPath());
		SQLRunner sr=new SQLRunner((Connection)connMock.proxy());
		sr.setTimeout(37);
		sr.runScript(f, true, true);
		f.close();
	}

}
