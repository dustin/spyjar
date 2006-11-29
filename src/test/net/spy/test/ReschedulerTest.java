// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 239ED540-3CB0-4348-A880-FD87A68A2D18

package net.spy.test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import junit.framework.TestCase;

import net.spy.concurrent.Rescheduler;
import net.spy.concurrent.RetryableCallable;

/**
 * Test out the ol' rescheduler.
 */
public class ReschedulerTest extends TestCase {

	private Rescheduler sched=null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		sched=new Rescheduler(new InlineScheduledExecutorService());
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		sched.shutdown();
	}

	// Plain runnable (no special handling)
	public void testBasicRunnable() {
		TestRunnable tr=new TestRunnable();
		assertEquals(0, tr.runs);
		sched.execute(tr);
		assertEquals(1, tr.runs);
	}

	// Plain callable (no special handling)
	public void testBasicCallable() throws Exception {
		TestCallable<String> tc=new TestCallable<String>("X");
		assertEquals(0, tc.runs);
		Future<String> f=sched.submit(tc);
		assertEquals("X", f.get());
		assertEquals(1, tc.runs);
	}

	// A few retries and then success
	public void testRetryableCallable() throws Exception {
		TestRtryCallable<String> tc=new TestRtryCallable<String>("X", 2, 3);
		assertEquals(0, tc.runs);
		assertEquals(0, tc.retries);
		Future<String> f=sched.submit(tc);
		assertEquals("X", f.get());
		assertTrue(f.isDone());
		assertFalse(f.isCancelled());
		assertFalse(tc.gaveUp);
		assertEquals(3, tc.runs);
		assertEquals(2, tc.retries);
	}

	// A few retries and a failure
	public void testFailingRetryableCallable() throws Exception {
		TestRtryCallable<String> tc=new TestRtryCallable<String>("X", 4, 3);
		assertEquals(0, tc.runs);
		assertEquals(0, tc.retries);
		Future<String> f=sched.submit(tc);
		try {
			String s=f.get();
			fail("Expected failure, got " + s);
		} catch(ExecutionException e) {
			assertEquals("Too many failures", e.getMessage());
			StringWriter sw=new StringWriter();
			PrintWriter pw=new PrintWriter(sw);
			e.printStackTrace(pw);
			assertTrue("Hey, no also caused by",
					sw.toString().indexOf("Also caused by") > 0);
		}
		assertTrue(tc.gaveUp);
		assertEquals(4, tc.runs);
		assertEquals(3, tc.retries);
	}

	// Verify you can still return an exception from your callable
	public void testRetryableCallableReturningExc() throws Exception {
		ExecutionException e=new ExecutionException(new Exception());
		TestRtryCallable<ExecutionException> tc=
			new TestRtryCallable<ExecutionException>(e, 0, 1);
		assertEquals(0, tc.runs);
		assertEquals(0, tc.retries);
		Future<ExecutionException> f=sched.submit(tc);
		assertSame(e, f.get());
		assertFalse(tc.gaveUp);
		assertEquals(1, tc.runs);
		assertEquals(0, tc.retries);
	}

	// Verify you can return null from your callable
	public void testRetryableCallableReturningNull() throws Exception {
		TestRtryCallable<Object> tc=new TestRtryCallable<Object>(null, 1, 1);
		assertEquals(0, tc.runs);
		assertEquals(0, tc.retries);
		Future<Object> f=sched.submit(tc);
		assertNull(f.get());
		assertFalse(tc.gaveUp);
		assertEquals(2, tc.runs);
		assertEquals(1, tc.retries);
	}

	//
	// Support classes
	//

	static class TestRunnable implements Runnable {
		int runs=0;
		public void run() {
			runs++;
		}
	}

	public static class TestCallable<T> implements Callable<T> {
		public int runs=0;
		private T val=null;
		public TestCallable(T v) {
			super();
			val=v;
		}
		public T call() throws Exception {
			runs++;
			return val;
		}
	}

	public static class TestRtryCallable<T> extends TestCallable<T>
		implements RetryableCallable<T> {

		int retries=0;
		int maxRetries=0;
		int failures=0;
		public boolean gaveUp=false;

		public TestRtryCallable(T v, int fail, int nretries) {
			super(v);
			maxRetries=nretries;
			failures=fail;
		}

		public long getRetryDelay() {
			long rv=10;
			if(retries >= maxRetries) {
				rv=-1;
			}
			return rv;
		}

		public void givingUp() {
			gaveUp=true;
		}

		public void retrying() {
			retries++;
		}

		@Override
		public T call() throws Exception {
			T rv=super.call();
			if(--failures >= 0) {
				throw new Exception("Failed!");
			}
			return rv;
		}
		
	}
}
