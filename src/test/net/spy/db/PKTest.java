// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.db;

import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

import junit.framework.TestCase;
import net.spy.util.SpyConfig;

import org.jmock.Mock;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.InvokeAtLeastOnceMatcher;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.stub.ReturnStub;
import org.jmock.core.stub.StubSequence;

/**
 * Test the primary key implementation.
 */
public class PKTest extends TestCase {
	
	@Override
	protected void tearDown() {
		GetPK.setInstance(null);
	}
	
	/**
	 * Test the singleton operation.
	 */
	public void testSingleton() {
		GetPK ref=GetPK.getInstance();
		assertSame(ref, GetPK.getInstance());
		GetPK.setInstance(null);
		assertNotSame(ref, GetPK.getInstance());
	}

	/** 
	 * Test basic primary key functionality.
	 */
	public void testPrimaryKeyByConf() throws Exception {
		SpyConfig conf=new SpyConfig();
		conf.put("dbConnectionSource", SuccessConnectionSource.class.getName());

		GetPK getpk=GetPK.getInstance();
		BigDecimal previous=getpk.getPrimaryKey(conf, "test_table");
		BigDecimal one=new BigDecimal(1);

		for(int i=0; i<1000; i++) {
			BigDecimal newKey=getpk.getPrimaryKey(conf, "test_table");

			assertEquals("Keys not in sequence", previous.add(one), newKey);

			previous=newKey;
		}

		ConnectionSourceFactory cnf=ConnectionSourceFactory.getInstance();
		MockConnectionSource mockSource=
			(MockConnectionSource)cnf.getConnectionSource(conf);

		mockSource.verifyConnections();
	}

	/** 
	 * Test a PK with a missing key (no update).
	 */
	public void testPrimaryKeyMissingKey() throws Exception {
		SpyConfig conf=new SpyConfig();
		conf.put("dbConnectionSource", MissingKeySource.class.getName());
		GetPK getpk=GetPK.getInstance();
		try {
			BigDecimal val=getpk.getPrimaryKey(conf, "test_table");
			fail("Expected a missing key, got " + val);
		} catch(SQLException e) {
			assertEquals("Incorrect row count for"
				+ " test_table (got 0) - "
				+ "This usually means the primary key table does not have "
				+ "test_table or there is a case mismatch.", e.getMessage());
		}

		ConnectionSourceFactory cnf=ConnectionSourceFactory.getInstance();
		MockConnectionSource mockSource=
			(MockConnectionSource)cnf.getConnectionSource(conf);
		mockSource.verifyConnections();
	}

