// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// $Id: AbstractCachable.java,v 1.2 2003/08/05 09:01:03 dustin Exp $

package net.spy.cache;

import net.spy.SpyObject;

/**
 * Abstract implementation of Cachable.
 */
public abstract class AbstractCachable
	extends AbstractCacheListener implements Cachable {

	private Object key=null;
	private Object value=null;
	private long cacheTime=0;
	private int accesses=0;
	private long lastAccess=0;

	/**
	 * Get an instance of AbstractCachable.
	 */
	public AbstractCachable(Object k, Object v) {
		super();
		this.key=k;
		this.value=v;
		cacheTime=System.currentTimeMillis();
	}

	/** 
	 * Get the time at which this thing was cached.
	 */
	public long getCacheTime() {
		return(cacheTime);
	}

	/** 
	 * Get the cache key.
	 */
	public Object getCacheKey() {
		return(key);
	}

	/** 
	 * Get the object that was cached.
	 */
	public Object getCachedObject() {
		return(value);
	}

	/** 
	 * Get the timestamp of the last access of this object.
	 */
	public long getLastAccessTime() {
		return(lastAccess);
	}

	/** 
	 * Mark a new access.
	 * 
	 * @param t the time at which the access occurred
	 */
	public synchronized void setAccessTime(long t) {
		if(t>lastAccess) {
			lastAccess=t;
		}
		accesses++;
	}

	/** 
	 * Get the number of times this object has been accessed.
	 */
	public int getAccessCount() {
		return(accesses);
	}

	// get the contained object as a CacheListener if'n it is one
	private CacheListener getContainedObjectAsListener() {
		CacheListener rv=null;
		if(value instanceof CacheListener) {
			rv=(CacheListener)value;
		}
		return(rv);
	}

	/** 
	 * Override cachedEvent to also send the message to the cached object
	 * if it wants it.
	 */
	public void cachedEvent(Object k) {
		super.cachedEvent(k);
		// If we're holding a CacheListener, send the message to it.
		CacheListener o=getContainedObjectAsListener();
		if(o!=null) {
			o.cachedEvent(k);
		}
	}

	/** 
	 * Override uncachedEvent to also send the message to the cached object
	 * if it wants it.
	 * 
	 * @param k 
	 */
	public void uncachedEvent(Object k) {
		super.uncachedEvent(k);
		// If we're holding a CacheListener, send the message to it.
		CacheListener o=getContainedObjectAsListener();
		if(o!=null) { 
			o.uncachedEvent(k);
		}
	}

}
