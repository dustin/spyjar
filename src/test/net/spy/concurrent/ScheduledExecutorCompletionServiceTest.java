// Copyright (c) 2007  Dustin Sallings <dustin@spy.net>
package net.spy.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

/**
 * Test the scheduled executor completion service.
 */
public class ScheduledExecutorCompletionServiceTest extends TestCase {

	private ScheduledExecutorService tpe=null;
	private ScheduledCompletionService<Object> scs=null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tpe=new Rescheduler(new ScheduledThreadPoolExecutor(2));
		scs=new ScheduledExecutorCompletionService<Object>(tpe);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		tpe.shutdown();
		scs=null;
	}

	public void testRunnableNow() throws Exception {
		Object rv=new Object();
		scs.submit(new TestRunnable(), rv);
		Future<Object> f=scs.take();
		assertSame(rv, f.get());
	}

	public void testCallableNow() throws Exception {
		Object rv=new Object();
		scs.submit(new TestCallable(rv));
		Future<Object> f=scs.take();
		assertSame(rv, f.get());
	}

	public void testRetryableCallableNow() throws Exception {
		Object rv=new Object();
		scs.submit(new TestRetryableCallable(rv));
		Future<Object> f=scs.take();
		assertSame(rv, f.get());
	}

	public void testScheduledRunnable() throws Exception {
		scs.schedule(new TestRunnable(), 100, TimeUnit.MILLISECONDS);
		assertNull(scs.poll());
		assertNull(scs.poll(10, TimeUnit.MILLISECONDS));
		Future<Object> f=scs.poll(1000, TimeUnit.MILLISECONDS);
		assertNull(f.get()); // This shouldn't throw an exception
	}

	public void testScheduledFuture() throws Exception {
		Object rv=new Object();
		scs.schedule(new TestCallable(rv), 100, TimeUnit.MILLISECONDS);
		assertNull(scs.poll());
		assertNull(scs.poll(10, TimeUnit.MILLISECONDS));
		Future<Object> f=scs.take();
		assertSame(rv, f.get());
	}

	public void testScheduledRetryableCallable() throws Exception {
		Object rv=new Object();
		scs.schedule(new TestRetryableCallable(rv), 100, TimeUnit.MILLISECONDS);
		assertNull(scs.poll());
		Future<Object> f=scs.take();
		assertSame(rv, f.get());
	}

	public void testFailedScheduledRetryableCallable() throws Exception {
		Object rv=new Object();
		scs.schedule(new TestRetryableCallable(rv, 2), 100,
				TimeUnit.MILLISECONDS);
		assertNull(scs.poll());
		Future<Object> f=scs.take();
		try {
			Object x=f.get();
			fail("Expected failure, got " + x);
		} catch(CompositeExecutorException e) {
			assertEquals("Too many failures", e.getMessage());
			assertEquals(2, e.getExceptions().size());
			for(ExecutionException t : e.getExceptions()) {
				assertEquals("Damn!", t.getCause().getMessage());
				assertSame(Exception.class, t.getCause().getClass());
			}
		}
	}

	static class TestRunnable implements Runnable {
		public void run() {
			// look, I ran
		}
	}

	static class TestCallable implements Callable<Object> {
		private Object rv=null;
		public TestCallable(Object o) {
			super();
			rv=o;
		}
		public Object call() throws Exception {
			return rv;
		}
	}

	static class TestRetryableCallable
		extends TestCallable implements RetryableCallable<Object> {
		int failures=0;
		int maxFailures=0;
		private long retryTime=10;

		public TestRetryableCallable(Object o, int m) {
			super(o);
			maxFailures=m;
		}

		public TestRetryableCallable(Object o) {
			this(o, 4);
		}

		@Override
		public Object call() throws Exception {
			if(failures < 3) {
				failures++;
				throw new Exception("Damn!");
			}
			return super.call();
		}

		public long getRetryDelay() {
			return retryTime;
		}

		public void onComplete(boolean success, Object res) {
			// OK
		}

		public void onExecutionException(ExecutionException exception) {
			if(++failures >= maxFailures) {
				retryTime=-1;
			}
		}
	}
}
