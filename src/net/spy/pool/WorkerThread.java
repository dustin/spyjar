// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 7F1BB678-1110-11D9-AE83-000A957659CC

package net.spy.pool;

import net.spy.util.LoopingThread;
import net.spy.util.RunnableRunner;

/**
 * Protected class used to run stuff in a thread pool.
 */
class WorkerThread extends LoopingThread implements RunnableRunner {

	private Runnable theRunnable=null;

	/** 
	 * Get the runnable.
	 */
	WorkerThread(ThreadGroup tg, String name) {
		super(tg, name);
		setDaemon(true);
		// Wait at most five minutes.
		setMsPerLoop(300000);
		start();
	}

	/** 
	 * String me.
	 */
	public String toString() {
		// Get a copy of this so if we lose the reference in anotther
		// thread, we'll still have our copy long enough to print it
		// out.
		Runnable tmp=theRunnable;

		StringBuffer sb=new StringBuffer(64);
		sb.append(super.toString());
		if(tmp != null) {
			sb.append(" - running ");
			sb.append(tmp);
		} else {
			sb.append(" - idle");
		}

		return(sb.toString());
	}

	/** 
	 * Implementation of RunnableRunner that gets the job done in the pool.
	 */
	public synchronized void run(Runnable r) {
		if(theRunnable != null) {
			throw new IllegalStateException(
				"Already has a valid runnable.");
		}
		theRunnable=r;
		// Notify so we'll wake up immediately.
		notify();
	}

	/** 
	 * Run the task.
	 */
	protected synchronized void runLoop() {
		if(theRunnable != null) {
			try {
				theRunnable.run();
			} catch(RuntimeException e) {
				getLogger().error("Problem processing job", e);
			} finally {
				theRunnable=null;
			}
		} // Has a runnable 
	} // runLoop()
}

