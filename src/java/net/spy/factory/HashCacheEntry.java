// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 702B8A67-5EE9-11D9-B99F-000A957659CC

package net.spy.factory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.spy.SpyObject;

/**
 * An implementation of CacheEntry that is backed by a HashMap.
 */
public class HashCacheEntry<T extends Instance> extends SpyObject
	implements CacheEntry<T> {

	private Map<Integer, T> cache=null;
	private Map<String, Map<Object, T>> altCache=null;

	/**
	 * Get an instance of HashCacheEntry.
	 */
	public HashCacheEntry() {
		super();
		cache=new HashMap<Integer, T>();
		altCache=new HashMap<String, Map<Object, T>>();
	}

	private Object invoke(Method m, T i) {
		try {
			return m.invoke(i, new Object[]{});
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	/** 
	 * Cache this instance.
	 */
	public void cacheInstance(T i) {
		cache.put(i.getId(), i);
		for(Method m : i.getClass().getMethods()) {
			CacheKey ck=m.getAnnotation(CacheKey.class);
			if(ck != null) {
				Map<Object, T> ccache=altCache.get(ck.name());
				if(ccache == null) {
					ccache=new HashMap<Object, T>();
					altCache.put(ck.name(), ccache);
				}
				ccache.put(invoke(m, i), i);
			}
		}
	}

	/** 
	 * Get the object at this ID.
	 * @param id the object ID
	 * @return the object, or null if there's no object by this ID
	 */
	public T getById(int id) {
		return(cache.get(id));
	}

	/** 
	 * Get all of the objects in this cache.
	 * 
	 * @return an unmodifiable collection of object instances
	 */
	public Collection<T> getAllObjects() {
		return(Collections.unmodifiableCollection(cache.values()));
	}

	public T getByAltCache(String cacheName, Object key) {
		T rv=null;
		Map<Object, T>ccache=altCache.get(cacheName);
		if(ccache != null) {
			rv=ccache.get(key);
		}
		return rv;
	}

}
