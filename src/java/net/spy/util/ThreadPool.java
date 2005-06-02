// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 8BA62098-1110-11D9-B9A3-000A957659CC

package net.spy.util;

import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;

import net.spy.SpyThread;

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
public class ThreadPool extends ThreadGroup {

	// The threads we're managing.
	private Collection<RunThread> threads=null;
	// The tasks for the threads to do.
	private LinkedList<Task> tasks=null;

	// This is what we monitor for things being checked out (otherwise we
	// can't tell the difference between adds and check outs).
	private ThreadPoolObserver monitor=null;

	// Private thread ID allocator for the inner class.
	private static int threadIds=0;

	// Set to true when shutdown is called.
	private boolean shutdown=false;

	// 16,384 should be enough for anybody.
	private static final int DEFAULT_LIST_LIMIT=16384;

	// Default number of threads for a thread pool
	private static final int DEFAULT_NUM_THREADS=5;

	// Maximum amount of time to wait for an object notification while waiting
	// for jobs to finish
	private static final int WAIT_TIMEOUT=5000;

	// StringBuffer default size for toString methods
	private static final int TOSTRING_BUFFER_SIZE=128;

	private boolean started=false;

	// The priority for all threads we create
	private int priority=Thread.NORM_PRIORITY;

	private Logger logger=null;

	private int minTotalThreads=0;
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
	public ThreadPool(String name, int n, int prio) {
		super(name);
		setDaemon(true);

		if(priority<Thread.MIN_PRIORITY || priority>Thread.MAX_PRIORITY) {
			throw new IllegalArgumentException(priority
				+ " is an invalid priority.");
		}
		setPriority(prio);

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
		this(name, DEFAULT_NUM_THREADS, Thread.NORM_PRIORITY);
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
			Constructor cons=getPoolMangerConstructor();
			Object args[]={this};
			poolManager=(ThreadPoolManager)cons.newInstance(args);
			poolManager.setThreadPool(this);
			getLogger().info("Starting the thread pool manager");
			poolManager.start();
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(
				"Problem starting ThreadPoolManager", e);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(
				"Problem starting ThreadPoolManager", e);
		} catch(InvocationTargetException e) {
			throw new RuntimeException(
				"Problem starting ThreadPoolManager", e);
		} catch(InstantiationException e) {
			throw new RuntimeException(
				"Problem starting ThreadPoolManager", e);
		}

