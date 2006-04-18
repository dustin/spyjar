// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 09F800C1-6B2E-4638-9734-2E6D6F6A34BA

package net.spy.test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.spy.db.AbstractSavable;
import net.spy.db.ConnectionSource;
import net.spy.db.ConnectionSourceFactory;
import net.spy.db.Savable;
import net.spy.db.SaveContext;
import net.spy.db.SaveException;
import net.spy.db.Saver;
import net.spy.db.TransactionListener;
import net.spy.db.savables.CollectionSavable;
import net.spy.db.savables.SavableHashMap;
import net.spy.db.savables.SavableHashSet;
import net.spy.util.SpyConfig;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.constraint.IsInstanceOf;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.stub.ReturnStub;

/**
 * Test savable.
 */
public class SaverTest extends MockObjectTestCase {

	private SpyConfig successConfig=null;
	private SpyConfig failConfig=null;
	private SpyConfig isoConfig=null;
	private SpyConfig brokenConfig=null;

	private MockConnectionSource successSource=null;
	private MockConnectionSource failSource=null;
	private MockConnectionSource isoSource=null;
	/**
	 * Get an instance of SaverTest.
	 */
	public SaverTest(String name) {
		super(name);
	}

	/** 
	 * Set up the tests.
	 */
	protected void setUp() {
		ConnectionSourceFactory cnf=ConnectionSourceFactory.getInstance();

		successConfig=new SpyConfig();
		successConfig.put("dbConnectionSource",
			"net.spy.test.SaverTest$SuccessConnectionSource");
		successSource=
			(MockConnectionSource)cnf.getConnectionSource(successConfig);

		isoConfig=new SpyConfig();
		isoConfig.put("dbConnectionSource",
			"net.spy.test.SaverTest$MockConnectionSourceWithIso");
		isoSource=(MockConnectionSource)cnf.getConnectionSource(isoConfig);

		failConfig=new SpyConfig();
		failConfig.put("dbConnectionSource",
			"net.spy.test.SaverTest$MockFailingConnectionSource");
		failSource=(MockConnectionSource)cnf.getConnectionSource(failConfig);

		brokenConfig=new SpyConfig();
		brokenConfig.put("dbConnectionSource",
			"net.spy.test.SaverTest$BrokenConnectionSource");
	}

	/** 
	 * Shut down the tests.
	 */
	protected void tearDown() {
		successSource.clearSeenObjects();
		isoSource.clearSeenObjects();
		failSource.clearSeenObjects();
	}

	private void verifyAllConnections() throws Exception {
		successSource.verifyConnections();
		failSource.verifyConnections();
		isoSource.verifyConnections();
	}

	/** 
	 * Test a basic run with a success.
	 */
	public void testSuccessfulConnection() throws Exception {
		Connection conn=successSource.getConnection(successConfig);
		conn.setAutoCommit(false);
		conn.commit();
		conn.setAutoCommit(true);
		successSource.returnConnection(conn);

		verifyAllConnections();
	}

	/** 
	 * Test a basic run with a rollback.
	 */
	public void testFailingConnection() throws Exception {
		Connection conn=failSource.getConnection(failConfig);
		conn.setAutoCommit(false);
		conn.rollback();
		conn.setAutoCommit(true);
		failSource.returnConnection(conn);

		verifyAllConnections();
	}

	/** 
	 * Test an empty saver.
	 */
	public void testEmptySaver() throws Exception {
		Saver s=new Saver(successConfig);

		Mock mockSavable=mock(Savable.class);
		mockSavable.expects(once()).method("isNew")
			.will(returnValue(true));
		mockSavable.expects(once()).method("save")
			.with(new IsInstanceOf(Connection.class),
				new IsInstanceOf(SaveContext.class));
		// Return an empty list of pre savs
		mockSavable.expects(once()).method("getPreSavables")
			.with(new IsInstanceOf(SaveContext.class))
			.will(returnValue(Collections.EMPTY_LIST));
		// Return null for post savs, which should do the same thing
		mockSavable.expects(once()).method("getPostSavables")
			.with(new IsInstanceOf(SaveContext.class))
			.will(returnValue(null));

		s.save((Savable)mockSavable.proxy());

		verifyAllConnections();
	}

