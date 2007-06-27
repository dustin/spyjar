// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>

package net.spy.cache;

/**
 * Object that are self-maintaining in a cache.
 */
public interface Cachable extends CacheListener {

	/** 
	 * Get the key with which this object is cached.
	 */
	Object getCacheKey();

	/** 
	 * Get the value that has been cached.
	 */
	Object getCachedObject();

	/** 
	 * Determine whether this cache entry has expired.
	 * 
	 * @return true if the object has expired
	 */
	boolean isExpired();

	/** 
	 * Get the timestamp at which this object was cached.
	 */
	long getCacheTime();

	/** 
	 * Get the timestamp of the last access.
	 */
	long getLastAccessTime();

	/** 
	 * Set the timestamp of a particular access.
	 *
	 * Implementations are expected to increment their access count and
	 * determine whether this particular access time is mroe recent than
	 * their current latest access time (which may be the case if the
	 * notifications are asynchronous).
	 *
	 * @param t a timestamp indicating access
	 */
	void setAccessTime(long t);

	/** 
	 * Get the number of times this object has been accessed.
	 */
	int getAccessCount();

}
