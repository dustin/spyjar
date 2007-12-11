// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.factory;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.spy.SpyObject;

/**
 * Locate relevant CacheKeys for a given class.
 */
public class CacheKeyFinder extends SpyObject {

	private static CacheKeyFinder instance=null;

	private final ConcurrentMap<Class<?>, Map<CacheKey, Accessor<?>>> memo=
		new ConcurrentHashMap<Class<?>, Map<CacheKey, Accessor<?>>>();

	protected CacheKeyFinder() {
		super();
	}

	/**
	 * Get the singleton CacheKeyFinder instance.
	 */
	public static synchronized CacheKeyFinder getInstance() {
		if(instance == null) {
			instance=new CacheKeyFinder();
		}
		return instance;
	}

	/**
	 * Set the singleton CacheKeyFinder instance.
	 */
	public static void setInstance(CacheKeyFinder to) {
		instance=to;
	}

	/**
	 * Get the cache keys for the given class.
	 *
	 * @param c the class to search
	 * @return the cache keys for the given class
	 */
	public Map<CacheKey, Accessor<?>> getCacheKeys(Class<?> c) {
		Map<CacheKey, Accessor<?>> rv=memo.get(c);
		if(rv == null) {
			synchronized(c) {
				rv=memo.get(c);
				if(rv == null) {
					rv=new HashMap<CacheKey, Accessor<?>>();
					lookupCacheKeys(rv, c);
				}
			}
		}
		return rv;
	}

	private void lookupCacheKeys(Map<CacheKey, Accessor<?>> rv, Class<?> c) {
		// Get the recursion out of the way first
		Class<?> sup=c.getSuperclass();
		if(sup != null) {
			lookupCacheKeys(rv, sup);
		}
		for(Class<?> i : c.getInterfaces()) {
			lookupCacheKeys(rv, i);
		}

		// Do the real work
		for(Method m : c.getDeclaredMethods()) {
			CacheKey ck=m.getAnnotation(CacheKey.class);
			if(ck != null) {
				rv.put(ck, new MethodAccessor(m));
			}
		}
		for(Field f : c.getDeclaredFields()) {
			CacheKey ck=f.getAnnotation(CacheKey.class);
			if(ck != null) {
				rv.put(ck, new FieldAccessor(f));
			}
		}
	}

	/**
	 * Class to access an object from within another object.
	 */
	public static abstract class Accessor<T extends AccessibleObject> {
		protected final T ao;
		protected Accessor(T o) {
			ao=o;
		}
		/**
		 * Get the value from the given object.
		 *
		 * @param o the object
		 * @return the value
		 * @throws Exception if an exception is thrown while accessing
		 */
		public final Object get(Object o) throws Exception {
			boolean accessible=ao.isAccessible();
			if(!accessible) {
				ao.setAccessible(true);
			}
			Object rv=realGet(o);
			if(!accessible) {
				ao.setAccessible(false);
			}
			return rv;
		}
		protected abstract Object realGet(Object o) throws Exception;
	}

	private static class MethodAccessor extends Accessor<Method> {
		public MethodAccessor(Method m) {
			super(m);
		}
		@Override
		protected Object realGet(Object o) throws Exception {
			return ao.invoke(o, new Object[0]);
		}
	}

	private static class FieldAccessor extends Accessor<Field> {
		public FieldAccessor(Field f) {
			super(f);
		}
		@Override
		public Object realGet(Object o) throws Exception {
			return ao.get(o);
		}
	}
}
