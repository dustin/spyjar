// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>

package net.spy.cache;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract implementation of Cachable.
 */
public abstract class AbstractCachable
	extends AbstractCacheListener implements Cachable {

	private final Object key;
	private final Object value;
	private final long cacheTime;
	private final AtomicInteger accesses=new AtomicInteger(0);
	private final AtomicLong lastAccess=new AtomicLong(0);

	/**
	 * Get an instance of AbstractCachable.
	 */
	public AbstractCachable(Object k, Object v) {
		super();
		key=k;
		value=v;
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
		return(lastAccess.get());
	}

	/**
	 * Mark a new access.
	 *
	 * @param t the time at which the access occurred
	 */
	public void setAccessTime(long t) {

		long oldtime=lastAccess.get();
		if(t>oldtime) {
			lastAccess.compareAndSet(oldtime, t);
		}
		accesses.incrementAndGet();
	}

	/**
	 * Get the number of times this object has been accessed.
	 */
	public int getAccessCount() {
		return(accesses.intValue());
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
	@Override
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
	@Override
	public void uncachedEvent(Object k) {
		super.uncachedEvent(k);
		// If we're holding a CacheListener, send the message to it.
		CacheListener o=getContainedObjectAsListener();
		if(o!=null) {
			o.uncachedEvent(k);
		}
	}

}
