// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.util;

import java.util.concurrent.Callable;

import junit.framework.TestCase;

import net.spy.test.SyncThread;

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
		} catch(RuntimeException e) {
			// pass
		}
		assertTrue("Got: " + String.valueOf(p),
			String.valueOf(p).indexOf("Broken Promise ") == 0);

		// Second time is a different code path
		try {
			Object o=p.get();
			fail("Broken promise gave me a value the second time:  " + o);
		} catch(RuntimeException e) {
			// pass
		}
	}

	public void testNullPromise() throws Exception {
		Promise<Object> p=new NullPromise();
		assertNull(p.get());
		assertEquals("Promise {null}", String.valueOf(p));
	}

	public void testBrokenPromiseException() {
		RuntimeException e=new RuntimeException("test");
		assertEquals("test", e.getMessage());
		assertNull(e.getCause());

		Exception e2=new Exception();
		e=new RuntimeException("test", e2);
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
		protected Integer execute() {
			return new Integer(myInt++);
		}

	}

	private class PromiseBreaker extends Promise<Object> {
		public PromiseBreaker() {
			super();
		}

		@Override
		protected Object execute() {
			throw new RuntimeException("Fail");
		}
	}

	private class NullPromise extends Promise<Object> {
		public NullPromise() {
			super();
		}

		@Override
		protected Object execute() {
			return(null);
		}
	}

}
