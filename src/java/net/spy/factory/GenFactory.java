// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 8618ACE5-5EE7-11D9-80B3-000A957659CC

package net.spy.factory;

import java.util.Collection;

import net.spy.SpyObject;
import net.spy.cache.SpyCache;

/**
 * Generic object instance cache.
 */
public abstract class GenFactory<T extends Instance> extends SpyObject {

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
		if(time < 1) {
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
	protected CacheEntry<T> getCache() {
		CacheEntry<T> rv=null;
		SpyCache sc=SpyCache.getInstance();
		rv=(CacheEntry)sc.get(cacheKey);
		if(rv == null) {
			rv=setCache();
		}
		return(rv);
	}

	// Set the cache
	private CacheEntry<T> setCache() {
		CacheEntry<T> rv=getNewCacheEntry();
		for(T inst : getInstances()) {
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
	protected CacheEntry<T> getNewCacheEntry() {
		return new HashCacheEntry<T>();
	}

	/** 
	 * Get the collection of all Instance objects to be cached.
	 */
	protected abstract Collection<T> getInstances();

	/** 
	 * This method is called whenever getObject would return null.  The result
	 * of this object will be used instead.  Alternatively, one may throw a
	 * RuntimeException indicating a failure.
	 * 
	 * @param id the ID of the object that was requested.
	 * @return null
	 */
	protected T handleNullLookup(int id) {
		return(null);
	}

	/** 
	 * Get an object by ID.
	 * 
	 * @param id the object ID
	 * @return the object instance, or null if there's no such object
	 */
	public T getObject(int id) {
		CacheEntry<T> ce=getCache();
		T rv=ce.getById(id);
		if(rv == null) {
			rv=handleNullLookup(id);
		}
		return(rv);
	}

	/** 
	 * Get all objects cached by this factory.
	 */
	public Collection<T> getObjects() {
		CacheEntry<T> ce=getCache();
		return(ce.getAllObjects());
	}

	/** 
	 * Reset the cache for this factory.
	 */
	public void recache() {
		setCache();
	}

}