	abstract static class BaseConnectionSource
		extends MockConnectionSource {

		protected static final String UPDATE=
			"update primary_key\n"
			+ "\tset primary_key=primary_key+incr_value\n"
			+ "\twhere table_name=?\n";
		protected static final String SELECT=
			"select\n"
			+ "\ttable_name,\n"
			+ "\t(primary_key - (incr_value-1)) as first_key,\n"
			+ "\tprimary_key as last_key\n"
			+ "from\n"
			+ "\tprimary_key\n"
			+ "where\n"
			+ "\ttable_name=?\n";

		protected PreparedStatement getUpdateStatementToReturn(int what) {
			// the prepared statement that will update the record
			Mock updatePst=new Mock(PreparedStatement.class);
			updatePst.expects(new InvokeOnceMatcher()).method("setQueryTimeout")
				.with(new IsEqual(new Integer(0)));
			updatePst.expects(new InvokeOnceMatcher()).method("setMaxRows")
				.with(new IsEqual(new Integer(0)));
			updatePst.expects(new InvokeOnceMatcher()).method("setString")
				.with(new IsEqual(new Integer(1)), new IsEqual("test_table"));
			updatePst.expects(new InvokeOnceMatcher()).method("executeUpdate")
				.will(new ReturnStub(new Integer(what)));
			updatePst.expects(new InvokeOnceMatcher()).method("close");
			registerMock(updatePst);
			return((PreparedStatement)updatePst.proxy());
		}

		protected ResultSet getResultSetWithRange(
			BigDecimal from, BigDecimal to) {

			Mock rsmd=new Mock(ResultSetMetaData.class);
			rsmd.expects(new InvokeAtLeastOnceMatcher())
				.method("getColumnCount")
				.will(new ReturnStub(new Integer(2)));
			rsmd.expects(new InvokeAtLeastOnceMatcher())
				.method("getColumnName").with(new IsEqual(new Integer(1)))
				.will(new ReturnStub("first_key"));
			rsmd.expects(new InvokeAtLeastOnceMatcher())
				.method("getColumnName").with(new IsEqual(new Integer(2)))
				.will(new ReturnStub("last_key"));
			rsmd.expects(new InvokeAtLeastOnceMatcher())
				.method("getColumnTypeName").with(new IsEqual(new Integer(1)))
				.will(new ReturnStub("BIGINT"));
			rsmd.expects(new InvokeAtLeastOnceMatcher())
				.method("getColumnTypeName").with(new IsEqual(new Integer(2)))
				.will(new ReturnStub("BIGINT"));
			registerMock(rsmd);

			Mock rs=new Mock(ResultSet.class);
			rs.expects(new InvokeOnceMatcher()).method("getMetaData")
				.will(new ReturnStub(rsmd.proxy()));
			ArrayList<ReturnStub> ofResults=new ArrayList<ReturnStub>();
			ofResults.add(new ReturnStub(Boolean.TRUE));
			ofResults.add(new ReturnStub(Boolean.FALSE));
			rs.expects(new InvokeAtLeastOnceMatcher()).method("next")
				.will(new StubSequence(ofResults));
			rs.expects(new InvokeOnceMatcher()).method("getBigDecimal")
				.with(new IsEqual("first_key"))
				.will(new ReturnStub(from));
			rs.expects(new InvokeOnceMatcher()).method("getBigDecimal")
				.with(new IsEqual("last_key"))
				.will(new ReturnStub(to));
			rs.expects(new InvokeOnceMatcher()).method("close");
			registerMock(rs);
			return((ResultSet)rs.proxy());
		}

		protected PreparedStatement getSelectStatementWithRange(
			BigDecimal from, BigDecimal to) {

			// the prepared statement that will update the record
			Mock selectPst=new Mock(PreparedStatement.class);
			selectPst.expects(new InvokeOnceMatcher()).method("setQueryTimeout")
				.with(new IsEqual(new Integer(0)));
			selectPst.expects(new InvokeOnceMatcher()).method("setMaxRows")
				.with(new IsEqual(new Integer(0)));
			selectPst.expects(new InvokeOnceMatcher()).method("setString")
				.with(new IsEqual(new Integer(1)), new IsEqual("test_table"));
			selectPst.expects(new InvokeOnceMatcher()).method("executeQuery")
				.will(new ReturnStub(getResultSetWithRange(from, to)));
			selectPst.expects(new InvokeOnceMatcher()).method("close");
			registerMock(selectPst);
			return((PreparedStatement)selectPst.proxy());
		}

		protected void setupSuccesfulGetPK(Mock connMock,
			BigDecimal first, BigDecimal last) {

			connMock.expects(new InvokeOnceMatcher()).method("prepareStatement")
				.with(new IsEqual(UPDATE))
				.will(new ReturnStub(getUpdateStatementToReturn(1)));
			connMock.expects(new InvokeOnceMatcher()).method("prepareStatement")
				.with(new IsEqual(SELECT))
				.will(new ReturnStub(getSelectStatementWithRange(first, last)));
		}


		protected void setupDBMD(Mock connMock) {
			Mock dbMdMock=new Mock(DatabaseMetaData.class);
			dbMdMock.expects(new InvokeAtLeastOnceMatcher())
				.method("getDatabaseProductName")
				.will(new ReturnStub("UnknownProduct"));
			connMock.expects(new InvokeAtLeastOnceMatcher())
				.method("getMetaData")
				.will(new ReturnStub(dbMdMock.proxy()));
		}

	}

	public static class SuccessConnectionSource extends BaseConnectionSource {

		private static final int INCR=100;
		private static int startPoint=0;

		@Override
		protected void setupMock(Mock connMock, SpyConfig conf) {
			// autocommit will be enabled, and then disabled
			connMock.expects(new InvokeOnceMatcher()).method("setAutoCommit")
				.with(new IsEqual(Boolean.FALSE)).id("disableAutocommit");

			setupSuccesfulGetPK(connMock, new BigDecimal(startPoint),
				new BigDecimal((INCR+startPoint) - 1));
			// Increment the start point
			startPoint+=INCR;

			connMock.expects(new InvokeOnceMatcher()).method("commit");
			connMock.expects(new InvokeOnceMatcher()).method("setAutoCommit")
				.with(new IsEqual(Boolean.TRUE)).id("enableAutocommit");
			connMock.expects(new InvokeOnceMatcher()).method("close");

			setupDBMD(connMock);
		}

	}

	public static class MissingKeySource extends BaseConnectionSource {

		@Override
		protected void setupMock(Mock connMock, SpyConfig conf) {
			// autocommit will be enabled, and then disabled
			connMock.expects(new InvokeOnceMatcher()).method("setAutoCommit")
				.with(new IsEqual(Boolean.FALSE)).id("disableAutocommit");

			connMock.expects(new InvokeOnceMatcher()).method("prepareStatement")
				.with(new IsEqual(UPDATE))
				.will(new ReturnStub(getUpdateStatementToReturn(0)));

			connMock.expects(new InvokeOnceMatcher()).method("rollback");
			connMock.expects(new InvokeOnceMatcher()).method("setAutoCommit")
				.with(new IsEqual(Boolean.TRUE)).id("enableAutocommit");
			connMock.expects(new InvokeOnceMatcher()).method("close");

			setupDBMD(connMock);
		}

	}

}
