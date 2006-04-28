// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 8BA62098-1110-11D9-B9A3-000A957659CC

package net.spy.concurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;

/**
 * A producer/consumer thread pool for easy parallelism.
 *
 * <p>
 * Quick start example (assuming you want to do a million tasks, 15 at time):
 *
 * <pre>
 * // Get a thread pool that will perform 15 tasks at a time
 * ThreadPool tp=new ThreadPool("Test Pool", 15);
 * // Start the thread pool
 * tp.start();
 *
 * // Do the tasks in a loop, throttling to make sure all we don't
 * // create too many objects that aren't ready to be used.
 * for(int i=0; i&lt;1000000; i++) {
 *     // Don't have more than 32 unclaimed tasks
 *     tp.waitForTaskCount(32);
 *     tp.addTask(new MyRunnableClass());
 * }
 * tp.waitForCompletion();
 * 
 * </pre>
 *
 * </p>
 *
 * <p>
 * The ThreadPoolManager instance is responsible for sizing and resizing
 * the pool.  On start(), the ThreadPoolManager will size the pool to the
 * start size configured in the ThreadPool.  The manager will receive a
 * notification after any task is added to the pool and may choose to take
 * action at that point.  Otherwise, it will sleep for a certain amount of
 * time (one minute by default) and then begin its main loop verifying the
 * number of idle threads satisfies the configuration.
 * </p>
 * 
 * <p>
 * If the number of idle threads is too low, it will create as many as is
 * required to have the appropriate number of idle threads as long as the
 * total number of threads will not exceed the configured maximum.
 * </p>
 *
 * <p>
 *  If the number of idle threads is too high, it will reduce the total
 *  number of threads by one (for each loop).
 * </p>
 *
 */
public class ThreadPool extends ThreadPoolExecutor {

	// This is what we monitor for things being checked out (otherwise we
	// can't tell the difference between adds and check outs).
	private ThreadPoolObserver monitor=null;

	// 8,192 should be enough for anybody.
	private static final int DEFAULT_LIST_LIMIT=8192;

	// Default number of threads for a thread pool
	private static final int DEFAULT_NUM_THREADS=5;

	// Maximum amount of time to wait for an object notification while waiting
	// for jobs to finish
	private static final int WAIT_TIMEOUT=5000;

	private transient Logger logger=null;

	/**
	 * Get an instance of ThreadPool.
	 * 
	 * @param name the name of this pool
	 * @param n the core size of this pool
	 * @param max the maximum size of this pool
	 * @param prio the priority of threads created within this pool
	 * @param q the work queue
	 */
	public ThreadPool(final String name, int n, int max, int prio,
			BlockingQueue<Runnable> q) {
		super(n, max, 1, TimeUnit.SECONDS,
				q, new MyThreadFactory(name, prio));

		setPriority(prio);
		monitor=new ThreadPoolObserver();
	}

	/**
	 * Get an instance of ThreadPool.
	 *
	 * @param name Name of the pool.
	 * @param n number of threads
	 * @param max the maximum number of threads
	 * @param prio Priority of the child threads.
	 * @param size the queue size (as an ArrayBlockingQueue)
	 */
	public ThreadPool(final String name, int n, int max, int prio, int size) {
		this(name, n, max, prio, new ArrayBlockingQueue<Runnable>(size, true));
	}

	/**
	 * Get an instance of ThreadPool.
	 * 
	 * @param name name of the pool
	 * @param n core pool size
	 * @param max max pool size
	 * @param prio priority of threads created within this pool
	 */
	public ThreadPool(String name, int n, int max, int prio) {
		this(name, n, max, prio, DEFAULT_LIST_LIMIT);
	}

	/**
	 * Get an instance of ThreadPool.
	 * 
	 * @param name name of the pool
	 * @param n core pool size
	 * @param max max pool size
	 */
	public ThreadPool(String name, int n, int max) {
		this(name, n, max, Thread.NORM_PRIORITY);
	}

	/**
	 * Get an instance of ThreadPool.
	 *
	 * @param name Name of the pool.
	 * @param n Number of threads.
	 */
	public ThreadPool(String name, int n) {
		this(name, n, n);
	}

	/**
	 * Get an instance of ThreadPool with five threads and a normal priority.
	 *
	 * @param name Name of the pool.
	 */
	public ThreadPool(String name) {
		this(name, DEFAULT_NUM_THREADS, Thread.NORM_PRIORITY);
	}