	/** 
	 * Test an empty saver with a context.
	 */
	@SuppressWarnings("unchecked")
	public void testEmptySaverWithContext() throws Exception {
		SaveContext context=new SaveContext();
		context.put("a", "b");
		Saver s=new Saver(successConfig, context);

		Mock mockSavable=mock(Savable.class);
		mockSavable.expects(once()).method("isNew")
			.will(returnValue(true));
		mockSavable.expects(once()).method("save")
			.with(new IsInstanceOf(Connection.class),
				eq(context));
		mockSavable.expects(once()).method("getPreSavables")
			.with(eq(context))
			.will(returnValue(Collections.EMPTY_LIST));
		mockSavable.expects(once()).method("getPostSavables")
			.with(eq(context))
			.will(returnValue(Collections.EMPTY_LIST));

		s.save((Savable)mockSavable.proxy());

		verifyAllConnections();
	}

	/** 
	 * Test an empty saver with an isolation level (and context).
	 */
	@SuppressWarnings("unchecked")
	public void testEmptySaverWithIsolation() throws Exception {
		SaveContext context=new SaveContext();
		context.put("c", "d");
		Saver s=new Saver(isoConfig, context);

		Mock mockSavable=mock(Savable.class);
		mockSavable.expects(once()).method("isNew")
			.will(returnValue(true));
		mockSavable.expects(once()).method("save")
			.with(new IsInstanceOf(Connection.class),
				eq(context));
		mockSavable.expects(once()).method("getPreSavables")
			.with(eq(context))
			.will(returnValue(Collections.EMPTY_LIST));
		mockSavable.expects(once()).method("getPostSavables")
			.with(eq(context))
			.will(returnValue(Collections.EMPTY_LIST));

		s.save((Savable)mockSavable.proxy(),
			Connection.TRANSACTION_SERIALIZABLE);

		verifyAllConnections();
	}


	private void failingSaverTest(Throwable t) throws Exception {
		Saver s=new Saver(failConfig);

		Mock mockSavable=mock(Savable.class);
		// These objects will present themselves as not new, but modified
		mockSavable.expects(once()).method("isNew")
			.will(returnValue(false));
		mockSavable.expects(once()).method("isModified")
			.will(returnValue(true));
		mockSavable.expects(once()).method("save")
			.with(new IsInstanceOf(Connection.class),
				new IsInstanceOf(SaveContext.class))
			.will(throwException(t));
		mockSavable.expects(once()).method("getPreSavables")
			.with(new IsInstanceOf(SaveContext.class))
			.will(returnValue(Collections.EMPTY_LIST));

		// Note: getPostSavables will not be called due to the exception

		try {
			s.save((Savable)mockSavable.proxy());
			fail("Expected a SaveException");
		} catch(SaveException e) {
			// pass
		}

		verifyAllConnections();
	}

	/** 
	 * Test an empty saver that fails.
	 */
	public void testEmptyFailingSaverWithSaveException() throws Exception {
		failingSaverTest(new SaveException("Testing a failure"));
	}

	/** 
	 * Test an empty saver that fails with a SQLException.
	 */
	public void testEmptyFailingSaverWithSQLException() throws Exception {
		failingSaverTest(new SQLException("Testing a failure"));
	}

	private void assertAllSaved(Collection c) {
		for(Iterator i=c.iterator(); i.hasNext(); ) {
			TestSavable ts=(TestSavable)i.next();
			assertTrue(ts.committed);
		}
	}

