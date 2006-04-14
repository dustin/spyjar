// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 293B0664-1110-11D9-8706-000A957659CC

package net.spy.test;

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

import junit.framework.TestCase;

import net.spy.SpyObject;
import net.spy.util.ThreadPool;

/**
 * Test the ThreadPool.
 */
public class ThreadPoolTest extends TestCase {
	// Thread IDs (inner classes can't have static fields)
	private static int ids=0;

	/** 
	 * Test the constructors.
	 */
	public void testConstructors() {
		try {
			ThreadPool tp=new ThreadPool("Test1", 1, 1, Thread.MIN_PRIORITY-1);
			fail("Was able to set a priority of " + (Thread.MIN_PRIORITY-1));
			tp.shutdown();
		} catch(IllegalArgumentException e) {
			// pass
		}
		try {
			ThreadPool tp=new ThreadPool("Test2", 1, 1, Thread.MAX_PRIORITY+1);
			fail("Was able to set a priority of " + (Thread.MAX_PRIORITY+1));
			tp.shutdown();
		} catch(IllegalArgumentException e) {
			// pass
		}
		ThreadPool tp=new ThreadPool("Test3", 1);
		tp.shutdown();
	}

	/** 
	 * Test the mutators.
	 */
	public void testMutators() throws Exception {
		ThreadPool tp=new ThreadPool("testMutators");
		tp.setMinTotalThreads(5);
		try {
			tp.setMinTotalThreads(-5);
			fail("Allowed me to set min total threads size to -5");
		} catch(IllegalArgumentException e) {
			// pass
		}
		tp.setMaxTotalThreads(5);
		try {
			tp.setMaxTotalThreads(-5);
			fail("Allowed me to set min total threads size to -5");
		} catch(IllegalArgumentException e) {
			// pass
		}
		tp.setStartThreads(5);
		try {
			tp.setStartThreads(-5);
			fail("Allowed me to set start threads threads size to -5");
		} catch(IllegalArgumentException e) {
			// pass
		}
		tp.shutdown();
	}

	/** 
	 * Test the basics of the thread pool.
	 */
	public void testBasicThreadPool() throws Exception {
		ThreadPool tp=new ThreadPool("TestThreadPool", 15, 20,
				Thread.NORM_PRIORITY, new LinkedBlockingQueue<Runnable>(50));

		// Test string before start
		String.valueOf(tp);
		assertEquals("Incorrect thread count", 0, tp.getLargestPoolSize());

		// Starting the pool
		tp.start();

		// Make sure the priority is where we expect it.
		assertEquals(Thread.NORM_PRIORITY, tp.getPriority());

		// Make sure toString() doesn't error
		String.valueOf(tp);

		// Verify the start threads started.
		assertEquals("Start threads incorrect", 15, tp.getPoolSize());

		// Add some tasks
		for(int i=0; i<70; i++) {
			tp.addTask(new TestRunnable());
			// Give it a little time so the threads can ramp up.
			Thread.yield();
		}
		Thread.sleep(100);
		// Verify the thread count has risen to accommodate.
		assertEquals("Incorrect thread count", 20, tp.getLargestPoolSize());
		assertEquals("Incorrect thread count", 20, tp.getPoolSize());

		// Wait long enough for the jobs to all be accepted (at most, they
		// should be 5 seconds each, 20 at a time, for 70 tasks, giving us
		// seven seconds (I think))
		Thread.sleep(10000);
		// Verify they all got accepted
		assertTrue("Not all jobs accepted", tp.getQueue().size() == 0);

		// Add another one with a timeout (this will also trigger the run loop
		// which should notice there are too many threads).
		TestRunnable t=new TestRunnable();
		boolean started=tp.addTask(t, 500);
		assertFalse("New task wasn't accepted", started);
		// Give it a bit to start
		Thread.sleep(100);
		assertTrue("New task wasn't started", t.wasRun());

		// Wait for the monitor to notice and do something about it.
		Thread.sleep(1500);
		// Verify the thread count has risen to accommodate.
		assertTrue("Thread count not decreasing", tp.getPoolSize() < 20);

		// OK, now let's make sure the threads are busy
		for(int i=0; i<50; i++) {
			tp.addTask(new TestRunnable());
		}

		// Now that that's done, we want to add a thread with a timeout and
		// verify it *doesn't* make it.
		t=new TestRunnable();
		started=tp.addTask(t, 1000);
		assertTrue("New task shouldn't have been accepted", (!started));

		// Wait for the queue to die down to zero
		tp.waitForTaskCount(0);
		// Sleep just a second longer, so things can get initialized
		Thread.sleep(1000);
		// Now, verify the task did not start
		assertTrue("New task shouldn't have started", (!t.wasRun()));

		// Happier shutdown
		tp.shutdown();
	}

	public void testShutdown() throws Exception {
		ThreadPool tp=new ThreadPool("TestThreadPool");
		tp.addTask(new TestRunnable());

		// make sure we can't wait for the threads before shutting down
		try {
			tp.waitForThreads();
		} catch(IllegalStateException e) {
			// pass
		}

		// Happier shutdown
		tp.waitForCompletion();
	}

	public void testOverflow() throws Exception {
		ThreadPool tp=new ThreadPool("test pool", 1, 10,
				Thread.NORM_PRIORITY, 2);
		try {
			// Add more than it can take.
			for(int i=0; i<10; i++) {
				tp.addTask(new TestRunnable());
			}
		} catch(RejectedExecutionException e) {
			assertNull(e.getMessage());
		} finally {
			tp.shutdown();
		}
	}

	private static class TestRunnable extends SpyObject implements Runnable {
		private int id=0;
		boolean wasRun=false;

		public TestRunnable() {
			super();
			synchronized(TestRunnable.class) {
				id=ids++;
			}
		}

		public boolean wasRun() {
			return(wasRun);
		}

		public void run() {
			// Make sure this wasn't run more than once.
			synchronized(this) {
				if(wasRun) {
					throw new IllegalStateException(
						"This task has already been run.");
				}
				wasRun=true;
			}
			try {
				Random rand=new Random();
				// Get things that sleep between 1 and 5 seconds.
				long l=1000 + Math.abs(rand.nextLong()%4000);
				getLogger().info(id + ":  sleeping for " + l);
				Thread.sleep(l);
				getLogger().info(id + ":  done!");
			} catch(InterruptedException e) {
				throw new RuntimeException("Interrupted");
			}
		}
	}

}
