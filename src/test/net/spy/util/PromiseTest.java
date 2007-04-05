// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 1BE4657E-1110-11D9-BE97-000A957659CC

package net.spy.util;

import java.util.concurrent.Callable;

import net.spy.test.SyncThread;

import junit.framework.TestCase;

/**
 * Test the promise implementation.
 */
public class PromiseTest extends TestCase {

	/** 
	 * Test a promise that returns an int.
	 */
	public void testIntPromise() throws Exception {
		Promise<Integer> p=new IntPromise(17);
		assertTrue(String.valueOf(p).indexOf("not yet executed") > 0);

		Integer i1=p.get();
		Integer i2=p.get();
		assertNotNull(i1);
		assertNotNull(i2);

		// next time, it should have been executed
		assertFalse(String.valueOf(p).indexOf("not yet executed") > 0);

		assertEquals("First run", 17, i1.intValue());
		assertEquals("Second run", 17, i2.intValue());

		assertSame(i1, i2);
	}

	public void testConcurrentPromises() throws Throwable {
		final Promise<Integer> p=new IntPromise(17);
		int num=SyncThread.getDistinctResultCount(10, new Callable<Integer>() {
			public Integer call() throws Exception {
				return p.get();
			}});
		assertEquals(1, num);
	}

	public void testBrokenPromise() throws Exception {
		Promise<Object> p=new PromiseBreaker();
		try {
			Object o=p.get();
			fail("Broken promise gave me a value:  " + o);
		} catch(BrokenPromiseException e) {
			// pass
		}
		assertTrue("Got: " + String.valueOf(p),
			String.valueOf(p).indexOf("Broken Promise ") == 0);

		// Second time is a different code path
		try {
			Object o=p.get();
			fail("Broken promise gave me a value the second time:  " + o);
		} catch(BrokenPromiseException e) {
			// pass
		}
	}

	public void testNullPromise() throws Exception {
		Promise<Object> p=new NullPromise();
		assertNull(p.get());
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

	private class IntPromise extends Promise<Integer> {

		private int myInt=-1;

		public IntPromise(int what) {
			super();
			myInt=what;
		}

		@Override
		protected Integer execute() throws BrokenPromiseException {
			return new Integer(myInt++);
		}

	}

	private class PromiseBreaker extends Promise<Object> {
		public PromiseBreaker() {
			super();
		}

		@Override
		protected Object execute() throws BrokenPromiseException {
			throw new BrokenPromiseException("Fail");
		}
	}

	private class NullPromise extends Promise<Object> {
		public NullPromise() {
			super();
		}

		@Override
		protected Object execute() throws BrokenPromiseException {
			return(null);
		}
	}

}
