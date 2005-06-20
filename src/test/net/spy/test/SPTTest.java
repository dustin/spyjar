// Copyright (c) 2005  Scott Lamb <slamb@slamb.org>
//
// arch-tag: 9F734ECE-788F-4AF9-8662-FB2245C69A44

package net.spy.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.matcher.InvokeAtLeastOnceMatcher;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.constraint.IsInstanceOf;
import org.jmock.core.stub.ReturnStub;

import net.spy.db.DBSP;
import net.spy.util.SpyConfig;
import net.spy.test.db.BooleanTest;
import net.spy.test.db.DialectTest;

import net.spy.db.sp.PrimaryKeyStuff;
import net.spy.db.sp.SelectPrimaryKey;
import net.spy.db.sp.UpdatePrimaryKey;

import net.spy.test.db.ImplTest;

public class SPTTest extends MockObjectTestCase {
	private Mock connMock;
	private Connection conn;
	private Mock stMock;

	public void setUp() {
		connMock = mock(Connection.class);
		conn = (Connection) connMock.proxy();

		stMock = mock(PreparedStatement.class);
	}

	private void testCallSequence(boolean[] booleans) throws Exception {
		stMock.expects(once()).method("setQueryTimeout");
		stMock.expects(once()).method("setMaxRows").with(eq(10));

		BooleanTest bt = new BooleanTest(conn);

		connMock.expects(once())
				.method("prepareStatement")
				.with(eq("select ? as a_boolean\n"))
				.will(returnValue(stMock.proxy()));
		bt.setMaxRows(10);

		boolean tryCursor=false;

		for(int i=0; i<booleans.length; i++) {
			boolean aBoolean=booleans[i];

			bt.setABoolean(aBoolean);

			// Meta data handling
			Mock rsmdMock=mock(ResultSetMetaData.class);
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
			Mock rsMock = mock(ResultSet.class);
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

	private void runQuerySelectorTest(SpyConfig conf) throws Exception {
		DialectTest dt=new DialectTest(conf);
		dt.setAnInt(13);
		ResultSet rs=dt.executeQuery();
		rs.close();
		dt.close();
	}

	/** 
	 * Test query selection within SPTs.
	 */
	public void testQuerySelectionDefaultByDriver() throws Exception {
		SpyConfig conf=new SpyConfig();
		conf.put("dbConnectionSource",
			"net.spy.test.SPTTest$GeneralConnectionSource");
		runQuerySelectorTest(conf);
	}

	/** 
	 * Test query selection within SPTs (by driver name).
	 */
	public void testQuerySelectionDefault() throws Exception {
		SpyConfig conf=new SpyConfig();
		conf.put("dbDriverName", "blah.some.driver.name");
		conf.put("dbConnectionSource",
			"net.spy.test.SPTTest$GeneralConnectionSource");
		runQuerySelectorTest(conf);
	}

	/** 
	 * Test a non-existent query source (should select default).
	 */
	public void testQuerySelectionNonExistent() throws Exception {
		SpyConfig conf=new SpyConfig();
		conf.put("dbConnectionSource",
			"net.spy.test.SPTTest$GeneralConnectionSource");
		conf.put("queryName", "martha");
		runQuerySelectorTest(conf);
	}

	/** 
	 * Test an existing query selector.
	 */
	public void testQuerySelectionOracle() throws Exception {
		SpyConfig conf=new SpyConfig();
		conf.put("queryName", "oracle");
		conf.put("dbConnectionSource",
			"net.spy.test.SPTTest$OracleConnectionSource");
		runQuerySelectorTest(conf);
	}

	/** 
	 * Test an existing query selector by driver name for a driver whose query
	 * we don't have.
	 */
	public void testQuerySelectionByMissingDriver() throws Exception {
		SpyConfig conf=new SpyConfig();
		conf.put("dbDriverName", "org.postgresql.Driver");
		conf.put("dbConnectionSource",
			"net.spy.test.SPTTest$GeneralConnectionSource");
		runQuerySelectorTest(conf);
	}

	/** 
	 * Test a default query with a name close to another one.
	 */
	public void testQuerySelectionCloseToKnownDriver() throws Exception {
		SpyConfig conf=new SpyConfig();
		conf.put("dbDriverName", "oracle.jdbc.dr");
		conf.put("dbConnectionSource",
			"net.spy.test.SPTTest$GeneralConnectionSource");
		runQuerySelectorTest(conf);
	}

	/** 
	 * Test an existing query selector by driver name.
	 */
	public void testQuerySelectionOracleByDriver() throws Exception {
		SpyConfig conf=new SpyConfig();
		conf.put("dbDriverName", "oracle.jdbc.driver.Driver");
		conf.put("dbConnectionSource",
			"net.spy.test.SPTTest$OracleConnectionSource");
		runQuerySelectorTest(conf);
	}

	/** 
	 * Test an existing query selector by driver name (exact).
	 */
	public void testQuerySelectionOracleByExactDriverPrefix() throws Exception {
		SpyConfig conf=new SpyConfig();
		conf.put("dbDriverName", "oracle.jdbc.driver.");
		conf.put("dbConnectionSource",
			"net.spy.test.SPTTest$OracleConnectionSource");
		runQuerySelectorTest(conf);
	}

	/** 
	 * Test an existing query selector by driver name (exact).
	 */
	public void testQuerySelectionOracleByExactDriver() throws Exception {
		SpyConfig conf=new SpyConfig();
		// This is a weird case when the key name is the same as a known query.
		// Perhaps it makes sense if you name your queries directly after
		// drivers.
		conf.put("dbDriverName", "oracle");
		conf.put("dbConnectionSource",
			"net.spy.test.SPTTest$OracleConnectionSource");
		runQuerySelectorTest(conf);
	}

	/** 
	 * Test the primary key selection spt.
	 */
	public void testSelectPrimaryKey() throws Exception {
		SpyConfig conf=new SpyConfig();
		conf.put("dbConnectionSource",
			"net.spy.test.SPTTest$PKConnectionSource");
		SelectPrimaryKey spk1=new SelectPrimaryKey(conf);
		spk1.setTableName("test");

		Mock cMock=mock(Connection.class);
		SelectPrimaryKey spk2=new SelectPrimaryKey((Connection)cMock.proxy());
		spk2.setTableName("test");
	}

	/** 
	 * Test the primary key update spt.
	 */
	public void testUpdatePrimaryKey() throws Exception {
		SpyConfig conf=new SpyConfig();
		conf.put("dbConnectionSource",
			"net.spy.test.SPTTest$PKConnectionSource");
		UpdatePrimaryKey upk1=new UpdatePrimaryKey(conf);
		upk1.setTableName("test");

		Mock cMock=mock(Connection.class);
		UpdatePrimaryKey upk2=new UpdatePrimaryKey((Connection)cMock.proxy());
		upk2.setTableName("test");
	}

	private void successfulCoercion(DBSP db, String field, String val)
		throws Exception {
		db.setCoerced(field, null);
		db.setCoerced(field, val);
	}

	private void failedCoercion(DBSP db, String field, String val,
		Class expectedException, String expectedMessage) throws SQLException {
		// This should be OK
		db.setCoerced(field, null);
		try {
			db.setCoerced(field, val);
			fail("Expected exception setting " + field + " to " + val);
		} catch(Exception e) {
			assertTrue("Unexpected exception",
				e.getClass().isAssignableFrom(expectedException));
			if(expectedMessage != null) {
				assertEquals(expectedMessage, e.getMessage());
			}
		}
	}

	/** 
	 * Test coercion routines.
	 * 
	 * @throws Exception 
	 */
	public void testCoercion() throws Exception {
		Mock cMock=mock(Connection.class);
		ImplTest it=new ImplTest((Connection)cMock.proxy());
		successfulCoercion(it, "param1", "true");
		successfulCoercion(it, "param1", "false");
		failedCoercion(it, "param2", "some date", SQLException.class,
			"Date types not currently handled");
		successfulCoercion(it, "param3", "13.523");
		successfulCoercion(it, "param3", "13");
		successfulCoercion(it, "param4", "35.2352");
		successfulCoercion(it, "param4", "35");
		successfulCoercion(it, "param5", "2532");
		failedCoercion(it, "param5", "3.526", NumberFormatException.class,
			null);
		successfulCoercion(it, "param6", "83259252390528");
		successfulCoercion(it, "param7", "992359236826722.835382");
		successfulCoercion(it, "param8", "8835823952390523.23852");
		successfulCoercion(it, "param9", "2");
		successfulCoercion(it, "param10", "2");
		successfulCoercion(it, "param11", "some obj");
		successfulCoercion(it, "param12", "This is a string");
		failedCoercion(it, "param13", "some date", SQLException.class,
			"Date types not currently handled");
		failedCoercion(it, "param14", "some date", SQLException.class,
			"Date types not currently handled");
	}

	// A dumb connection source that's just used to make the PK related SPT
	// constructors happy.
	public static class PKConnectionSource extends MockConnectionSource {
		protected void setupMock(Mock connMock, SpyConfig conf) {
			// nothing
		}
	}

	public abstract static class AbsConnSrc extends MockConnectionSource {
		protected abstract String getQuery();

		protected void setupMock(Mock connMock, SpyConfig conf) {
			Mock rsmdMock=new Mock(ResultSetMetaData.class);
			rsmdMock.expects(new InvokeAtLeastOnceMatcher())
				.method("getColumnCount")
				.will(new ReturnStub(new Integer(1)));
			rsmdMock.expects(new InvokeAtLeastOnceMatcher())
				.method("getColumnName")
				.with(new IsEqual(new Integer(1)))
				.will(new ReturnStub("the_int"));
			rsmdMock.expects(new InvokeAtLeastOnceMatcher())
				.method("getColumnTypeName")
				.with(new IsEqual(new Integer(1)))
				.will(new ReturnStub("INTEGER"));

			Mock rsMock=new Mock(ResultSet.class);
			rsMock.expects(new InvokeOnceMatcher()).method("close");
			rsMock.expects(new InvokeOnceMatcher()).method("getMetaData")
				.will(new ReturnStub(rsmdMock.proxy()));

			Mock pstMock=new Mock(PreparedStatement.class);
			pstMock.expects(new InvokeOnceMatcher()).method("setQueryTimeout");
			pstMock.expects(new InvokeOnceMatcher()).method("setMaxRows")
				.with(new IsEqual(new Integer(0)));
			pstMock.expects(new InvokeOnceMatcher()).method("setInt")
				  .with(new IsEqual(new Integer(1)),
				  	new IsEqual(new Integer(13)));
			pstMock.expects(new InvokeOnceMatcher()).method("executeQuery")
				  .will(new ReturnStub(rsMock.proxy()));
			pstMock.expects(new InvokeOnceMatcher()).method("close");

			connMock.expects(new InvokeOnceMatcher()).method("prepareStatement")
				.with(new IsEqual(getQuery()))
				.will(new ReturnStub(pstMock.proxy()));

			connMock.expects(new InvokeOnceMatcher()).method("close");
		}
	}

	public static class GeneralConnectionSource extends AbsConnSrc {
		public String getQuery() {
			return("select ? as the_int\n");
		}
	}

	public static class OracleConnectionSource extends AbsConnSrc {
		public String getQuery() {
			return("select ? as the_int from dual\n");
		}
	}

}
