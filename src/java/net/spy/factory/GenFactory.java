// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 8618ACE5-5EE7-11D9-80B3-000A957659CC

package net.spy.factory;

import java.util.Collection;
import java.util.TimerTask;

import net.spy.SpyObject;
import net.spy.cache.SimpleCache;

/**
 * Generic object instance cache for objects collections suitable of being
 * stored completely in memory.
 */
public abstract class GenFactory<T> extends SpyObject {

	private String cacheKey=null;
	private long cacheTime=0;
	private long lastRefresh=0;
	private TimerTask nextRefresh=null;

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
	protected Storage<T> getCache() {
		SimpleCache sc=SimpleCache.getInstance();
		@SuppressWarnings("unchecked")
		Storage<T> rv=(Storage)sc.get(cacheKey);
		if(rv == null) {
			rv=setCache();
		}
		return(rv);
	}

	// Set the cache
	private Storage<T> setCache() {
		Storage<T> rv=new Storage<T>(getInstances());
		lastRefresh=System.currentTimeMillis();
		SimpleCache sc=SimpleCache.getInstance();
		sc.store(cacheKey, rv, cacheTime);
		return(rv);
	}

	/**
	 * Store or update an object in the cache.
	 * 
	 * @param i the object to store
	 * @throws Exception if the cache cannot be updated with this instance
	 */
	protected void cacheInstance(T i) throws Exception {
		getCache().cacheInstance(i);
	}

	/** 
	 * Get the collection of all Instance objects to be cached.
	 */
	protected abstract Collection<T> getInstances();

	/** 
	 * This method is called whenever getObject(String,Object) would return
	 * null.  The result of this object will be used instead.  Alternatively,
	 * one may throw a RuntimeException indicating a failure.
	 * 
	 * @param cacheName the name of the cache that was accessed
	 * @param key the key under that cache that was accessed
	 * @return null
	 */
	protected T handleNullLookup(String cacheName, Object key) {
		return null;
	}

	/**
	 * Get an object from an alternate cache by cache name and key.
	 * 
	 * @param cacheName the name of the alt cache
	 * @param key the key under which to look
	 * @return the object instance, or null if there's no such object
	 */
	public T getObject(String cacheName, Object key) {
		Storage<T> ce=getCache();
		T rv=ce.getObject(cacheName, key);
		if(rv == null) {
			rv=handleNullLookup(cacheName, key);
		}
		return(rv);
	}

	/**
	 * Convenience method for getObject(String,Object) assuming an integer
	 * field uniquely cached as ``id.''
	 * 
	 * @param id the id value
	 * @return the object stored with this id
	 */
	public T getObject(int id) {
		return getObject("id", id);
	}

	/**
	 * Get all of the objects mapped with the given key under the given
	 * cache name.
	 * 
	 * @param cacheName the name of the cache
	 * @param key the key with that name
	 * @return the objects mapped to that key, or an empty string if none
	 */
	public Collection<T> getObjects(String cacheName, Object key) {
		Collection<T> rv=getCache().getObjects(cacheName, key);
		assert rv != null;
		return rv;
	}

	/** 
	 * Get all objects cached by this factory.
	 */
	public Collection<T> getObjects() {
		Storage<T> ce=getCache();
		return(ce.getAllObjects());
	}

	/** 
	 * Reset the cache for this factory.
	 */
	public void recache() {
		setCache();
	}

	/**
	 * Get the timestamp of the last time this was refreshed.
	 */
	public long getLastRefresh() {
		return lastRefresh;
	}

	/**
	 * Get the TimerTask scheduled to refresh this cache (if any).
	 */
	TimerTask getNextRefresh() {
		return nextRefresh;
	}

	/**
	 * Set the TimerTask for the update 
	 */
	void setNextRefresh(TimerTask next) {
		nextRefresh = next;
	}

}
