// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.concurrent;

/**
 * Thread that will perform the work for thread pools.
 */
public class WorkerThread extends Thread {

	private volatile Runnable running=null;
	private volatile long started=0;

	/**
	 * Construct a named worker thread for the given runnable in the given
	 * ThreadGroup.
	 */
	public WorkerThread(ThreadGroup tg, Runnable r, String name) {
		super(tg, r, name);
	}

	/**
	 * Construct a named worker thread for the given runnable.
	 */
	public WorkerThread(Runnable r, String name) {
		this(null, r, name);
	}

	/**
	 * Set the runnable that is being executed, or null if the run is complete.
	 */
	public void setRunning(Runnable to) {
		running=to;
		if(to != null) {
			started=System.currentTimeMillis();
		}
	}

	@Override
	public String toString() {
		String rv=null;
		Runnable r=running;
		if(r != null) {
			long now=System.currentTimeMillis();
			String runString=r.getClass().getName();
			if(r instanceof ThreadPoolRunnable) {
				ThreadPoolRunnable tpr=(ThreadPoolRunnable)r;
				runString=tpr.toString();
			}
			rv=super.toString() + " - running " + runString + " for "
				+ (now - started) + "ms";
		} else {
			rv=super.toString() + " - idle";
		}
		return rv;
	}
}