	/** 
	 * Start the ThreadPool.
	 */
	public synchronized void start() {
		int threads=prestartAllCoreThreads();
		getLogger().info("Started %d of %d threads", threads,
			getCorePoolSize());
	}

	private Logger getLogger() {
		if(logger == null) {
			logger=LoggerFactory.getLogger(getClass());
		}
		return(logger);
	}

	/** 
	 * Find out how many threads are idle.
	 *
	 * This is a snapshot, it is subject to change between the time that
	 * the number is calculated and the time that the value is returned.
	 * 
	 * @return the number of threads not currently running a task
	 */
	public int getIdleThreadCount() {
		return(getPoolSize() - getActiveCount());
	}

	/** 
	 * String me.
	 */
	public String toString() {
		return(super.toString() + " - " + getQueue().size()
				+ " of a maximum " + DEFAULT_LIST_LIMIT + " tasks queud");
	}

	/** 
	 * Get the minimum number of threads that may exist in the thread pool
	 * at any moment.
	 */
	public int getMinTotalThreads() {
		return(getCorePoolSize());
	}

	/** 
	 * Get the priority that will be used for any new threads within this
	 * thread group.
	 */
	public int getPriority() {
		MyThreadFactory t=(MyThreadFactory)getThreadFactory();
		return(t.priority);
	}

	/** 
	 * Set the priority to be used for any new threads within this threaad
	 * group.
	 */
	public void setPriority(int p) {
		MyThreadFactory t=(MyThreadFactory)getThreadFactory();
		t.setPriority(p);
	}

	/** 
	 * Get the monitor that receives notifications when a worker thread
	 * finishes a job.
	 */
	public ThreadPoolObserver getMonitor() {
		return(monitor);
	}

	/** 
	 * Set the observer who will receive notification whenever a task is
	 * completed.
	 */
	public void setMonitor(ThreadPoolObserver m) {
		this.monitor=m;
	}

	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		monitor.completedJob(r);
	}

	/**
	 * Add a task for one of the threads to execute.
	 *
	 * This method will add a new task to the queue and notify the queue of
	 * the new task (as well as notify the pool manager) then return
	 * immediately.
	 *
	 * @see ThreadPoolRunnable
	 * @exception IndexOutOfBoundsException if the backing list is full
	 */
	public void addTask(Runnable r) {
		execute(r);
	}

	/** 
	 * Add a task for one of the threads to execute.
	 *
	 * This method will add a new task to the queue, but only wait a
	 * certain amount of time for the task to get picked up.  If the task
	 * is not picked up for execution by <i>timeout</i> milliseconds, the
	 * task will not be executed.
	 * 
	 * @param r the task to execute
	 * @param timeout the number of milliseconds to wait for it to start
	 *
	 * @return true if the task was started
	 *
	 * @exception IndexOutOfBoundsException if the backing list is full
	 */
	public boolean addTask(Runnable r, long timeout) {
		boolean wasDone=false;
		Future<?> f=submit(r, true);
		try {
			f.get(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			getLogger().debug("Interrupted while waiting for task", e);
			f.cancel(true);
		} catch (ExecutionException e) {
			getLogger().debug("Task execution threw an exception", e);
			wasDone=true;
		} catch (TimeoutException e) {
			getLogger().debug("Timed out while waiting for execution", e);
			f.cancel(true);
		}
		return wasDone;
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
		awaitTermination(86400, TimeUnit.SECONDS);
	}

	/**
	 * Wait until there are no more than <i>num</i> tasks in the queue.
	 * This is good for throttling task additions.
	 *
	 * @param num the number of tasks for which to wait
	 * @throws InterruptedException if wait fails
	 */
	public void waitForTaskCount(int num) throws InterruptedException {
		while(getQueue().size() > num) {
			synchronized(monitor) {
				monitor.wait(WAIT_TIMEOUT);
			}
		}
	}

	private final static class MyThreadFactory implements ThreadFactory {
		private String name=null;
		private int priority=Thread.NORM_PRIORITY;

		private MyThreadFactory(String nm, int prio) {
			super();
			name=nm;
			setPriority(prio);
		}

		public void setPriority(int to) {
			if(to<Thread.MIN_PRIORITY || to>Thread.MAX_PRIORITY) {
				throw new IllegalArgumentException(to
						+ " is an invalid priority.");
			}
		}

		public Thread newThread(Runnable r) {
			Thread t=new Thread(r, name + " worker");
			t.setPriority(priority);
			return t;
		}
	}

} // ThreadPool
