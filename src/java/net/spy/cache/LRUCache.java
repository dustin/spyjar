// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 60E626CC-1110-11D9-ACFA-000A957659CC

package net.spy.cache;

import java.lang.ref.Reference;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A fixed-size least-recently-used cache.
 */
public class LRUCache<K,V> extends LinkedHashMap<K,V> {

	private int maxSize=0;

	/**
	 * Get an instance of LRUCache.
	 */
	public LRUCache(int size) {
		super(size, 0.75f, true);
		maxSize=size;
	}

	/**
	 * Get the object from the cache, and defereference it if it's a reference.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public V get(Object o) {
		V rv=super.get(o);
		if(rv instanceof Reference) {
			rv=(V) ((Reference)rv).get();
		}
		return rv;
	}

	@Override
	public V put(K key, V val) {
		if(val instanceof CacheListener) {
			CacheListener cl=(CacheListener)val;
			cl.cachedEvent(key);
		}
		return super.put(key, val);
	}

	// When an object is removed from the cache, send a removed event if it
	// wants to hear it
	private void sendRemovedEvent(Object key, Object value) {
		if(value!=null && value instanceof CacheListener) {
			CacheListener cl=(CacheListener)value;
			cl.uncachedEvent(key);
		}
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> e) {
		boolean shouldRemove=false;
		shouldRemove=size() > maxSize;
		if(shouldRemove) {
			sendRemovedEvent(e.getKey(), e.getValue());
		}
		return shouldRemove;
	}
}