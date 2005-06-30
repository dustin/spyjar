// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 702B8A67-5EE9-11D9-B99F-000A957659CC

package net.spy.factory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.spy.SpyObject;

/**
 * An implementation of CacheEntry that is backed by a HashMap.
 */
public class HashCacheEntry<T extends Instance> extends SpyObject
	implements CacheEntry<T> {

	private Map<Integer, T> cache=null;

	/**
	 * Get an instance of HashCacheEntry.
	 */
	public HashCacheEntry() {
		super();
		cache=new HashMap();
	}

	/** 
	 * Cache this instance.
	 */
	public void cacheInstance(T i) {
		cache.put(i.getId(), i);
	}

	/** 
	 * Get the object at this ID.
	 * @param id the object ID
	 * @return the object, or null if there's no object by this ID
	 */
	public T getById(int id) {
		return(cache.get(id));
	}

	/** 
	 * Get all of the objects in this cache.
	 * 
	 * @return an unmodifiable collection of object instances
	 */
	public Collection<T> getAllObjects() {
		return(Collections.unmodifiableCollection(cache.values()));
	}

}
