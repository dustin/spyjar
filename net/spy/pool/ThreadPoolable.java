// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: ThreadPoolable.java,v 1.2 2003/03/28 07:30:54 dustin Exp $

package net.spy.pool;

/**
 * PoolAble object for containing a Thread.
 */
public class ThreadPoolable extends PoolAble {

	/**
	 * Get an instance of ThreadPoolable.
	 */
	public ThreadPoolable(Object theObject, int poolHash) {
		super(theObject, poolHash);
	}

	/** 
	 * Get a thread poolable.
	 */
	public ThreadPoolable(Object theObject, long maxAge, int poolHash) {
		super(theObject, maxAge, poolHash);
	}

	/** 
	 * @see PoolAble
	 */
	public void discard() {
		// Close down the worker thread
		WorkerThread wt=(WorkerThread)intGetObject();
		if(wt!=null) {
			wt.requestStop();
		}
		super.discard();
	}

	/** 
	 * True if this thread is still alive.
	 */
	public boolean isAlive() {
		boolean rv=false;
		WorkerThread wt=(WorkerThread)intGetObject();
		if(wt!=null) {
			rv=wt.isAlive();
		}
		return(rv);
	}

}
