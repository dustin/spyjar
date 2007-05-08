package net.spy.factory;

import java.util.Collection;

/**
 * Interface for storing stuff in a GenFactory.
 */
public interface Storage<T> {

	/** 
	 * Cache this instance.
	 */
	void cacheInstance(T i) throws Exception;

	/** 
	 * Get all of the objects in this cache.
	 * 
	 * @return an unmodifiable collection of object instances
	 */
	Collection<T> getAllObjects();

	/**
	 * Get a specific object by cache name and key.
	 * 
	 * @param cacheName the name of the cache containing the object
	 * @param key the key under which the object is cached
	 * @return the object, or null if there's no match
	 */
	T getObject(String cacheName, Object key);

	/**
	 * Get the objects multicached to a particular key.
	 * 
	 * @param cacheName the name of the cache containing the objects
	 * @param key the key under which the objects are cached
	 * @return all the matching objects, or an empty collection if no match
	 */
	Collection<T> getObjects(String cacheName, Object key);

}