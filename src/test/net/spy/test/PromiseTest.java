// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 1BE4657E-1110-11D9-BE97-000A957659CC

package net.spy.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.spy.util.BrokenPromiseException;
import net.spy.util.Promise;

/**
 * Test the promise implementation.
 */
public class PromiseTest extends TestCase {

	/**
	 * Get an instance of PromiseTest.
	 */
	public PromiseTest(String name) {
		super(name);
	}

	/** 
	 * Get the test suite.
	 * 
	 * @return this test
	 */
	public static Test suite() {
		return new TestSuite(PromiseTest.class);
	}

	/** 
	 * Test a promise that returns an int.
	 */
	public void testIntPromise() throws Exception {
		Promise p=new IntPromise(17);
		assertTrue(String.valueOf(p).indexOf("not yet executed") > 0);

		Integer i1=(Integer)p.getObject();
		Integer i2=(Integer)p.getObject();
		assertNotNull(i1);
		assertNotNull(i2);

		// next time, it should have been executed
		assertFalse(String.valueOf(p).indexOf("not yet executed") > 0);

		assertEquals("First run", 17, i1.intValue());
		assertEquals("Second run", 17, i2.intValue());

		assertSame(i1, i2);
	}

	public void testBrokenPromise() throws Exception {
		Promise p=new PromiseBreaker();
		try {
			Object o=p.getObject();
			fail("Broken promise gave me a value:  " + o);
		} catch(BrokenPromiseException e) {
			// pass
		}
		assertTrue("Got: " + String.valueOf(p),
			String.valueOf(p).indexOf("Broken Promise ") == 0);

		// Second time is a different code path
		try {
			Object o=p.getObject();
			fail("Broken promise gave me a value the second time:  " + o);
		} catch(BrokenPromiseException e) {
			// pass
		}
	}

	public void testNullPromise() throws Exception {
		Promise p=new NullPromise();
		assertNull(p.getObject());
		assertEquals("Promise {null}", String.valueOf(p));
	}

	public void testBrokenPromiseException() {
		BrokenPromiseException e=new BrokenPromiseException("test");
		assertEquals("test", e.getMessage());
		assertNull(e.getCause());

		Exception e2=new Exception();
		e=new BrokenPromiseException("test", e2);
		assertEquals("test", e.getMessage());
		assertSame(e2, e.getCause());
	}

	//
	// Private inner classes for testing.
	//

	private class IntPromise extends Promise {

		private int myInt=-1;

		public IntPromise(int what) {
			super();
			myInt=what;
		}

		protected Object execute() throws BrokenPromiseException {
			return new Integer(myInt++);
		}

	}

	private class PromiseBreaker extends Promise {
		public PromiseBreaker() {
			super();
		}

		protected Object execute() throws BrokenPromiseException {
			throw new BrokenPromiseException("Fail");
		}
	}

	private class NullPromise extends Promise {
		public NullPromise() {
			super();
		}

		protected Object execute() throws BrokenPromiseException {
			return(null);
		}
	}

}
