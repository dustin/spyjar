// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: ThreadPoolManager.java,v 1.1 2003/04/11 00:57:05 dustin Exp $

package net.spy.util;

import java.util.List;

import net.spy.SpyThread;

/**
 * Management thread for managing a ThreadPool.
 *
 * This thread basically gets to hear about everything that's going on and
 * can make the decision to spawn more threads, or kill some off if
 * necessary.
 */
public class ThreadPoolManager extends SpyThread {

	private ThreadPool tp=null;

	/**
	 * Get an instance of ThreadPoolManager.
	 */
	public ThreadPoolManager() {
		super();
		setDaemon(true);
		setName("ThreadPoolManager");
	}

	/** 
	 * Get the ThreadPool we're watching.
	 */
	protected void getThreadPool() {
		return(tp);
	}

	/** 
	 * Set the ThreadPool to watch.
	 */
	public final void setThreadPool(ThreadPool t) {
		tp=t;
	}

}
