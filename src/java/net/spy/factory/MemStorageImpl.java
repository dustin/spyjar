// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 702B8A67-5EE9-11D9-B99F-000A957659CC

package net.spy.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.spy.SpyObject;

/**
 * An implementation of Storage that is backed by a HashMap.
 * 
 * This implementation will find any public methods on the given objects
 * containing a CacheKey annotation, or any fields with any visibility declared
 * only within the classes of the given objects and use them as cache hints.
 */
public class MemStorageImpl<T> extends SpyObject implements Storage<T> {

	private Collection<T> allObjects;
	private final Map<String, Map<Object, T>> singleCache;
	private final Map<String, Map<Object, List<T>>> multiCache;

	/**
	 * Get an instance of HashCacheEntry.
	 */
	public MemStorageImpl(Collection<T> obs) {
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
				internalCacheInstance(i);
			} catch(Exception e) {
				throw new RuntimeException("Problem indexing at " + i, e);
			}
		}
	}

	/* (non-Javadoc)
	 * @see net.spy.factory.Storage#cacheInstance(T)
	 */
	public void cacheInstance(T i) throws Exception {
		internalCacheInstance(i);
		if(!allObjects.contains(i)) {
			Collection<T> newAll=new ArrayList<T>(allObjects);
			newAll.add(i);
			allObjects=Collections.unmodifiableCollection(newAll);
		}
	}

	private void internalCacheInstance(T i) throws Exception {
		CacheKeyFinder ckf=CacheKeyFinder.getInstance();
		Map<CacheKey,CacheKeyFinder.Accessor<?>> m=
			ckf.getCacheKeys(i.getClass());
		for(Map.Entry<CacheKey, CacheKeyFinder.Accessor<?>> me : m.entrySet()) {
			storeEntry(me.getKey(), me.getValue().get(i), i);
		}
	}

	/* (non-Javadoc)
	 * @see net.spy.factory.Storage#getAllObjects()
	 */
	public Collection<T> getAllObjects() {
		return allObjects;
	}

	/* (non-Javadoc)
	 * @see net.spy.factory.Storage#getObject(java.lang.String, java.lang.Object)
	 */
	public T getObject(String cacheName, Object key) {
		T rv=null;
		Map<Object, T>ccache=singleCache.get(cacheName);
		if(ccache != null) {
			rv=ccache.get(key);
		}
		return rv;
	}

	/* (non-Javadoc)
	 * @see net.spy.factory.Storage#getObjects(java.lang.String, java.lang.Object)
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
