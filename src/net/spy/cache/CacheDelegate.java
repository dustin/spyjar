// Copyright (c) 2005  Dustin Sallings
// arch-tag: 97FDAE3E-81EE-11D9-81FE-000393CFE6B8

package net.spy.cache;

/**
 * The cache delegate is notified of any objects going in or out of the cache.
 */
public interface CacheDelegate {

	/** 
	 * Called whenever an object is added to the cache.
	 */
	void cachedObject(String key, Cachable value);

	/** 
	 * Called whenever an object is removed from the cache.
	 */
	void uncachedObject(String key, Cachable value);

}
