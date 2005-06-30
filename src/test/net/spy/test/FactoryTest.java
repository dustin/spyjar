// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 3AFD430F-97A4-4080-8AAC-A5DCB45E0ABE

package net.spy.test;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.spy.factory.GenFactory;
import net.spy.factory.Instance;

/**
 * Test the generic factory code.
 */
public class FactoryTest extends TestCase {

	private static final int NUM_OBJECTS=1000;

	/**
	 * Get an instance of FactoryTest.
	 */
	public FactoryTest(String name) {
		super(name);
	}

	/** 
	 * Get the test.
	 */
	public static Test suite() {
		return new TestSuite(FactoryTest.class);
	}

	/** 
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	/** 
	 * Test the factory thingy.
	 */
	public void testFactory() throws Exception {
		TestFactory tf=TestFactory.getInstance();

		// Try all the existing objects
		for(int i=0; i<NUM_OBJECTS; i++) {
			TestInstance ti=tf.getObject(i);
			assertNotNull("Null at " + i, ti);
			assertEquals(ti.getId(), i);
		}

		assertEquals(NUM_OBJECTS, tf.getObjects().size());

		// Try non-existing objects
		TestInstance ti=tf.getObject(NUM_OBJECTS + 138);
		assertNull(ti);

		// Get the thing to recache
		tf.recache();

		// Now that object should be there.
		ti=tf.getObject(NUM_OBJECTS + 138);
		assertEquals(ti.getId(), NUM_OBJECTS + 138);

		// And this object shouldn't
		ti=tf.getObject(138);
		assertNull(ti);
	}

	public void testInvalidConstructors() {
		try {
			TestFactory tf=new TestFactory(null, 10000);
			fail("Shouldn't be able to construct a factory with a null name: "
				+ tf);
		} catch(NullPointerException e) {
			assertNotNull(e.getMessage());
		}

		try {
			TestFactory tf=new TestFactory("Test", 0);
			fail("Shouldn't be able to construct a factory with 0 cache time: "
				+ tf);
		} catch(IllegalArgumentException e) {
			assertNotNull(e.getMessage());
		}

		try {
			TestFactory tf=new TestFactory("Test", -103);
			fail("Shouldn't be able to construct a factory with "
				+ "negative cache time: " + tf);
		} catch(IllegalArgumentException e) {
			assertNotNull(e.getMessage());
		}
	}

	private static class TestInstance extends Object implements Instance {
		private int oid=0;
		private String name=null;

		public TestInstance(int id, String s) {
			super();
			this.oid=id;
			this.name=s;
		}

		public int getId() {
			return(oid);
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

		private static TestFactory instance=new TestFactory();

		private int lastObject=0;

		public TestFactory(String nm, long t) {
			super(nm, t);
		}

		public TestFactory() {
			this("TestStuff", 10000);
		}

		public static TestFactory getInstance() {
			return(instance);
		}

		public Collection<TestInstance> getInstances() {
			Collection<TestInstance> rv=new ArrayList();
			int startId=lastObject;
			for(int i=0; i<NUM_OBJECTS; i++) {
				int id=i + startId;
				rv.add(new TestInstance(id, "Test#" + id));
				lastObject=id;
			}
			return(rv);
		}
	}

}
