// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 60E626CC-1110-11D9-ACFA-000A957659CC

package net.spy.cache;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;

import net.spy.SpyObject;

/**
 * A fixed-size least-recently-used cache.
 */
public class LRUCache extends SpyObject {

	private int maxSize=0;

	private Map map=null;
	private LinkedList list=null;

	/**
	 * Get an instance of LRUCache.
	 */
	public LRUCache(int size) {
		super();
		map=new HashMap(size);
		list=new LinkedList();
		maxSize=size;
	}

	/** 
	 * Get the named object from the cache.
	 * 
	 * @param key the key
	 * @return the value, or null if this mapping is not in the cache
	 */
	public synchronized Object get(Object key) {
		updateLRU(key);
		return(map.get(key));
	}

	private void updateLRU(Object key) {
		if(map.containsKey(key)) {
			list.remove(key);
			list.addFirst(key);
		}
	}

	/** 
	 * Store an object in the cache.  This may cause an object to be
	 * removed from the cache to make room.
	 * 
	 * @param key the cache key
	 * @param value the cache value
	 */
	public synchronized void put(Object key, Object value) {

		// If the cache is full, make room
		while(map.size() >= maxSize) {
			Object removed=map.remove(list.getLast());
			list.removeLast();
			sendRemovedEvent(key, removed);
		}

		// Add the new object.  If this caused an object to be removed,
		// make sure the event is sent.
		Object removed=map.put(key, value);
		sendRemovedEvent(key, removed);
		updateLRU(key);

		// If the new value wants a cached event, send it
		if(value instanceof CacheListener) {
			CacheListener cl=(CacheListener)value;
			cl.cachedEvent(key);
		}
	}

	// When an object is removed from the cache, send a removed event if it
	// wants to hear it
	private void sendRemovedEvent(Object key, Object value) {
		if(value!=null && value instanceof CacheListener) {
			CacheListener cl=(CacheListener)value;
			cl.uncachedEvent(key);
		}
	}

}