	/** 
	 * Test a complex sequence of savables.
	 */
	@SuppressWarnings("unchecked")
	public void testComplexSequence() throws Exception {
		// Our root object is going to be TestSavable 1
		TestSavable ts1=new TestSavable(1);

		// It's going to have a collection of preSavables
		ts1.preSavs.add(new TestSavable(2));
		ts1.preSavs.add(new TestSavable(3));

		// It's going to have a collection of and postsavables
		ts1.postSavs.add(new TestSavable(4));
		ts1.postSavs.add(new TestSavable(5));

		// One of the postsavables is going to have presavables
		TestSavable ts6=new TestSavable(6);
		ts6.preSavs.add(new TestSavable(7));
		ts6.preSavs.add(new TestSavable(8));
		ts1.postSavs.add(ts6);

		// And one postsavable with a presavable and a postsavable, but it
		// should, itself, not be saved.
		TestSavable ts9=new TestSavable(9);
		ts9.setNew(false);
		ts9.setModified(false);
		ts9.postSavs.add(new TestSavable(10));
		ts9.preSavs.add(new TestSavable(11));
		ts6.postSavs.add(ts9);
		// Add the same object again, make sure we don't try to save it the
		// second time
		ts9.postSavs.add(ts6);

		// OK, and for our final trick, we're going to add a collection
		Collection someSavables=new ArrayList();
		someSavables.add(new TestSavable(12));
		someSavables.add(new TestSavable(13));
		// Including one we've seen before.
		someSavables.add(ts6);
		ts9.postSavs.add(someSavables);

		// Perform the save
		SaveContext context=new SaveContext();
		context.put("sequence", new ArrayList());
		Saver s=new Saver(successConfig, context);
		s.save(ts1);

		// That should yield the following sequence:
		int seq[]={2, 3, 1, 4, 5, 7, 8, 6, 11, 10, 12, 13};
		ArrayList al=new ArrayList();
		for(int i=0; i<seq.length; i++) {
			al.add(new Integer(seq[i]));
		}

		assertEquals(al, context.get("sequence"));
		assertTrue(ts1.committed);

		verifyAllConnections();
	}

	/** 
	 * Test an object getting resaved.
	 */
	public void testResavable() throws Exception {
		Saver s=new Saver(successConfig);
		BasicSavable s1=new BasicSavable();
		assertFalse(s1.saved);
		s.save(s1);

		// After the first save, it should be considered saved
		assertTrue(s1.saved);
		s1.saved=false;
		s.save(s1);
		// A second save should not modify it
		assertFalse(s1.saved);

		// ...but if we consider it modified, we should be able to save again
		s1.modify();
		s.save(s1);
		assertTrue(s1.saved);
	}

	/** 
	 * Test a save with an invalid savable.
	 */
	@SuppressWarnings("unchecked")
	public void testInvalidObject() throws Exception {
		TestSavable ts1=new TestSavable(1);
		// This is some arbitrary object that is not savable, but more
		// importantly, it's also really big and will exercise the debug
		// stringifier a bit more.
		ts1.postSavs.add(new Integer(13));
		Saver s=new Saver(failConfig);
		try {
			s.save(ts1);
			fail("Shouldn't allow me to save this object.");
		} catch(SaveException e) {
			assertTrue(e.getMessage().startsWith("Invalid object type"));
		}
		assertFalse(ts1.committed);
		verifyAllConnections();
	}

	/** 
	 * Test a save with a null object.
	 */
	@SuppressWarnings("unchecked")
	public void testNullObject() throws Exception {
		TestSavable ts1=new TestSavable(1);
		// This is some arbitrary object that is not savable, but more
		// importantly, it's also really big and will exercise the debug
		// stringifier a bit more.
		ts1.postSavs.add(null);
		Saver s=new Saver(failConfig);
		try {
			s.save(ts1);
			fail("Shouldn't allow me to save this object.");
		} catch(NullPointerException e) {
			assertNotNull(e.getMessage());
			assertTrue(e.getMessage().startsWith("Got a null object"));
		}
		assertFalse(ts1.committed);
		verifyAllConnections();
	}

	/** 
	 * Test a save with a broken connection source.
	 */
	public void testBrokenConnSrc() throws Exception {
		TestSavable ts1=new TestSavable(1);
		Saver s=new Saver(brokenConfig);
		try {
			s.save(ts1);
			fail("Shouldn't allow me to save this object.");
		} catch(SaveException e) {
			assertNotNull(e.getMessage());
			assertEquals("Error saving object", e.getMessage());
		}
		assertFalse(ts1.committed);
		verifyAllConnections();
	}

