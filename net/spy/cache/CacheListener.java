// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// $Id: CacheListener.java,v 1.1 2002/12/08 06:21:25 dustin Exp $

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
