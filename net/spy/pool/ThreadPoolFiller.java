// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: ThreadPoolFiller.java,v 1.2 2003/03/28 08:00:29 dustin Exp $

package net.spy.pool;

import java.util.Map;
import java.util.HashMap;
import java.lang.ref.WeakReference;

import net.spy.SpyConfig;

/**
 * Filler for filling thread pools.
 */
public class ThreadPoolFiller extends PoolFiller {

	private int id=0;
	private static Map groups=null;

	/**
	 * Get an instance of ThreadPoolFiller.
	 */
	public ThreadPoolFiller(String name, SpyConfig conf) {
		super(name, conf);
	}

	private synchronized ThreadGroup getThreadGroup() {
		if(groups==null) {
			groups=new HashMap();
		}
		// Get the reference
		WeakReference wr=(WeakReference)groups.get(getName());
		ThreadGroup rv=null;
		// If the reference worked, try to get the object from the reference
		if(wr!=null) {
			rv=(ThreadGroup)wr.get();
		}
		// If we didn't get an object, make one.
		if(rv==null) {
			rv=new ThreadGroup("ThreadPool - " + getName());
			groups.put(getName(), new WeakReference(rv));
		}
		return(rv);
	}

	/** 
	 * Get a new thread poolable.
	 */
	public PoolAble getObject() throws PoolException {
		ThreadPoolable rv=null;
		// get the thread itself
		WorkerThread o=new WorkerThread(getThreadGroup(), "Worker#" + id++);
		// Get the poolable
		rv=new ThreadPoolable(o,
			(long)getPropertyInt("max_age", 0), getPoolHash());
		// Return it
		return(rv);
	}

}
