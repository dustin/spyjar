// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// $Id: AbstractCacheListener.java,v 1.1 2002/12/08 06:21:25 dustin Exp $

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
