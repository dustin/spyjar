// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 239ED540-3CB0-4348-A880-FD87A68A2D18

package net.spy.concurrent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.TestCase;

/**
 * Test out the ol' rescheduler.
 */
public class ReschedulerTest extends TestCase {

	private Rescheduler schedInline=null;
	private Rescheduler schedPooled=null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		schedInline=new Rescheduler(new InlineScheduledExecutorService());
		schedPooled=new Rescheduler(new ScheduledThreadPoolExecutor(3));
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		schedInline.shutdown();
		assertTrue(schedInline.awaitTermination(
				Long.MAX_VALUE, TimeUnit.MILLISECONDS));
		schedPooled.shutdown();
		assertTrue(schedPooled.awaitTermination(
				Long.MAX_VALUE, TimeUnit.MILLISECONDS));
		// Note:  This is always true because the inline scheduler never says no
		assertFalse(schedInline.isShutdown());
		assertTrue(schedPooled.isShutdown());
	}

	// Plain runnable (no special handling)
	public void testBasicRunnable() {
		TestRunnable tr=new TestRunnable();
		assertEquals(0, tr.runs);
		schedInline.execute(tr);
		assertEquals(1, tr.runs);
	}

	// Plain runnable with value (no special handling)
	public void testBasicRunnableWithValue() throws Exception {
		TestRunnable tr=new TestRunnable();
		assertEquals(0, tr.runs);
		Future<String> f=schedInline.submit(tr, "X");
		assertEquals("X", f.get());
		assertEquals(1, tr.runs);
	}

	// Plain callable (no special handling)
	public void testBasicCallable() throws Exception {
		TestCallable<String> tc=new TestCallable<String>("X");
		assertEquals(0, tc.runs);
		Future<String> f=schedInline.submit(tc);
		assertEquals("X", f.get());
		assertEquals(1, tc.runs);
	}

	// Plain callable (no special handling)
	public void testBasicScheduledCallable() throws Exception {
		TestCallable<String> tc=new TestCallable<String>("X");
		assertEquals(0, tc.runs);
		Future<String> f=schedInline.schedule(tc, 10, TimeUnit.MILLISECONDS);
		assertEquals("X", f.get());
		assertEquals(1, tc.runs);
	}

	// A few retries and then success
	public void testRetryableCallable() throws Exception {
		TestRtryCallable<String> tc=new TestRtryCallable<String>("X", 2, 3);
		assertEquals(0, tc.runs);
		assertEquals(0, tc.retries);
		Future<String> f=schedPooled.submit(tc);
		assertEquals("X", f.get());
		assertTrue(f.isDone());
		assertFalse(f.isCancelled());
		assertFalse(tc.gaveUp);
		assertEquals("X", tc.result);
		assertEquals(3, tc.runs);
		assertEquals(2, tc.retries);
	}

	public void testScheduledFutureSorting() throws Exception {
		TestRtryCallable<String> tc=new TestRtryCallable<String>("X", 2, 3);
		ScheduledFuture<String> f1=schedPooled.schedule(
				tc, 10, TimeUnit.MILLISECONDS);
		ScheduledFuture<String> f2=schedPooled.schedule(
				tc, 100, TimeUnit.MILLISECONDS);
		assertTrue(f1.compareTo(f2) < 0);
	}

	public void testShutdownNow() throws Exception {
		TestRtryCallable<String> tc=new TestRtryCallable<String>("X", 2, 3);
		schedPooled.schedule(tc, 1, TimeUnit.SECONDS);
		schedPooled.schedule(tc, 2, TimeUnit.SECONDS);
		List<Runnable> l=schedPooled.shutdownNow();
		assertEquals(2, l.size());
		Thread.sleep(100);
		assertTrue(schedPooled.isTerminated());
	}

	public void testFixedRateRunnable() throws Exception {
		TestRunnable r=new TestRunnable();
		ScheduledFuture<?> f=schedPooled.scheduleAtFixedRate(r,
				10, 10, TimeUnit.MILLISECONDS);
		assertFalse(f.isDone());
		assertEquals(0, r.runs);
		Thread.sleep(20);
		assertTrue(r.runs > 0);
		assertFalse(f.isCancelled());
		f.cancel(true);
		assertTrue(f.isCancelled());
	}

	public void testFixedDelayRunnable() throws Exception {
		TestRunnable r=new TestRunnable();
		ScheduledFuture<?> f=schedPooled.scheduleWithFixedDelay(r,
				10, 10, TimeUnit.MILLISECONDS);
		assertFalse(f.isDone());
		assertEquals(0, r.runs);
		Thread.sleep(20);
		assertTrue(r.runs > 0);
		assertFalse(f.isCancelled());
		f.cancel(true);
		assertTrue(f.isCancelled());
	}

	@SuppressWarnings("unchecked") // java 1.5 screwed up invokeAll's definition
	public void testInvokeAll() throws Exception {
		Collection callables=Arrays.asList(
				new TestCallable<String>("A"),
				new TestCallable<String>("B"),
				new TestCallable<String>("C"));

		List<String> expected=new ArrayList<String>();
		expected.addAll(Arrays.asList("A", "B", "C"));

		List<String> got=new ArrayList<String>();
		List<Future<String>> res=schedInline.invokeAll(callables);
		for(Future<String> f : res) {
			got.add(f.get());
		}

		assertEquals(expected, got);
	}

	@SuppressWarnings("unchecked") // java 1.5 screwed up invokeAll's definition
	public void testInvokeAllWithRetry() throws Exception {
		Collection callables=Arrays.asList(
				new TestRtryCallable<String>("A", 2, 3),
				new TestRtryCallable<String>("B", 2, 3),
				new TestRtryCallable<String>("C", 2, 3));

		List<String> expected=new ArrayList<String>();
		expected.addAll(Arrays.asList("A", "B", "C"));

		List<String> got=new ArrayList<String>();
		List<Future<String>> res=schedPooled.invokeAll(callables);
		for(Future<String> f : res) {
			got.add(f.get());
		}

		assertEquals(expected, got);
	}

	@SuppressWarnings("unchecked") // java 1.5 screwed up invokeAll's definition
	public void testInvokeAllWithRetryTimeout() throws Exception {
		Collection callables=Arrays.asList(
				new TestRtryCallable<String>("A", 1, 9, 10),
				new TestRtryCallable<String>("B", 5, 9, 100),
				new TestRtryCallable<String>("C", 7, 9, 1000));

		// Only one of these should finish.
		List<String> expected=new ArrayList<String>();
		expected.addAll(Arrays.asList("A"));

		List<String> got=new ArrayList<String>();
		List<Future<String>> res=schedPooled.invokeAll(callables,
				20, TimeUnit.MILLISECONDS);
		for(Future<String> f : res) {
			if(f.isDone()) {
				got.add(f.get());
			}
		}

		assertEquals(expected, got);
	}

	@SuppressWarnings("unchecked") // java 1.5 screwed up invokeAll's definition
	public void testInvokeAllTimeout() throws Exception {
		Collection callables=Arrays.asList(
				new TestCallable<String>("A"),
				new TestCallable<String>("B"),
				new TestCallable<String>("C"));

		Set<String> expected=new HashSet<String>();
		expected.addAll(Arrays.asList("A", "B", "C"));

		Set<String> got=new HashSet<String>();
		List<Future<String>> res=schedInline.invokeAll(callables,
				10, TimeUnit.MILLISECONDS);
		for(Future<String> f : res) {
			got.add(f.get());
		}

		assertEquals(expected, got);
	}

	public void testInvokeAny() throws Exception {
		// java 1.5 screwed up invokeAny's definition
		@SuppressWarnings("unchecked")
		Collection c=Arrays.asList(
				new TestCallable<String>("A"),
				new TestCallable<String>("B"),
				new TestCallable<String>("C"));
		// java 1.5 screwed up invokeAny's definition
		@SuppressWarnings("unchecked")
		Collection<Callable<String>> callables=c;

		Set<String> expected=new HashSet<String>();
		expected.addAll(Arrays.asList("A", "B", "C"));

		String res=schedInline.invokeAny(callables);

		assertTrue(expected.contains(res));
	}

	public void testInvokeAnyTimeout() throws Exception {
		// java 1.5 screwed up invokeAny's definition
		@SuppressWarnings("unchecked")
		Collection<Callable<String>> callables=new ArrayList<Callable<String>>(
				Arrays.asList(
				new TestCallable<String>("A"),
				new TestCallable<String>("B", 30),
				new TestCallable<String>("C", 30)));

		String res=schedPooled.invokeAny(callables, 10, TimeUnit.MILLISECONDS);

		assertEquals("A", res);
	}

	public void testInvokeAnyWithRetryTimeoutTimedOut() throws Exception {
		// java 1.5 screwed up invokeAny's definition
		@SuppressWarnings("unchecked")
		Collection<Callable<String>> callables=new ArrayList<Callable<String>>(
				Arrays.asList(
				new TestRtryCallable<String>("A", 5, 9, 1000),
				new TestRtryCallable<String>("B", 5, 9, 1000),
				new TestRtryCallable<String>("C", 5, 9, 1000)));

		try {
			String res=schedPooled.invokeAny(
					callables, 10, TimeUnit.MILLISECONDS);
			fail("Expected timeout, got " + res);
		} catch(TimeoutException e) {
			// OK
		}
	}

	public void testInvokeAnyException() throws Exception {
		// java 1.5 screwed up invokeAny's definition
		@SuppressWarnings("unchecked")
		Collection<Callable<String>> callables=new ArrayList<Callable<String>>(
				Arrays.asList(
				new TestRtryCallable<String>("A", 4, 3),
				new TestRtryCallable<String>("B", 4, 3),
				new TestRtryCallable<String>("C", 4, 3)));

		try {
			String res=schedPooled.invokeAny(callables);
			fail("Expected exception, got " + res);
		} catch(ExecutionException e) {
			// OK
		}
	}

	public void testInvokeAnyExceptionTimeout() throws Exception {
		// java 1.5 screwed up invokeAny's definition
		@SuppressWarnings("unchecked")
		Collection<Callable<String>> callables=new ArrayList<Callable<String>>();
		for(int i=0; i<3; i++) {
			callables.add(new Callable<String>(){
				public String call() throws Exception {
					throw new Exception("Nope");
				}});
		}

		try {
			String res=schedPooled.invokeAny(
					callables, 10, TimeUnit.MILLISECONDS);
			fail("Expected exception, got " + res);
		} catch(ExecutionException e) {
			// OK
		}
	}

	@SuppressWarnings("unchecked") // java 1.5 screwed up invokeAll's definition
	public void testInvokeAnyWithRetry() throws Exception {
		Collection<Callable<String>> callables=new ArrayList<Callable<String>>(
				Arrays.asList(
				new TestRtryCallable<String>("A", 4, 5),
				new TestRtryCallable<String>("B", 6, 5),
				new TestRtryCallable<String>("C", 3, 5)));

		String res=schedPooled.invokeAny(callables);

		assertEquals("Got " + res + ", expected C", "C", res);
	}

	@SuppressWarnings("unchecked") // java 1.5 screwed up invokeAll's definition
	public void testInvokeAnyWithRetryTimeout() throws Exception {
		Collection<Callable<String>> callables=new ArrayList<Callable<String>>(
				Arrays.asList(
				new TestRtryCallable<String>("A", 4, 3),
				new TestRtryCallable<String>("B", 1, 3),
				new TestRtryCallable<String>("C", 3, 5)));

		String res=schedPooled.invokeAny(callables, 10, TimeUnit.SECONDS);

		assertEquals("B", res);
	}

	// A few retries and then success
	public void testCancellingRetryableCallable() throws Exception {
		TestRtryCallable<String> tc=new TestRtryCallable<String>("X", 2, 3);
		assertEquals(0, tc.runs);
		assertEquals(0, tc.retries);
		Future<String> f=schedPooled.submit(tc);
		f.cancel(true);
		try {
			Object o=f.get();
			fail("expected cancellation, got " + o);
		} catch(CancellationException e) {
			// ok
		}
	}

	// A few retries and a failure
	public void testFailingRetryableCallable() throws Exception {
		TestRtryCallable<String> tc=new TestRtryCallable<String>("X", 4, 3);
		assertEquals(0, tc.runs);
		assertEquals(0, tc.retries);
		Future<String> f=schedPooled.submit(tc);
		try {
			String s=f.get();
			fail("Expected failure, got " + s);
		} catch(ExecutionException e) {
			assertEquals("Too many failures", e.getMessage());
			assertEquals(4,
					((CompositeExecutorException)e).getExceptions().size());
			StringWriter sw=new StringWriter();
			PrintWriter pw=new PrintWriter(sw);
			e.printStackTrace(pw);
			assertTrue("Hey, no also caused by",
					sw.toString().indexOf("Also caused by") > 0);
			assertSame(e, tc.result);
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
		Future<ExecutionException> f=schedPooled.submit(tc);
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
		Future<Object> f=schedPooled.submit(tc);
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
		private long sleeptime=0;
		public TestCallable(T v) {
			this(v, 0);
		}
		public TestCallable(T v, long st) {
			super();
			val=v;
			sleeptime=st;
		}
		public T call() throws Exception {
			if(sleeptime > 0) {
				Thread.sleep(sleeptime);
			}
			runs++;
			return val;
		}
	}

	public static class TestRtryCallable<T> extends TestCallable<T>
		implements RetryableCallable<T> {

		int retries=0;
		int maxRetries=0;
		int failures=0;
		Object result=null;
		private long baseDelay=10;
		public boolean gaveUp=false;

		public TestRtryCallable(T v, int fail, int nretries, long delay) {
			super(v);
			maxRetries=nretries;
			failures=fail;
		}

		public TestRtryCallable(T v, int fail, int nretries) {
			this(v, fail, nretries, 10);
		}

		public long getRetryDelay() {
			long rv=baseDelay;
			if(retries >= maxRetries) {
				rv=-1;
			}
			return rv;
		}

		public void onComplete(boolean success, Object res) {
			gaveUp=!success;
			result=res;
		}

		public void onExecutionException(ExecutionException exception) {
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
