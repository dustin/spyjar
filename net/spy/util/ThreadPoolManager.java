// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: ThreadPoolManager.java,v 1.5 2003/04/18 21:23:13 dustin Exp $

package net.spy.util;

import java.util.List;

/**
 * Management thread for managing a ThreadPool.
 *
 * This thread basically gets to hear about everything that's going on and
 * can make the decision to spawn more threads, or kill some off if
 * necessary.
 */
public class ThreadPoolManager extends LoopingThread {

	private ThreadPool tp=null;

	/**
	 * Get an instance of ThreadPoolManager.
	 * @param tg the thread group in which the pool manager will exist
	 */
	public ThreadPoolManager(ThreadGroup tg) {
		super(tg, "ThreadPoolManager");
		setDaemon(true);
		setMsPerLoop(60000);
	}

	/** 
	 * Get the ThreadPool we're watching.
	 */
	protected ThreadPool getThreadPool() {
		return(tp);
	}

	/** 
	 * Set the ThreadPool to watch.
	 */
	public final void setThreadPool(ThreadPool t) {
		tp=t;
	}

	/** 
	 * Initialize this ThreadPoolManager.
	 */
	public void start() {
		if(tp==null) {
			throw new IllegalStateException("There's no ThreadPool set");
		}
		int startThreads=tp.getStartThreads();
		getLogger().info("Initializing " + startThreads + " threads.");
		for(int i=0; i<startThreads; i++) {
			tp.createThread();
		}
		// Dah dah dah dah...super start!
		super.start();
	}

	/** 
	 * Check to see if there are too few threads to handle the current work
	 * load.  If the number of idle threads is below the minIdleThreads
	 * from the ThreadPool, but not greater than maxTotalThreads, we can
	 * spin some more threads up.
	 */
	protected void checkTooFewThreads() {
		int idleThreads=tp.getIdleThreadCount();
		int minIdle=tp.getMinIdleThreads();
		int totalThreads=tp.getActiveThreadCount();
		int maxTotal=tp.getMaxTotalThreads();

		// Figure out how many threads we need.
		int need=0;
		// Don't bother unless there are fewer idle threads than we want
		if(idleThreads < minIdle) {
			need=(minIdle - idleThreads);
		}
		// Add another one if there are any tasks queued
		if(tp.getTaskCount() > 0) {
			need++;
		}
		// If that would give us more than we are supposed to have, just
		// get as many as we're supposed to have.
		if(totalThreads + need > maxTotal) {
			need = maxTotal - totalThreads;
		}
		if(need>0) {
			getLogger().info("Spinning up " + need + " more threads to get "
				+ "us to " + (totalThreads + need));
			getLogger().info("There are " + tp.getTaskCount()
				+ " tasks queued.");
			for(int i=0; i<need; i++) {
				tp.createThread();
			}
		}
	}

	/** 
	 * Check to see if there are too many idle threads.  If the number of
	 * idle threads is above minIdleThreads, we can request to kill some
	 * off here.
	 *
	 * <p>
	 * The shutdown should be slow, no more than one thread per loop.
	 * </p>
	 */
	protected void checkTooManyThreads() {
		int idleThreads=tp.getIdleThreadCount();
		int minThreads=tp.getMinTotalThreads();
		int minIdle=tp.getMinIdleThreads();
		int totalThreads=tp.getActiveThreadCount();

		// Figure out if we can kill any off.
		if( (idleThreads > minIdle) && (totalThreads > minThreads)) {
			getLogger().info("Shutting down a thread (bring us down to "
				+ (totalThreads - 1) + ")");
			tp.destroyThread();
		}
	}

	/** 
	 * Check to see if we should start up or shut down any threads.
	 */
	protected void runLoop() {
		// Wait a second before doing checks (we receive a notify whenever
		// a task is added so we can deal with it), but we don't want to go
		// crazy processingt these things, so let's try to sleep
		try {
			sleep(1000);
		} catch(InterruptedException e) {
			getLogger().warn("Interrutped", e);
		}
		checkTooFewThreads();
		checkTooManyThreads();
	}

}
