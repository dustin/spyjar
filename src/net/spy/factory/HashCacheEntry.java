// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 702B8A67-5EE9-11D9-B99F-000A957659CC

package net.spy.factory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import net.spy.SpyObject;

/**
 * An implementation of CacheEntry that is backed by a HashMap.
 */
public class HashCacheEntry extends SpyObject implements CacheEntry {

	private Map cache=null;

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
	public void cacheInstance(Instance i) {
		cache.put(new Integer(i.getId()), i);
	}

	/** 
	 * Get the object at this ID.
	 * @param id the object ID
	 * @return the object, or null if there's no object by this ID
	 */
	public Object getById(int id) {
		return(cache.get(new Integer(id)));
	}

	/** 
	 * Get all of the objects in this cache.
	 * 
	 * @return an unmodifiable collection of object instances
	 */
	public Collection getAllObjects() {
		return(Collections.unmodifiableCollection(cache.values()));
	}

}
