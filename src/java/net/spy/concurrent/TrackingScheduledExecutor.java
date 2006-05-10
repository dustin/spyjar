// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: EDB0FF7D-6598-4187-A005-4C02F46221E6

package net.spy.concurrent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * ScheduledExecutorServer that uses WorkerThreads to track what's beging
 * executed.
 */
public class TrackingScheduledExecutor extends ScheduledThreadPoolExecutor {

	// Stolen from ThreadPool
	private Map<Runnable, WorkerThread> currentWorkers=
		new ConcurrentHashMap<Runnable, WorkerThread>();

	/**
	 * Get a TrackingScheduledExecutor.
	 * 
	 * @param maxThreads core pool size
	 * @param reject the rejected execution handler
	 * @param tg the thread group in which the threads should be created
	 * @param name the name of of the threads
	 */
	public TrackingScheduledExecutor(int maxThreads,
			RejectedExecutionHandler reject,
			final ThreadGroup tg, final String name) {
		super(maxThreads, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread rv=new WorkerThread(tg, r, name);
				return rv;
			}
		}, reject);
	}

	/**
	 * Get a TrackingScheduledExecutor.
	 * 
	 * @param maxThreads core pool size
	 * @param tg the thread group in which the threads should be created
	 * @param name the name of of the threads
	 */
	public TrackingScheduledExecutor(int maxThreads,
			final ThreadGroup tg, final String name) {
		super(maxThreads, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread rv=new WorkerThread(tg, r, name);
				return rv;
			}
		});
	}

	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		assert t instanceof WorkerThread : "Thread is not a WorkerThread";
		WorkerThread wt=(WorkerThread)t;
		wt.setRunning(r);
		currentWorkers.put(r, wt);
	}

	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		WorkerThread wt=currentWorkers.get(r);
		assert wt != null : "Lost worker for " + r;
		wt.setRunning(null);
		currentWorkers.remove(r);
	}
}
