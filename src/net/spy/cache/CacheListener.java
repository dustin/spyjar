// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 60385016-1110-11D9-BD4C-000A957659CC

package net.spy.cache;

/**
 * Objects implementing this interface will be aware of when they're added
 * to, or removed from a cache.
 */
public interface CacheListener {

	/** 
	 * Called whenever an object is added to a cache.
	 *
	 * @param key the key with which the object was cached
	 */
	void cachedEvent(Object key);

	/** 
	 * Called whenever an object is removed from a cache.
	 *
	 * @param key the key with which the object was cached
	 */
	void uncachedEvent(Object key);

}
