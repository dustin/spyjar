// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: ThreadPoolTest.java,v 1.1 2003/04/11 09:05:07 dustin Exp $

package net.spy.test;

import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.spy.SpyObject;
import net.spy.util.ThreadPool;

/**
 * Test the ThreadPool.
 */
public class ThreadPoolTest extends TestCase {
	// Thread IDs (inner classes can't have static fields)
	private static int ids=0;

	/**
	 * Get an instance of ThreadPoolTest.
	 */
	public ThreadPoolTest(String name) {
		super(name);
	}

	/** 
	 * Get the suite.
	 */
	public static Test suite() {
		return new TestSuite(ThreadPoolTest.class);
	}

	/** 
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	/** 
	 * Test the basics of the thread pool.
	 */
	public void testBasicThreadPool() throws Exception {
		ThreadPool tp=new ThreadPool("TestThreadPool");
		tp.setStartThreads(5);
		tp.setMaxTotalThreads(20);
		tp.setMinIdleThreads(5);
		tp.start();

		// Verify the start threads started.
		assertEquals("Start threads incorrect", 5, tp.getThreadCount());

		// Add some tasks
		for(int i=0; i<30; i++) {
			tp.addTask(new TestRunnable());
			Thread.sleep(100);
		}
		// Verify the thread count has risen to accommodate.
		assertEquals("Incorrect thread count", 20, tp.getThreadCount());
		// Wait for jobs to all be accepted
		Thread.sleep(10000);
		assertTrue("Not all jobs finished", tp.getTaskCount() == 0);
		// Add another one (this will trigger the run loop which should
		// notice there are too many threads).
		tp.addTask(new TestRunnable());
		// Wait for the monitor to notice and do something about it.
		Thread.sleep(1500);
		// Verify the thread count has risen to accommodate.
		assertTrue("Thread count not decreasing", tp.getThreadCount() < 20);
		Thread.sleep(65000);
		tp.shutdown();
	}

	private class TestRunnable extends SpyObject implements Runnable {
		private int id=0;

		public TestRunnable() {
			super();
			synchronized(TestRunnable.class) {
				id=ids++;
			}
		}

		public void run() {
			try {
				Random rand=new Random();
				long l=Math.abs(rand.nextLong()%5000);
				getLogger().info(id + ":  sleeping for " + l);
				Thread.sleep(l);
				getLogger().info(id + ":  done!");
			} catch(InterruptedException e) {
				throw new RuntimeException("Interrupted");
			}
		}
	}

}
