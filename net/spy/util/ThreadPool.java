// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: ThreadPool.java,v 1.9 2003/04/05 02:07:14 dustin Exp $

package net.spy.util;

import java.util.Iterator;
import java.util.Random;
import java.util.List;
import java.util.NoSuchElementException;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;

/**
 * A thread pool for easy parallelism.
 *
 * <p>
 * Example (assuming you want to do a million tasks, 15 at time):
 *
 * <pre>
 * // Get a thread pool that will perform 15 tasks at a time
 * ThreadPool tp=new ThreadPool("Test Pool", 15);
 *
 * // Do the tasks in a loop, throttling to make sure all we don't
 * // create too many objects that aren't ready to be used.
 * for(int i=0; i&lt;1000000; i++) {
 *     // Don't have more than 32 unclaimed tasks
 *     tp.waitForTaskCount(32);
 *     tp.addTask(new MyRunnableClass());
 *     }
 * }
 * tp.waitForCompletion();
 * 
 * </pre>
 *
 * </p>
 *
 */
public class ThreadPool extends ThreadGroup {

	// The threads we're managing.
	private List threads=null;
	// The tasks for the threads to do.
	private LimitedList tasks=null;

	// This is what we monitor for things being checked out (otherwise we
	// can't tell the difference between adds and check outs).
	private Object monitor=null;

	// Private thread ID allocator for the inner class.
	private static int threadIds=0;

	// Set to true when shutdown is called.
	private boolean shutdown=false;

	// 16,384 should be enough for anybody.
	private static final int DEFAULT_LIST_LIMIT=16384;

	/**
	 * Get an instance of ThreadPool.
	 *
	 * @param name Name of the pool.
	 * @param n Number of threads.
	 * @param prio Priority of the child threads.
	 */
	public ThreadPool(String name, int n, int priority) {
		super(name);
		setDaemon(true);

		if(priority<Thread.MIN_PRIORITY || priority>Thread.MAX_PRIORITY) {
			throw new IllegalArgumentException(priority
				+ " is an invalid priority.");
		}

		tasks=new LimitedList(DEFAULT_LIST_LIMIT);
		threads=new java.util.ArrayList(n);
		monitor=new Object();

		// Initialize all of the threads.
		for(int i=0; i<n; i++) {
			RunThread rt=new RunThread(this, tasks, monitor);
			rt.setPriority(priority);
			threads.add(rt);
		}
	}

	/**
	 * Get an instance of ThreadPool with a normal priority.
	 *
	 * @param name Name of the pool.
	 * @param n Number of threads.
	 */
	public ThreadPool(String name, int n) {
		this(name, n, Thread.NORM_PRIORITY);
	}

	/** 
	 * String me.
	 */
	public String toString() {
		StringBuffer sb=new StringBuffer(128);
		sb.append(super.toString());
		sb.append(" - ");
		sb.append(tasks.size());
		sb.append(" tasks queued");

		return(sb.toString());
	}

	/**
	 * Add a task for one of the threads to execute.
	 *
	 * @see ThreadPoolRunnable
	 */
	public void addTask(Runnable r) {
		synchronized(tasks) {
			tasks.add(r);
			tasks.notify();
		}
	}

	/**
	 * Find out how many tasks are in the queue.  This is the number of
	 * jobs that have <i>not</i> been accepted by threads, i.e. they
	 * haven't been started.
	 */
	public int getTaskCount() {
		int rv=0;
		synchronized(tasks) {
			rv=tasks.size();
		}
		return(rv);
	}

	/**
	 * Find out how many threads are in the pool.
	 */
	public int getThreadCount() {
		return(threads.size());
	}

	/**
	 * Find out how many threads are still active (not shut down) in the pool.
	 */
	public int getActiveThreadCount() {
		int rv=0;
		for(Iterator i=threads.iterator(); i.hasNext(); ) {
			RunThread t=(RunThread)i.next();
			if(t.isAlive()) {
				rv++;
			}
		}
		return(rv);
	}

	/**
	 * Tell all the threads to shut down after they finish their current
	 * tasks.
	 */
	public void shutdown() {
		for(Iterator i=threads.iterator(); i.hasNext(); ) {
			RunThread t=(RunThread)i.next();
			t.shutdown();
		}
		shutdown=true;
	}

	/** 
	 * Shut down all of the threads after all jobs are complete, and wait
	 * for all tasks to complete.
	 *
	 * <p>
	 *  This is a convenience method that calls waitforTaskCount(0),
	 *  followed by shutdown(), followed by waitForThreads().
	 * </p>
	 *
	 * @throws InterruptedException if waitForTaskCount or waitForThreads
	 *								throws an exception
	 */
	public void waitForCompletion() throws InterruptedException {
		waitForTaskCount(0);
		shutdown();
		waitForThreads();
	}