	/** 
	 * Validate the right thing happens when we try too complicated of an
	 * object graph.
	 */
	@SuppressWarnings("unchecked")
	public void testExcessiveDepth() throws Exception {
		// Build out a deep object graph.  Max depth is 100
		TestSavable ts1=new TestSavable(1);
		TestSavable currentSavable=ts1;
		for(int i=2; i<110; i++) {
			TestSavable newSavable=new TestSavable(i);
			currentSavable.postSavs.add(newSavable);
			currentSavable=newSavable;
		}

		// Perform the save
		Saver s=new Saver(failConfig);
		try {
			s.save(ts1);
			fail("Shouldn't allow me to save really deep objects");
		} catch(SaveException e) {
			assertTrue(e.getMessage().startsWith("Recursing too deep!"));
		}
		verifyAllConnections();
	}

	/** 
	 * Test the CollectionSavable implementation.
	 */
	@SuppressWarnings("unchecked")
	public void testCollectionSavable() throws Exception {
		ArrayList al=new ArrayList();
		ArrayList expectedSequence=new ArrayList();
		for(int i=0; i<10; i++) {
			al.add(new TestSavable(i));
			expectedSequence.add(new Integer(i));
		}
		SaveContext ctx=new SaveContext();
		ctx.put("sequence", new ArrayList());
		Saver s=new Saver(successConfig, ctx);
		s.save(new CollectionSavable(al));

		ArrayList seenSequence=new ArrayList();
		for(Iterator i=al.iterator(); i.hasNext();) {
			TestSavable ts=(TestSavable)i.next();
			seenSequence.add(new Integer(ts.id));
		}
		assertAllSaved(al);

		assertEquals("Save order was wrong", expectedSequence, seenSequence);
	}

	@SuppressWarnings("unchecked")
	private void populateMapAndTest(SavableHashMap shm) throws Exception {
		shm.put("1", new TestSavable(1));
		shm.put("2", new TestSavable(2));

		Saver s=new Saver(successConfig);
		s.save(shm);
		assertAllSaved(shm.values());
	}

	/** 
	 * Test the savable HashMap implementation.
	 */
	public void testSavableHashMap() throws Exception {
		// Map constructor
		Map<String, Savable> m=new HashMap<String, Savable>();
		m.put("1", new TestSavable(1));
		m.put("2", new TestSavable(2));
		SavableHashMap<String, Savable> shm
			=new SavableHashMap<String, Savable>(m);
		shm.save((Connection)mock(Connection.class).proxy(),
			new SaveContext());

		Saver s=new Saver(successConfig);
		s.save(shm);
		assertAllSaved(m.values());

		populateMapAndTest(new SavableHashMap());
		populateMapAndTest(new SavableHashMap(2));
		populateMapAndTest(new SavableHashMap(2, 0.5f));
	}

	private void populateSetAndTest(SavableHashSet shs) throws Exception {
		shs.add(new TestSavable(1));
		shs.add(new TestSavable(2));

		Saver s=new Saver(successConfig);
		s.save(shs);
		assertAllSaved(shs);
	}

	/** 
	 * Test the savable HashSet implementation.
	 */
	public void testSavableHashSet() throws Exception {
		// Map constructor
		Collection<Savable> c=new HashSet<Savable>();
		c.add(new TestSavable(1));
		c.add(new TestSavable(2));
		SavableHashSet shs=new SavableHashSet(c);
		shs.save((Connection)mock(Connection.class).proxy(),
			new SaveContext());

		Saver s=new Saver(successConfig);
		s.save(shs);
		assertAllSaved(c);

		populateSetAndTest(new SavableHashSet());
		populateSetAndTest(new SavableHashSet(2));
		populateSetAndTest(new SavableHashSet(2, 0.5f));
	}

	/** 
	 * A connection source for mock connections.
	 */
	public static class SuccessConnectionSource extends MockConnectionSource {

		protected void setupMock(Mock connMock, SpyConfig conf) {
			// autocommit will be enabled, and then disabled
			connMock.expects(new InvokeOnceMatcher()).method("setAutoCommit")
				.with(new IsEqual(Boolean.FALSE)).id("disableAutocommit");
			connMock.expects(new InvokeOnceMatcher()).method("commit")
				.after("disableAutocommit").id("commitSuccess");
			connMock.expects(new InvokeOnceMatcher()).method("setAutoCommit")
				.with(new IsEqual(Boolean.TRUE)).after("commitSuccess");

			connMock.expects(new InvokeOnceMatcher()).method("close");
		}

	}

