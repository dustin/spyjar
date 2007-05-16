// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 8618ACE5-5EE7-11D9-80B3-000A957659CC

package net.spy.factory;

import java.util.Collection;
import java.util.TimerTask;

import net.spy.SpyObject;
import net.spy.cache.SimpleCache;

/**
 * Base implementation of a generic object instance cache for objects
 * collections suitable of being stored completely in memory.
 */
public abstract class GenFactoryImpl<T> extends SpyObject
	implements GenFactory<T> {

	private final String cacheKey;
	private final long cacheTime;
	private long lastRefresh=0;
	private TimerTask nextRefresh=null;

	/**
	 * Get an instance of GenFactory.
	 * @param key the cache key to use
	 * @param time the refresh duration of the cache
	 */
	protected GenFactoryImpl(String key, long time) {
		super();
		if(key == null) {
			throw new NullPointerException("Cache key must not be null.");
		}
		if(time < 1) {
			throw new IllegalArgumentException("Invalid cache time:  " + time);
		}
		cacheKey=key;
		cacheTime=time;
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

	/**
	 * Get a new Storage over the given collection.
	 */
	protected Storage<T> getNewCacheEntry(Collection<T> c) {
		return new MemStorageImpl<T>(c);
	}

	// Set the cache
	private Storage<T> setCache() {
		Storage<T> rv=getNewCacheEntry(getInstances());
		lastRefresh=System.currentTimeMillis();
		SimpleCache sc=SimpleCache.getInstance();
		sc.store(cacheKey, rv, cacheTime);
		return(rv);
	}

	/**
	 * Store or update an individual object in the cache.
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

	/* (non-Javadoc)
	 * @see net.spy.factory.GenFactory#getObject(java.lang.String, java.lang.Object)
	 */
	public T getObject(String cacheName, Object key) {
		Storage<T> ce=getCache();
		T rv=ce.getObject(cacheName, key);
		if(rv == null) {
			rv=handleNullLookup(cacheName, key);
		}
		return(rv);
	}

	/* (non-Javadoc)
	 * @see net.spy.factory.GenFactory#getObject(int)
	 */
	public T getObject(int id) {
		return getObject("id", id);
	}

	/* (non-Javadoc)
	 * @see net.spy.factory.GenFactory#getObjects(java.lang.String, java.lang.Object)
	 */
	public Collection<T> getObjects(String cacheName, Object key) {
		Collection<T> rv=getCache().getObjects(cacheName, key);
		assert rv != null;
		return rv;
	}

	/* (non-Javadoc)
	 * @see net.spy.factory.GenFactory#getObjects()
	 */
	public Collection<T> getObjects() {
		Storage<T> ce=getCache();
		return(ce.getAllObjects());
	}

	/* (non-Javadoc)
	 * @see net.spy.factory.GenFactory#recache()
	 */
	public void recache() {
		setCache();
	}

	/* (non-Javadoc)
	 * @see net.spy.factory.GenFactory#getLastRefresh()
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
