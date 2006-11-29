// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 79AE962F-5679-46E3-97A7-93D91A8AD7C7

package net.spy.test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.stub.ReturnStub;

import net.spy.concurrent.SynchronizationObject;
import net.spy.db.AbstractSavable;
import net.spy.db.QuerySelector;
import net.spy.db.SaveContext;
import net.spy.db.SaveException;
import net.spy.db.TransactionPipeline;
import net.spy.test.db.DeleteTest;
import net.spy.util.SpyConfig;

/**
 * Test the transaction pipeline.
 */
public class TransactionPipelineTest extends MockObjectTestCase {

	private SpyConfig successConfig=null;

	@Override
	protected void setUp() {
		successConfig=new SpyConfig();
		successConfig.put("dbConnectionSource",
			"net.spy.test.TransactionPipelineTest$SuccessConnectionSource");
	}

	/**
	 * Test a simple transaction.
	 */
	public void testSimpleTransaction() throws Exception {
		TransactionPipeline tp=new TransactionPipeline();
		TestSavable ts=new TestSavable("testSimpleTransaction");
		assertTrue(ts.isNew());
		tp.addTransaction(ts, successConfig);
		ts.so.waitUntilNotNull(1, TimeUnit.SECONDS);
		assertFalse(ts.isNew());
		tp.shutdown();
	}

	/**
	 * Test using a future to monitor the transaction.
	 */
	public void testSimpleTransactionFuture() throws Exception {
		TransactionPipeline tp=new TransactionPipeline();
		TestSavable ts=new TestSavable("testSimpleTransactionFuture");
		assertTrue(ts.isNew());
		ScheduledFuture<?> f=tp.addTransaction(ts, successConfig);
		try {
			f.get(25, TimeUnit.MILLISECONDS);
			fail("Shouldn't got a return value in the first 25ms");
		} catch (TimeoutException e) {
			// expected
		}
		f.get(750, TimeUnit.MILLISECONDS);
		assertFalse(ts.isNew());
		tp.shutdown();
	}

	/**
	 * Test two transactions.
	 */
	public void testTwoTransactions() throws Exception {
		TransactionPipeline tp=new TransactionPipeline();
		TestSavable ts1=new TestSavable("testTwoTransactions(1)");
		assertTrue(ts1.isNew());
		tp.addTransaction(ts1, successConfig);
		Thread.sleep(250);
		assertTrue(ts1.isNew());
		TestSavable ts2=new TestSavable("testTwoTransactions(2)");
		ScheduledFuture<?>f=tp.addTransaction(ts2, successConfig);
		ts1.so.waitUntilNotNull(1, TimeUnit.SECONDS);
		assertFalse(ts1.isNew());
		assertTrue(ts2.isNew());
		f.get(350, TimeUnit.MILLISECONDS);
		assertFalse(ts2.isNew());
		tp.shutdown();
	}

	private static class TestSavable extends AbstractSavable {

		private String which=null;
		public SynchronizationObject<Boolean> so=
			new SynchronizationObject<Boolean>(null);

		public TestSavable(String w) {
			super();
			setModified(false);
			setNew(true);
			which=w;
		}

		public void save(Connection conn, SaveContext ctx)
			throws SaveException, SQLException {

			try {
				DeleteTest dt=new DeleteTest(conn);
				dt.setSomeColumn(1);
				dt.executeUpdate();
			} catch(Throwable t) {
				getLogger().error("Problem saving " + which, t);
				if(t instanceof RuntimeException) {
					throw(RuntimeException)t;
				} else if(t instanceof Error) {
					throw(Error)t;
				} else if(t instanceof SQLException) {
					throw(SQLException)t;
				} else if(t instanceof SaveException) {
					throw(SaveException)t;
				}
			}
		}

		@Override
		public void transactionCommited() {
			super.transactionCommited();
			so.set(Boolean.TRUE);
		}

	}

	/** 
	 * A connection source for mock connections.
	 */
	public static class SuccessConnectionSource extends MockConnectionSource {

		@Override
		protected void setupMock(Mock connMock, SpyConfig conf) {
			// autocommit will be enabled, and then disabled
			connMock.expects(new InvokeOnceMatcher()).method("setAutoCommit")
				.with(new IsEqual(Boolean.FALSE)).id("disableAutocommit");
			connMock.expects(new InvokeOnceMatcher()).method("commit")
				.after("disableAutocommit").id("commitSuccess");
			connMock.expects(new InvokeOnceMatcher()).method("setAutoCommit")
				.with(new IsEqual(Boolean.TRUE)).after("commitSuccess");

	        Mock mdMock=new Mock(DatabaseMetaData.class);
	        mdMock.expects(new InvokeOnceMatcher())
	            .method("getDatabaseProductName")
	            .will(new ReturnStub("Unknown database"));
	        connMock.expects(new InvokeOnceMatcher())
	                .method("getMetaData")
	                .will(new ReturnStub(mdMock.proxy()));

			Mock pstMock=new Mock(PreparedStatement.class);
			registerMock(pstMock);
			pstMock.expects(new InvokeOnceMatcher()).method("setInt")
				.with(new IsEqual(1), new IsEqual(1));
			pstMock.expects(new InvokeOnceMatcher())
				.method("setQueryTimeout")
				.with(new IsEqual(0));
			pstMock.expects(new InvokeOnceMatcher())
				.method("setMaxRows")
				.with(new IsEqual(0));
			pstMock.expects(new InvokeOnceMatcher()).method("executeUpdate")
				.will(new ReturnStub(1));

			try {
				connMock.expects(new InvokeOnceMatcher())
					.method("prepareStatement")
					.with(new IsEqual(new DeleteTest(new SpyConfig())
						.getRegisteredQueries()
						.get(QuerySelector.DEFAULT_QUERY)))
					.will(new ReturnStub(pstMock.proxy()));
			} catch (SQLException e) {
				throw new RuntimeException("Couldn't set up delete", e);
			}
			connMock.expects(new InvokeOnceMatcher()).method("close");
		}

	}
}
