// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 702B8A67-5EE9-11D9-B99F-000A957659CC

package net.spy.factory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.spy.SpyObject;

/**
 * An implementation of CacheEntry that is backed by a HashMap.
 */
public class Storage<T> extends SpyObject {

	private Collection<T> allObjects=null;
	private Map<String, Map<Object, T>> singleCache=null;
	private Map<String, Map<Object, List<T>>> multiCache=null;

	/**
	 * Get an instance of HashCacheEntry.
	 */
	public Storage(Collection<T> obs) {
		super();
		allObjects=Collections.unmodifiableCollection(obs);
		singleCache=new HashMap<String, Map<Object, T>>();
		multiCache=new HashMap<String, Map<Object,List<T>>>();
		index();
	}

	private void storeEntry(CacheKey ck, Object key, T val) {
		if(key != null) {
			String name=ck.name();
			switch(ck.type()) {
				case SINGLE:
					Map<Object, T> sm=singleCache.get(name);
					if(sm == null) {
						sm=new HashMap<Object, T>();
						singleCache.put(name, sm);
					}
					sm.put(key, val);
					break;
				case MULTI:
					Map<Object, List<T>> mm=multiCache.get(name);
					if(mm == null) {
						mm=new HashMap<Object, List<T>>();
						multiCache.put(name, mm);
					}
					List<T> l=mm.get(key);
					if(l == null) {
						l=new ArrayList<T>();
						mm.put(key, l);
					}
					l.add(val);
					break;
			}
		}
	}

	private void index() {
		for(T i : allObjects) {
			try {
				cacheInstance(i);
			} catch(Exception e) {
				throw new RuntimeException("Problem indexing at " + i, e);
			}
		}
	}

	/** 
	 * Cache this instance.
	 */
	private void cacheInstance(T i) throws Exception {
		for(Field f : i.getClass().getDeclaredFields()) {
			CacheKey ck=f.getAnnotation(CacheKey.class);
			if(ck != null) {
				f.setAccessible(true);
				storeEntry(ck, f.get(i), i);
				f.setAccessible(false);
			}
		}
		for(Method m : i.getClass().getMethods()) {
			CacheKey ck=m.getAnnotation(CacheKey.class);
			if(ck != null) {
				storeEntry(ck, m.invoke(i, new Object[0]), i);
			}
		}
	}

	/** 
	 * Get all of the objects in this cache.
	 * 
	 * @return an unmodifiable collection of object instances
	 */
	public Collection<T> getAllObjects() {
		return allObjects;
	}

	/**
	 * Get a specific object by cache name and key.
	 * 
	 * @param cacheName the name of the cache containing the object
	 * @param key the key under which the object is cached
	 * @return the object, or null if there's no match
	 */
	public T getObject(String cacheName, Object key) {
		T rv=null;
		Map<Object, T>ccache=singleCache.get(cacheName);
		if(ccache != null) {
			rv=ccache.get(key);
		}
		return rv;
	}

	/**
	 * Get the objects multicached to a particular key.
	 * 
	 * @param cacheName the name of the cache containing the objects
	 * @param key the key under which the objects are cached
	 * @return all the matching objects, or an empty collection if no match
	 */
	public Collection<T> getObjects(String cacheName, Object key) {
		Collection<T> rv=null;
		Map<Object, List<T>> m=multiCache.get(cacheName);
		if(m != null) {
			rv=m.get(key);
		}
		if(rv == null) {
			rv=Collections.emptyList();
		}
		return rv;
	}

}
