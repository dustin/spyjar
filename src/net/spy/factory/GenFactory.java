// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 8618ACE5-5EE7-11D9-80B3-000A957659CC

package net.spy.factory;

import java.util.Collection;
import java.util.Iterator;

import net.spy.SpyObject;
import net.spy.cache.SpyCache;

/**
 * Generic object instance cache.
 */
public abstract class GenFactory extends SpyObject {

	private String cacheKey=null;
	private long cacheTime=0;

	/**
	 * Get an instance of GenFactory.
	 * @param key the cache key to use
	 * @param time the refresh duration of the cache
	 */
	protected GenFactory(String key, long time) {
		super();
		if(key == null) {
			throw new NullPointerException("Cache key must not be null.");
		}
		if(time == 0) {
			throw new IllegalArgumentException("Invalid cache time:  " + time);
		}
		this.cacheKey=key;
		this.cacheTime=time;
	}

	/** 
	 * Get the cache for this factory.
	 *
	 * If the cache does not exist, getNewCacheEntry() will be called to get an
	 * uninitialized CacheEntry instance, and getInstances() will be called to
	 * get a collection of instances to populate the cache.
	 * 
	 * @return a CacheEntry
	 */
	protected CacheEntry getCache() {
		CacheEntry rv=null;
		SpyCache sc=SpyCache.getInstance();
		rv=(CacheEntry)sc.get(cacheKey);
		if(rv == null) {
			rv=setCache();
		}
		return(rv);
	}

	// Set the cache
	private CacheEntry setCache() {
		CacheEntry rv=getNewCacheEntry();
		Collection allEntries=getInstances();
		for(Iterator i=allEntries.iterator(); i.hasNext(); ) {
			Instance inst=(Instance)i.next();
			rv.cacheInstance(inst);
		}
		SpyCache sc=SpyCache.getInstance();
		sc.store(cacheKey, rv, cacheTime);
		return(rv);
	}

	/** 
	 * Get a CacheEntry instance to be populated with a collection of
	 * Instance objects.
	 *
	 * The default implentation returns an instance of HashCacheEntry.
	 * 
	 * @return an empty CacheEntry instance.
	 */
	protected CacheEntry getNewCacheEntry() {
		return new HashCacheEntry();
	}

	/** 
	 * Get the collection of all Instance objects to be cached.
	 */
	protected abstract Collection getInstances();

	/** 
	 * Get an object by ID.
	 * 
	 * @param id the object ID
	 * @return the object instance, or null if there's no such object
	 */
	public Object getObject(int id) {
		CacheEntry ce=getCache();
		return(ce.getById(id));
	}

	/** 
	 * Get all objects cached by this factory.
	 */
	public Collection getObjects() {
		CacheEntry ce=getCache();
		return(ce.getAllObjects());
	}

	/** 
	 * Reset the cache for this factory.
	 */
	public void recache() {
		setCache();
	}

}
