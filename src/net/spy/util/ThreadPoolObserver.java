// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: ThreadPoolObserver.java,v 1.4 2003/09/11 03:53:36 dustin Exp $

package net.spy.util;

import net.spy.SpyObject;

/**
 * Receive notification of job completion.
 */
public class ThreadPoolObserver extends SpyObject {

	/** 
	 * Get an instance of ThreadPoolObserver.
	 */
	public ThreadPoolObserver() {
		super();
	}

	/** 
	 * This method will be called to indicate the completion of a job.
	 *
	 * <p>
	 *
	 * After jobComplete(Runnable) is called, a notifyAll will be sent
	 * letting anyone watching this thing know that something has occurred.
	 * jobComplete should be quick as to not get in the way of threads
	 * recycling.  It will be called inside the ThreadPool's worker thread,
	 * but it is a synchronized call, so if it doesn't return quickly, it
	 * will prevent multiple threads from recycling.
	 *
	 * </p>
	 * 
	 * @param r the job that finished
	 */
	protected void jobComplete(Runnable r) {
		// Nothing.
	}

	/** 
	 * Send notification of the completion of a job.
	 *
	 * This is called by the worker thread to announce the completion of
	 * the provided job.
	 * 
	 * @param r the job that finished
	 */
	final void completedJob(Runnable r) {
		jobComplete(r);
		synchronized(this) {
			notifyAll();
		}
	}

}