		// Mark it as started.
		started=true;
	}

	private Constructor getPoolMangerConstructor()
		throws NoSuchMethodException {

		Class args[]={ThreadGroup.class};
		Constructor cons=poolManagerClass.getConstructor(args);
		return(cons);
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
		if(shutdown) {
			getLogger().warn("Trying to create a thread after shutdown.");
		} else {
			RunThread rt=new RunThread(this, tasks, monitor);
			rt.setPriority(priority);
			threads.add(rt);
		}
	}

	/** 
	 * Shut down a thread.
	 */
	synchronized void destroyThread() {
		boolean shutOneDown=false;
		// Try to shut down something that doesn't appear to be running
		// anything (although it may start as we go)
		for(Iterator<RunThread> i=threads.iterator();
			!shutOneDown && i.hasNext();) {

			RunThread rt=i.next();
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
		for(RunThread rt : threads) {
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
		StringBuffer sb=new StringBuffer(TOSTRING_BUFFER_SIZE);
		sb.append(super.toString());
		if(tasks==null) {
			sb.append(" - no queue");
		} else {
			sb.append(" - ");
			synchronized(tasks) {
				sb.append(tasks.size());
			}
			sb.append(" of a maximum");
			sb.append(maxTaskQueueSize);
			sb.append(" tasks queued");
		}

		return(sb.toString());
	}

	/** 
	 * Set the PoolManager class.  This class will be instantiated when the
	 * ThreadPool is started.
	 * 
	 * @param pmc a subclass of ThreadPoolManager
	 */
	public void setPoolManagerClass(Class pmc) {
		if(ThreadPoolManager.class.isAssignableFrom(pmc)) {
			throw new IllegalArgumentException(
				"PoolManagerClass must be a subclass of "
				+ "ThreadPoolManager");
		}
		this.poolManagerClass=pmc;
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
	public void setMaxTaskQueueSize(int mtqs) {
		if(mtqs <= 0) {
			throw new IllegalArgumentException(
				"Value must be greater than or equal to zero.");
		}
		if(tasks != null && !(tasks instanceof LimitedList)) {
			throw new IllegalArgumentException("Tasks is not a limited list");
		}
		this.maxTaskQueueSize=mtqs;
		if(tasks != null) {
			LimitedList ll=(LimitedList)tasks;
			ll.setLimit(maxTaskQueueSize);
		}
	}

	/** 
	 * Get the minimum number of threads that may exist in the thread pool
	 * at any moment.
	 */
	public int getMinTotalThreads() {
		return(minTotalThreads);
	}

	/** 
	 * Set the minimum number of threads that may exist in the thread pool
	 * at any moment.
	 * 
	 * @param minTotalThreads a value &ge; 0
	 */
	public void setMinTotalThreads(int mtt) {
		if(mtt < 0) {
			throw new IllegalArgumentException(
				"Value must be greater than or equal to zero.");
		}
		this.minTotalThreads=mtt;
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
	public void setMinIdleThreads(int mit) {
		if(mit < 0) {
			throw new IllegalArgumentException(
				"Value must be greater than or equal to zero.");
		}
		this.minIdleThreads=mit;
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
	public void setMaxTotalThreads(int mtt) {
		if(mtt < 0) {
			throw new IllegalArgumentException(
				"Value must be greater than or equal to zero.");
		}
		this.maxTotalThreads=mtt;
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
	public void setStartThreads(int st) {
		if(st < 0) {
			throw new IllegalArgumentException(
				"Value must be greater than or equal to zero.");
		}
		this.startThreads=st;
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
	public void setPriority(int p) {
		this.priority=p;
	}

	/** 
	 * Set the LinkedList to contain the tasks on which this ThreadPool
	 * will be listening.
	 */
	public void setTasks(LinkedList<Task> t) {
		if(started) {
			throw new IllegalStateException("Can't set tasks after the "
				+ "pool has started.");
		}
		this.tasks=t;
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

	// Make sure the thing's started
	private synchronized void checkStarted() {
		if(!started) {
			start();
			started=true;
		}
	}

	// Common stuff for adding tasks
	private void addTask(Task t) {
		checkStarted();
		synchronized(tasks) {
			tasks.add(t);
			tasks.notify();
		}
		// Let pool manager know something's been added.
		synchronized(poolManager) {
			poolManager.notify();
		}
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
		addTask(new Task(r));
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
		boolean wasStarted=false;
		Task t=new Task(r);
		addTask(t);
		// Give it a bit of time to start, which might help us bypass this
		// whole mess
		Thread.yield();
		synchronized(t) {
			// If it hadn't started by the time we got here, wait for it
			wasStarted=t.isStarted();
			if(!wasStarted) {
				try {
					t.wait(timeout);
				} catch(InterruptedException e) {
					// We catch the interrupted exception here because
					// there's not an easy way to give the client the
					// ability to cancel this task, so it's just going to
					// go anyway.  It's probably safer to potentially
					// automatically cancel the task than it is to always
					// run the task when there was an InterruptedException
					getLogger().warn("addTask interrupted", e);
				}
			}
		}
		// Regain the lock and check again
		synchronized(t) {
			// If it's still not started after our wait, cancel it
			wasStarted=t.isStarted();
			if(!wasStarted) {
				getLogger().info(
					"Cancelling new task, wasn't completed in time");
				t.cancel();
			}
		}
		return(wasStarted);
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
	public synchronized int getActiveThreadCount() {
		int rv=0;
		for(RunThread t : threads) {
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
	public synchronized void shutdown() {
		getLogger().info("Shutting down this thread pool.");
		if(shutdown) {
			throw new IllegalStateException("Already shut down");
		}
		// First shut down the manager so it doesn't try to create any more
		// threads
		poolManager.requestStop();
		// Now, tell all of the known threads that we don't need them anymore
		for(RunThread t : threads) {
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
				monitor.wait(WAIT_TIMEOUT);
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
	// This object wraps the Runnable while it's queued, giving us the
	// ability to effectively dequeue something.
	// //////////////////////////////////////////////////////////////////////

	private static class Task extends Object {
		private Runnable runnable=null;
		private boolean started=false;

		public Task(Runnable r) {
			super();
			this.runnable=r;
		}

		/** 
		 * True if this task has been started.
		 */
		public boolean isStarted() {
			return(started);
		}

		/** 
		 * Cancel a task before it gets a chance to start.
		 *
		 * @exception IllegalStateException if it's already been canceled or
		 * started
		 */
		public void cancel() {
			if(runnable==null) {
				throw new IllegalStateException("Already cancelled");
			}
			if(started) {
				throw new IllegalStateException("Already started");
			}

			runnable=null;
		}

		/** 
		 * Get the Runnable this task is representing.
		 * 
		 * @return the Runnable
		 *
		 * @exception IllegalStateException if this task has already started
		 */
		public Runnable getTask() {
			if(started) {
				throw new IllegalStateException("Already started");
			}

			started=true;
			return(runnable);
		}

	}

	// //////////////////////////////////////////////////////////////////////
	// The threads that make up the pool.
	// //////////////////////////////////////////////////////////////////////

	private static class RunThread extends SpyThread {
		private ThreadPoolObserver monitor=null;
		private LinkedList<Task> tasks=null;
		private boolean going=true;
		private int threadId=0;

		private String runningMutex=null;
		private Runnable running=null;
		private long start=0;

		public RunThread(ThreadGroup tg, LinkedList<Task> tsks,
			ThreadPoolObserver mntr) {

			super(tg, "RunThread");

			runningMutex="runningMutex";
			this.tasks=tsks;
			this.monitor=mntr;

			threadId=threadIds++;

			// System.out.println("RunThread " + threadId + " going online.");

			// Adjust the name to include the thread number
			setName("RunThread#" + threadId);
			// Note:  This should not be a daemon thread.
			this.start();
		}

		public String toString() {
			StringBuffer sb=new StringBuffer(TOSTRING_BUFFER_SIZE);
			sb.append(super.toString());

			int size=0;
			synchronized(tasks) {
				size=tasks.size();
			}

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
				this.getLogger().error("Problem running your runnable", t);
			}
			synchronized(runningMutex) {
				running=null;
			}
		}

		public void run() {
			while(going) {
				try {
					Task tsk=null;
					synchronized(tasks) {
						tsk=(Task)tasks.removeFirst();
					}
					// Get the runnable from the task (in a specific lock)
					Runnable rn=null;
					synchronized(tsk) {
						rn=tsk.getTask();
						tsk.notify();
					}
					// Make sure we got something there.
					if(rn!=null) {
						run(rn);
						// Let the monitor know we finished it
						synchronized(monitor) {
							monitor.completedJob(rn);
						}
					}
				} catch(NoSuchElementException e) {
					// If the stack is empty, wait for something to get added.
					synchronized(tasks) {
						try {
							// Wait for an object to show up
							tasks.wait(WAIT_TIMEOUT);
						} catch(InterruptedException ie) {
							// That's OK, we'll try again.
							getLogger().debug(
								"Interrupted while waiting for task");
						}
					}
				} // empty stack
			} // while
			this.getLogger().debug("Shutting down.");
		} // ThreadPool$RunThread.run()
	} // ThreadPool$RunThread
} // ThreadPool
