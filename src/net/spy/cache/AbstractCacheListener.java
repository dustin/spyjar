// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 5F233E04-1110-11D9-9CDE-000A957659CC

package net.spy.cache;

import net.spy.SpyObject;
import net.spy.log.Logger;

/**
 * Abstract implementation of CacheListener
 */
public class AbstractCacheListener extends SpyObject implements CacheListener {

	/**
	 * Get an instance of AbstractCacheListener.
	 */
	public AbstractCacheListener() {
		super();
	}

	/** 
	 * Receive notification of having been cached.
	 */
	public void cachedEvent(Object k) {
		Logger l=getLogger();
		if(l.isDebugEnabled()) {
			l.debug("Instance of " + getClass().getName()
				+ " cached with key:  " + k);
		}
	}

	/** 
	 * Receive notification of having been uncached.
	 */
	public void uncachedEvent(Object k) {
		Logger l=getLogger();
		if(l.isDebugEnabled()) {
			l.debug("Instance of " + getClass().getName()
				+ " uncached with key:  " + k);
		}
	}

}
