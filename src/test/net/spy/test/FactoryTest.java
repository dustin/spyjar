// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 3AFD430F-97A4-4080-8AAC-A5DCB45E0ABE

package net.spy.test;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

import net.spy.cache.SpyCache;
import net.spy.factory.CacheKey;
import net.spy.factory.CacheRefresher;
import net.spy.factory.GenFactory;
import net.spy.factory.Instance;

/**
 * Test the generic factory code.
 */
public class FactoryTest extends TestCase {

	private static final int NUM_OBJECTS=1000;
	private static final String TEST_KEY = "TestStuff";

	private TestFactory tf=null;

	public void setUp() {
		tf=new TestFactory();
	}

	public void tearDown() {
		SpyCache.getInstance().uncache(TEST_KEY);
		if(CacheRefresher.getInstance() != null) {
			CacheRefresher.getInstance().shutdown();
		}
	}

	/** 
	 * Test the factory thingy.
	 */
	public void testFactory() throws Exception {
		// Try all the existing objects
		checkFirstPass();

		// Get the thing to recache
		tf.recache();

		checkSecondPass();
	}

	private void checkFirstPass() {
		for(int i=0; i<NUM_OBJECTS; i++) {
			TestInstance ti=tf.getObject(i);
			assertNotNull("Null at " + i, ti);
			assertEquals(ti.getId(), i);

			ti=tf.getObject("iprop", String.valueOf(i));
			assertNotNull("Null at iprop/" + i, ti);
			assertEquals(ti.getId(), i);
		}

		assertEquals(NUM_OBJECTS, tf.getObjects().size());

		// Try non-existing objects
		TestInstance ti=tf.getObject(NUM_OBJECTS + 138);
		assertNull(ti);
		ti=tf.getObject("iprop", String.valueOf(NUM_OBJECTS + 138));
		assertNull("Null at iprop/" + (NUM_OBJECTS+138), ti);

		assertEquals(1, tf.numRuns);
	}

	private void checkSecondPass() {
		// Now that object should be there.
		TestInstance ti=tf.getObject(NUM_OBJECTS + 138);
		assertNotNull("Was null on second pass", ti);
		assertEquals(ti.getId(), NUM_OBJECTS + 138);

		// And this object shouldn't
		ti=tf.getObject(138);
		assertNull(ti);

		assertEquals(2, tf.numRuns);
	}

	/** 
	 * Test the factory thingy with delayed recaches.
	 */
	public void testFactoryDelayedRecache() throws Exception {

		checkFirstPass();

		// Tell the thing to recache
		CacheRefresher cc=CacheRefresher.getInstance();
		cc.recache(tf, System.currentTimeMillis(), 100);

		// Still should have the same stuff
		checkFirstPass();

		// Let the stuff have time to recache.
		Thread.sleep(250);

		checkSecondPass();
	}

	/** 
	 * Test the factory thingy with double-delayed recaches.
	 */
	public void testFactoryDoubleDelayedRecache() throws Exception {

		checkFirstPass();

		// Tell the thing to recache
		CacheRefresher cc=CacheRefresher.getInstance();
		cc.recache(tf, System.currentTimeMillis(), 100);

		// Still should have the same stuff
		checkFirstPass();

		// Let a little time pass and then recache.
		Thread.sleep(50);
		cc.recache(tf, System.currentTimeMillis(), 100);

		// Let a little more time pass and then validate we haven't recached
		Thread.sleep(75);
		checkFirstPass();

		// With a bit more time passed, we should be on the new set.
		Thread.sleep(100);
		checkSecondPass();
	}

	public void testInvalidCacheRefresherAssignment() {
		try {
			CacheRefresher cc=CacheRefresher.getInstance();
			CacheRefresher.setInstance(cc);
			fail("Overwrite cache refresher instance");
		} catch(IllegalStateException e) {
			assertEquals("Attempting to overwrite cache refresher instance",
					e.getMessage());
		}
	}

	public void testInvalidConstructors() {
		try {
			TestFactory t=new TestFactory(null, 10000);
			fail("Shouldn't be able to construct a factory with a null name: "
				+ t);
		} catch(NullPointerException e) {
			assertNotNull(e.getMessage());
		}

		try {
			TestFactory t=new TestFactory("Test", 0);
			fail("Shouldn't be able to construct a factory with 0 cache time: "
				+ t);
		} catch(IllegalArgumentException e) {
			assertNotNull(e.getMessage());
		}

		try {
			TestFactory t=new TestFactory("Test", -103);
			fail("Shouldn't be able to construct a factory with "
				+ "negative cache time: " + t);
		} catch(IllegalArgumentException e) {
			assertNotNull(e.getMessage());
		}
	}

	public static class TestInstance extends Object implements Instance {
		private int oid=0;
		public TestInstance(int id) {
			super();
			this.oid=id;
		}

		public int getId() {
			return(oid);
		}

		@CacheKey(name="iprop")
		public String getIndexedProp() {
			return String.valueOf(oid);
		}

		public String getNonindexedProp() {
			return String.valueOf(0-oid);
		}

		public int hashCode() {
			return(oid);
		}

		public boolean equals(Object o) {
			boolean rv=false;
			if(o instanceof TestInstance) {
				TestInstance ti=(TestInstance)o;
				rv=oid == ti.oid;
			}
			return(rv);
		}
	}

	private static class TestFactory extends GenFactory<TestInstance> {

		private int lastObject=0;
		public int numRuns=0;

		public TestFactory(String nm, long t) {
			super(nm, t);
		}

		public TestFactory() {
			this(TEST_KEY, 10000);
		}

		public Collection<TestInstance> getInstances() {
			Collection<TestInstance> rv=new ArrayList<TestInstance>();
			int startId=lastObject;
			for(int i=0; i<NUM_OBJECTS; i++) {
				int id=i + startId;
				rv.add(new TestInstance(id));
				lastObject=id;
			}
			numRuns++;
			return(rv);
		}
	}

}