	public static class MockConnectionSourceWithIso
		extends MockConnectionSource {

		protected void setupMock(Mock connMock, SpyConfig conf) {
			// autocommit will be enabled, and then disabled
			connMock.expects(new InvokeOnceMatcher())
				.method("getTransactionIsolation")
				.will(new ReturnStub(
					new Integer(Connection.TRANSACTION_READ_UNCOMMITTED)));
			connMock.expects(new InvokeOnceMatcher())
				.method("setTransactionIsolation")
				.with(new IsEqual(
					new Integer(Connection.TRANSACTION_SERIALIZABLE)))
				.id("initialIsolationSet");
			connMock.expects(new InvokeOnceMatcher()).method("setAutoCommit")
				.with(new IsEqual(Boolean.FALSE)).id("disableAutocommit");
			connMock.expects(new InvokeOnceMatcher()).method("commit")
				.after("disableAutocommit").id("commitSuccess");
			connMock.expects(new InvokeOnceMatcher()).method("setAutoCommit")
				.with(new IsEqual(Boolean.TRUE)).after("commitSuccess")
				.id("resetAutoCommit");

			connMock.expects(new InvokeOnceMatcher())
				.method("setTransactionIsolation")
				.with(new IsEqual(
					new Integer(Connection.TRANSACTION_READ_UNCOMMITTED)))
				.after("resetAutoCommit")
				.id("resetIsolation");

			connMock.expects(new InvokeOnceMatcher()).method("close");
		}
	}

	public static class MockFailingConnectionSource
		extends MockConnectionSource {

		protected void setupMock(Mock connMock, SpyConfig conf) {
			// autocommit will be enabled, and then disabled
			connMock.expects(new InvokeOnceMatcher()).method("setAutoCommit")
				.with(new IsEqual(Boolean.FALSE)).id("disableAutocommit");
			connMock.expects(new InvokeOnceMatcher()).method("rollback")
				.after("disableAutocommit").id("rollbackFail");
			connMock.expects(new InvokeOnceMatcher()).method("setAutoCommit")
				.with(new IsEqual(Boolean.TRUE)).after("rollbackFail");

			connMock.expects(new InvokeOnceMatcher()).method("close");
		}
	}

	public static class BrokenConnectionSource
		extends Object implements ConnectionSource {

		public Connection getConnection(SpyConfig conf) throws SQLException {
			throw new SQLException("Can't get a connection");
		}

		public void returnConnection(Connection conn) {
			throw new RuntimeException("Can't returna connection here");
		}
	}

	private static class BasicSavable extends AbstractSavable {
		public boolean saved=false;

		public BasicSavable() {
			super();
			setNew(true);
		}

		public void setNew(boolean to) {
			super.setNew(to);
		}

		public void setModified(boolean to) {
			super.setModified(to);
		}

		public void save(Connection conn, SaveContext ctx) {
			saved=true;
		}
	}

	//
	// A flexible savable was can set up a predicable test with
	//
	private static final class TestSavable extends BasicSavable
		implements TransactionListener {

		public Collection<Savable> preSavs=null;
		public Collection postSavs=null;
		public int id=0;
		public boolean committed=false;

		public TestSavable(int i) {
			super();
			this.id=i;
			this.preSavs=new ArrayList<Savable>();
			this.postSavs=new ArrayList<Savable>();
		}

		public Collection<Savable> getPreSavables(SaveContext ctx) {
			return(preSavs);
		}

		@SuppressWarnings("unchecked")
		public Collection<Savable> getPostSavables(SaveContext ctx) {
			return(postSavs);
		}

		@SuppressWarnings("unchecked")
		public void save(Connection conn, SaveContext ctx) {
			super.save(conn, ctx);
			Collection sequence=(Collection)ctx.get("sequence");
			if(sequence != null) {
				sequence.add(new Integer(id));
			}
		}

		public String toString() {
			return "TestSavable is an object that gets exposes "
				+ "enough of itself to make for a pretty useful test suite";
		}

		public void transactionCommited() {
			super.transactionCommited();
			committed=true;
		}
	}

}
