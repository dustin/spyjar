// Copyright (c) 2007  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 7436C342-74F9-4818-B91A-F917203F8059
package net.spy.test;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import net.spy.concurrent.ScheduledCompletionService;
import net.spy.concurrent.ScheduledExecutorCompletionService;

/**
 * Test the scheduled executor completion service.
 */
public class ScheduledExecutorCompletionServiceTest extends TestCase {

	private ScheduledThreadPoolExecutor tpe=null;
	private ScheduledCompletionService<Object> scs=null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tpe=new ScheduledThreadPoolExecutor(2);
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
}
