// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: ThreadPoolObserver.java,v 1.1 2003/04/11 00:57:05 dustin Exp $

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
	 * recycling.  It will be called by multiple threads simultaneously, so
	 * whatever it does, it must do thread safely.
	 * 
	 * @param r the job that finished
	 */
	public void jobComplete(Runnable r) {
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
