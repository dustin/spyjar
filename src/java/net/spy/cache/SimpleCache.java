// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 73E1AE49-65BA-4636-BFEB-1202E635F7F7

package net.spy.cache;

import java.lang.ref.Reference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.spy.SpyObject;

/**
 * A simple time-based cache.
 */
public class SimpleCache extends SpyObject {

	private static SimpleCache instance=null;

	private ConcurrentMap<String, Object> storage=null;
	private Timer timer=null;

	/**
	 * Get an instance of SimpleCache.
	 */
	protected SimpleCache() {
		super();
		storage=new ConcurrentHashMap<String, Object>();
		timer=new Timer("SimpleCacheTimer", true);
	}

	/**
	 * Get the singleton SimpleCache instance.
	 */
	public static synchronized SimpleCache getInstance() {
		if(instance == null) {
			instance=new SimpleCache();
		}
		return instance;
	}

	/**
	 * Set the singleton SimpleCache instance.
	 * This also cleanly shuts down the previous instance.
	 */
	public static synchronized void setInstance(SimpleCache to) {
		if(instance != null) {
			instance.timer.cancel();
		}
		instance=to;
	}

	/**
	 * Get an object from the cache.
	 * If the stored object is a reference, it'll be dereferenced before
	 * returned.
	 * 
	 * @param key the cache key
	 * @return the cached object
	 */
	public Object get(String key) {
		Object rv=storage.get(key);
		if(rv != null && rv instanceof Reference) {
			Reference<?> ref=(Reference)rv;
			rv=ref.get();
			if(rv == null) {
				storage.remove(key, ref);
			}
		}
		return rv;
	}

	/**
	 * Store an object in the cache.
	 * 
	 * @param key the cache key
	 * @param timeout how long until it's deleted
	 * @param value the value to cache
	 */
	public void store(String key, Object value, long timeout) {
		storage.put(key, value);
		if(timeout != Long.MAX_VALUE) {
			ClearTimer c=new ClearTimer(key, value);
			timer.schedule(c, timeout);
		}
	}

	/**
	 * Remove an object from the cache.
	 * 
	 * @param key the key of the object to remove
	 * @return the previous object under this key (null if there wasn't one)
	 */
	public Object remove(String key) {
		return storage.remove(key);
	}

	/**
	 * Conditionally remove an object from the cache.  The object will only
	 * be removed if the value is equal to the specified value.
	 * 
	 * @param key the key
	 * @param value the value
	 * @return true if a value was removed
	 */
	public boolean remove(String key, Object value) {
		return storage.remove(key, value);
	}

	// timer that fires to clear stuff
	private static class ClearTimer extends TimerTask {
		private String key=null;
		private Object value=null;
		public ClearTimer(String k, Object v) {
			super();
			key=k;
			value=v;
		}
		@Override
		public void run() {
			SimpleCache.getInstance().remove(key, value);
		}
	}
}
