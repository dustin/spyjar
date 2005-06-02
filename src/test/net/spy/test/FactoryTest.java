// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 3AFD430F-97A4-4080-8AAC-A5DCB45E0ABE

package net.spy.test;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.spy.factory.Instance;
import net.spy.factory.GenFactory;

/**
 * Test the generic factory code.
 */
public class FactoryTest extends TestCase {

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
		for(int i=0; i<1000; i++) {
			TestInstance ti=tf.getObject(i);
			assertEquals(ti.getId(), i);
		}

		// Try non-existing objects
		TestInstance ti=tf.getObject(1138);
		assertNull(ti);
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

		public TestFactory() {
			super("TestStuff", 10000);
		}

		public static TestFactory getInstance() {
			return(instance);
		}

		public Collection<TestInstance> getInstances() {
			Collection<TestInstance> rv=new ArrayList();
			for(int i=0; i<1000; i++) {
				rv.add(new TestInstance(i, "Test#" + i));
			}
			return(rv);
		}
	}

}
