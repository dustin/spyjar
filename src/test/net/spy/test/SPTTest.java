// Copyright (c) 2005  Scott Lamb <slamb@slamb.org>
//
// arch-tag: 9F734ECE-788F-4AF9-8662-FB2245C69A44

package net.spy.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import net.spy.db.DBSP;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

import net.spy.test.db.BooleanTest;

public class SPTTest extends MockObjectTestCase {
	private Mock connMock;
	private Connection conn;
	private Mock stMock;

	public void setUp() {
		connMock = new Mock(Connection.class);
		conn = (Connection) connMock.proxy();

		stMock = new Mock(PreparedStatement.class);
	}

	private void testCallSequence(boolean[] booleans) throws Exception {
		stMock.expects(atLeastOnce()).method("setQueryTimeout");
		stMock.expects(atLeastOnce()).method("setMaxRows").with(eq(10));;

		BooleanTest bt = new BooleanTest(conn);

		boolean tryCursor=false;

		for (boolean aBoolean : booleans) {
			connMock.expects(once())
					.method("prepareStatement")
					.with(eq("select ? as a_boolean\n"))
					.will(returnValue(stMock.proxy()));

			bt.setMaxRows(10);

			bt.setABoolean(aBoolean);

			// Meta data handling
			Mock rsmdMock=new Mock(ResultSetMetaData.class);
			rsmdMock.expects(atLeastOnce())
				.method("getColumnCount")
				.will(returnValue(1));
			rsmdMock.expects(atLeastOnce())
				.method("getColumnName")
				.with(eq(1))
				.will(returnValue("a_boolean"));
			rsmdMock.expects(atLeastOnce())
				.method("getColumnTypeName")
				.with(eq(1))
				.will(returnValue("BIT"));

			// Result set handling
			Mock rsMock = new Mock(ResultSet.class);
			ResultSet rs = (ResultSet) rsMock.proxy();
			rsMock.expects(once())
				.method("getMetaData")
				.will(returnValue(rsmdMock.proxy()));

			// What does the statement expect to happen each invocation?
			stMock.expects(once())
				  .method("setBoolean")
				  .with(eq(1), eq(aBoolean));
			stMock.expects(once())
				  .method("executeQuery")
				  .will(returnValue(rs));

			if(tryCursor) {
				stMock.expects(once())
				  	.method("setCursorName")
				  	.with(eq("testCursor"));

				bt.setCursorName("testCursor");
			}

			ResultSet actual = bt.executeQuery();
			// The result should be the same
			assertSame(rs, actual);

			tryCursor=true;
		}
		stMock.expects(once()).method("close");

		// After execution, we're going to look at more stuff.
		stMock.expects(once())
			  .method("getWarnings")
			  .will(returnValue(null));

		// Check for warnings.
		assertNull(bt.getWarnings());

		bt.close();
	}

	public void testSingleCalls() throws Exception {
		testCallSequence(new boolean[] { false });
		testCallSequence(new boolean[] { true });
	}

	public void testMultipleCalls() throws Exception {
		testCallSequence(new boolean[] { false, true });
	}
}
