// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: ThreadPool.java,v 1.12 2003/04/11 09:13:28 dustin Exp $

package net.spy.util;

import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;

import net.spy.SpyThread;

/**
 * A thread pool for easy parallelism.
 *
 * <p>
 * Example (assuming you want to do a million tasks, 15 at time):
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
 */
public class ThreadPool extends ThreadGroup {

	// The threads we're managing.
	private Collection threads=null;
	// The tasks for the threads to do.
	private LinkedList tasks=null;

	// This is what we monitor for things being checked out (otherwise we
	// can't tell the difference between adds and check outs).
	private ThreadPoolObserver monitor=null;

	// Private thread ID allocator for the inner class.
	private static int threadIds=0;

	// Set to true when shutdown is called.
	private boolean shutdown=false;

	// 16,384 should be enough for anybody.
	private static final int DEFAULT_LIST_LIMIT=16384;

	private boolean started=false;

	// The priority for all threads we create
	private int priority=Thread.NORM_PRIORITY;

	private Logger logger=null;

	private int minIdleThreads=0;
	private int maxTotalThreads=0;
	private int startThreads=0;
	private int maxTaskQueueSize=DEFAULT_LIST_LIMIT;

	// Pool manager stuff.
	private Class poolManagerClass=ThreadPoolManager.class;
	private ThreadPoolManager poolManager=null;

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
		setPriority(priority);

		minIdleThreads=n;
		maxTotalThreads=n;
		startThreads=n;
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
	 * Get an instance of ThreadPool with five threads and a normal priority.
	 *
	 * @param name Name of the pool.
	 */
	public ThreadPool(String name) {
		this(name, 5, Thread.NORM_PRIORITY);
	}

	// Make sure the thread parameters are within reason
	private void checkValues() {
		if(minIdleThreads > maxTotalThreads) {
			throw new IllegalStateException(
				"minIdleThreads is greater than maxTotalThreads");
		}
		if(startThreads > maxTotalThreads) {
			throw new IllegalStateException(
				"startThreads is greater than maxTotalThreads");
		}
	}

	/** 
	 * Start the ThreadPool.
	 */
	public synchronized void start() {
		// make sure we haven't already started
		if(started) {
			throw new IllegalStateException("Already started");
		}
		// Make sure the values are reasonable
		checkValues();
		// Make sure there's a place to put the tasks
		if(tasks==null) {
			tasks=new LimitedList(getMaxTaskQueueSize());
		}
		// Make sure there's a place to put the threads
		if(threads==null) {
			threads=new java.util.ArrayList(getStartThreads());
		}
		// We'll also need an object monitor
		if(monitor==null) {
			monitor=new ThreadPoolObserver();
		}

		// Set up the manager
		try {
			poolManager=(ThreadPoolManager)poolManagerClass.newInstance();
			poolManager.setThreadPool(this);
			getLogger().info("Starting the thread pool manager");
			poolManager.start();
		} catch(IllegalAccessException e) {
			throw new NestedRuntimeException(
				"Problem starting ThreadPoolManager", e);
		} catch(InstantiationException e) {
			throw new NestedRuntimeException(
				"Problem starting ThreadPoolManager", e);
		}

		// Mark it as started.
		started=true;
	}

	private Logger getLogger() {
		if(logger == null) {
			logger=LoggerFactory.getLogger(getClass());
		}
		return(logger);
	}

	/** 
	 * Create a new worker thread in the pool.
	 */
	synchronized void createThread() {
		RunThread rt=new RunThread(this, tasks, monitor);
		rt.setPriority(priority);
		threads.add(rt);
	}

	/** 
	 * Shut down a thread.
	 */
	synchronized void destroyThread() {
		boolean shutOneDown=false;
		// Try to shut down something that doesn't appear to be running
		// anything (although it may start as we go)
		for(Iterator i=threads.iterator();
			shutOneDown == false && i.hasNext(); ) {

			RunThread rt=(RunThread)i.next();
			if(!rt.isRunning()) {
				rt.shutdown();
				shutOneDown=true;
				i.remove();
			}

			// If we haven't shut one down yet, and this is the last one,
			// shut it down.
			if((!shutOneDown) && (!i.hasNext())) {
				rt.shutdown();
				shutOneDown=true;
				i.remove();
			}
		} // loop through all threads
	}

	/** 
	 * Find out how many threads are idle.
	 *
	 * This is a snapshot, it is subject to change between the time that
	 * the number is calculated and the time that the value is returned.
	 * 
	 * @return the number of threads not currently running a task
	 */
	public synchronized int getIdleThreadCount() {
		int rv=0;
		for(Iterator i=threads.iterator(); i.hasNext(); ) {
			RunThread rt=(RunThread)i.next();
			if(!rt.isRunning()) {
				rv++;
			}
		} // Loop through all threads
		return(rv);
	}