	/**
	 * Wait until there are no more than <i>num</i> tasks in the queue.
	 * This is good for throttling task additions.
	 *
	 * @param num the number of tasks for which to wait
	 * @throws InterruptedException if wait fails
	 */
	public void waitForTaskCount(int num) throws InterruptedException {
		while(getTaskCount() > num) {
			synchronized(monitor) {
				monitor.wait(5000);
			}
		}
	}

	/**
	 * Wait until there are no more threads processing.
	 *
	 * This will return when all threads have shut down.
	 *
	 * @throws IllegalStateException if shutdown() has not been called
	 * @throws InterruptedException if sleep() is interrupted
	 */
	public void waitForThreads() throws InterruptedException {
		if(!shutdown) {
			throw new IllegalStateException("Not shut down.");
		}
		while(getActiveThreadCount() > 0) {
			Thread.sleep(1);
		}
	}

	// A test task that takes a random amount of time.
	private static Runnable getTestRunnable() {
		return(new Runnable() {
			public void run() {
				try {
					Random rand=new Random();
					long l=Math.abs(rand.nextLong()%15000);
					System.err.println("Sleeping for " + l);
					Thread.sleep(l);
					System.err.println("Done!");
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			});
	}

	/**
	 * Shuts down in case you didn't.
	 */
	protected void finalize() throws Throwable {
		if(!shutdown) {
			Logger l=LoggerFactory.getLogger(getClass());
			l.error("********* Shutting down abandoned thread pool *********");
		}
		shutdown();
	}

	/**
	 * Testing and what not.
	 */
	public static void main(String args[]) throws Exception {
		ThreadPool tp=new ThreadPool("TestThreadPool", 15);

		// Toss a hunded tasks into the thing.
		for(int i=0; i<100; i++) {
			tp.addTask(getTestRunnable());
			Thread.currentThread().getThreadGroup().list();
		}

		// Add another 100 tasks, but only if the task count is below 50
		for(int i=0; i<100; i++) {
			tp.waitForTaskCount(50);
			System.out.println("Adding new task.");
			tp.addTask(getTestRunnable());
		}

		// Wait for all of the tasks to finish
		tp.waitForTaskCount(0);
		System.out.println("All tasks have been accepted, shutting down.");
		// I shut 'em down!
		tp.shutdown();
		tp.waitForThreads();
		System.out.println("All threads have been shut down.");

		System.out.println("Done.");
	}

	// //////////////////////////////////////////////////////////////////////
	// The threads that make up the pool.
	// //////////////////////////////////////////////////////////////////////

	private class RunThread extends Thread {
		private Object monitor=null;
		private LimitedList tasks=null;
		private boolean going=true;
		private int threadId=0;

		private String runningMutex=null;
		private Object running=null;
		private long start=0;

		public RunThread(ThreadGroup tg, LimitedList tasks, Object monitor) {
			super(tg, "RunThread");

			runningMutex=new String("runningMutex");
			this.tasks=tasks;
			this.monitor=monitor;

			threadId=threadIds++;

			// System.out.println("RunThread " + threadId + " going online.");

			// Adjust the name to include the thread number
			setName("RunThread#" + threadId);
			// Note:  This should not be a daemon thread.
			start();
		}

		public String toString() {
			StringBuffer sb=new StringBuffer(128);
			sb.append(super.toString());

			int size=tasks.size();

			synchronized(runningMutex) {
				if(running==null) {
					sb.append(" - idle");
				} else {
					sb.append(" - running ");
					// Figure out whether we should call toString() or
					// display the class type.
					if(running instanceof ThreadPoolRunnable) {
						sb.append(running.toString());
					} else {
						sb.append(running.getClass().getName());
					}
					sb.append(" for ");
					sb.append(System.currentTimeMillis() - start);
					sb.append("ms");
				}
			}
			return(sb.toString());
		}

		// I shut 'em down!
		public void shutdown() {
			going=false;
		}

		private void run(Runnable r) {
			try {
				// Record the runnable
				running=r;
				start=System.currentTimeMillis();
				// Run the runnable.
				r.run();
			} catch(Throwable t) {
				Logger l=LoggerFactory.getLogger(getClass());
				l.error("Problem running your runnable", t);
			}
			synchronized(runningMutex) {
				running=null;
			}
		}

		public void run() {
			while(going) {
				try {
					Runnable r=(Runnable)tasks.removeFirst();
					// Let the monitor know we got one.
					synchronized(monitor) {
						monitor.notify();
					}
					run(r);
				} catch(NoSuchElementException e) {
					// If the stack is empty, wait for something to get added.
					synchronized(tasks) {
						try {
							// Wait up to ten seconds
							tasks.wait(10000);
						} catch(InterruptedException ie) {
							// That's OK, we'll try again.
						}
					}
				} // empty stack
			} // while
		} // ThreadPool$RunThread.run()
	} // ThreadPool$RunThread
} // ThreadPool