	/** 
	 * Get the total number of threads in this pool.
	 * 
	 * @return the number of threads
	 */
	public synchronized int getThreadCount() {
		int rv=threads.size();
		return(rv);
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
	 * Set the PoolManager class.  This class will be instantiated when the
	 * ThreadPool is started.
	 * 
	 * @param poolManagerClass a subclass of ThreadPoolManager
	 */
	public void setPoolManagerClass(Class poolManagerClass) {
		if(ThreadPoolManager.class.isAssignableFrom(poolManagerClass)) {
			throw new IllegalArgumentException(
				"PoolManagerClass must be a subclass of "
				+ "ThreadPoolManager");
		}
		this.poolManagerClass=poolManagerClass;
	}

	/** 
	 * Get the maximum size of the task queue.
	 */
	public int getMaxTaskQueueSize() {
		return(maxTaskQueueSize);
	}

	/** 
	 * Set the maximum size of the job queue.  This is the number of
	 * unclaimed tasks that may be queued.  If more than this many tasks
	 * are queued, exceptions will be thrown.
	 * 
	 * @param maxTaskQueueSize a value &gt; 0
	 */
	public void setMaxTaskQueueSize(int maxTaskQueueSize) {
		if(maxTaskQueueSize <= 0) {
			throw new IllegalArgumentException(
				"Value must be greater than or equal to zero.");
		}
		this.maxTaskQueueSize=maxTaskQueueSize;
	}

	/** 
	 * Get the minimum number of idle threads.
	 */
	public int getMinIdleThreads() {
		return(minIdleThreads);
	}

	/** 
	 * Set the minimum number of idle threads to maintain.
	 * 
	 * @param minIdleThreads a value &ge; 0
	 */
	public void setMinIdleThreads(int minIdleThreads) {
		if(minIdleThreads < 0) {
			throw new IllegalArgumentException(
				"Value must be greater than or equal to zero.");
		}
		this.minIdleThreads=minIdleThreads;
	}

	/** 
	 * Get the maximum number of total threads this pool may have.
	 */
	public int getMaxTotalThreads() {
		return(maxTotalThreads);
	}

	/** 
	 * Set the maximum number of threads that may be in this pool.
	 * 
	 * @param maxTotalThreads a value &ge; 0
	 */
	public void setMaxTotalThreads(int maxTotalThreads) {
		if(maxTotalThreads < 0) {
			throw new IllegalArgumentException(
				"Value must be greater than or equal to zero.");
		}
		this.maxTotalThreads=maxTotalThreads;
	}

	/** 
	 * Get the number of threads to spin up when bringing up this
	 * ThreadPool.
	 */
	public int getStartThreads() {
		return(startThreads);
	}

	/** 
	 * Set the number of threads to start when bringing up this pool.
	 * 
	 * @param startThreads a value &ge; 0
	 */
	public void setStartThreads(int startThreads) {
		if(startThreads < 0) {
			throw new IllegalArgumentException(
				"Value must be greater than or equal to zero.");
		}
		this.startThreads=startThreads;
	}

	/** 
	 * Get the priority that will be used for any new threads within this
	 * thread group.
	 */
	public int getPriority() {
		return(priority);
	}

	/** 
	 * Set the priority to be used for any new threads within this threaad
	 * group.
	 */
	public void setPriority(int priority) {
		this.priority=priority;
	}

	/** 
	 * Set the LinkedList to contain the tasks on which this ThreadPool
	 * will be listening.
	 */
	public void setTasks(LinkedList tasks) {
		if(started) {
			throw new IllegalStateException("Can't set tasks after the "
				+ "pool has started.");
		}
		this.tasks=tasks;
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
	public void setMonitor(ThreadPoolObserver monitor) {
		this.monitor=monitor;
	}

	// Make sure the thing's started
	private synchronized void checkStarted() {
		if(!started) {
			start();
			started=true;
		}
	}

	/**
	 * Add a task for one of the threads to execute.
	 *
	 * @see ThreadPoolRunnable
	 */
	public void addTask(Runnable r) {
		checkStarted();
		synchronized(tasks) {
			tasks.add(r);
			tasks.notify();
		}
		// Let pool manager know something's been added.
		synchronized(poolManager) {
			poolManager.notify();
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
		getLogger().info("Shutting down this thread pool.");
		for(Iterator i=threads.iterator(); i.hasNext(); ) {
			RunThread t=(RunThread)i.next();
			t.shutdown();
		}
		poolManager.requestStop();
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

	/**
	 * Shuts down in case you didn't.
	 */
	protected void finalize() throws Throwable {
		if(!shutdown) {
			getLogger().error(
				"********* Shutting down abandoned thread pool *********");
		}
		shutdown();
	}

	// //////////////////////////////////////////////////////////////////////
	// The threads that make up the pool.
	// //////////////////////////////////////////////////////////////////////

	private class RunThread extends SpyThread {
		private ThreadPoolObserver monitor=null;
		private LinkedList tasks=null;
		private boolean going=true;
		private int threadId=0;

		private String runningMutex=null;
		private Runnable running=null;
		private long start=0;

		public RunThread(ThreadGroup tg, LinkedList tasks,
			ThreadPoolObserver monitor) {

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

		/** 
		 * Ask if this thing is currently running something.
		 *
		 * This offer subject to local taxes and race conditions.
		 * 
		 * @return true if this object is running a task
		 */
		public boolean isRunning() {
			return(running != null);
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
						monitor.completedJob(r);
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
			getLogger().debug("Shutting down.");
		} // ThreadPool$RunThread.run()
	} // ThreadPool$RunThread
} // ThreadPool
