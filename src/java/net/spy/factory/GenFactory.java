package net.spy.factory;

import java.util.Collection;

/**
 * Generic factory interface.
 */
public interface GenFactory<T> {

	/**
	 * Get an object from an alternate cache by cache name and key.
	 *
	 * @param cacheName the name of the alt cache
	 * @param key the key under which to look
	 * @return the object instance, or null if there's no such object
	 */
	T getObject(String cacheName, Object key);

	/**
	 * Convenience method for getObject(String,Object) assuming an integer
	 * field uniquely cached as ``id.''
	 *
	 * @param id the id value
	 * @return the object stored with this id
	 */
	T getObject(int id);

	/**
	 * Get all of the objects mapped with the given key under the given
	 * cache name.
	 *
	 * @param cacheName the name of the cache
	 * @param key the key with that name
	 * @return the objects mapped to that key, or an empty string if none
	 */
	Collection<T> getObjects(String cacheName, Object key);

	/**
	 * Get all objects cached by this factory.
	 */
	Collection<T> getObjects();

	/**
	 * Reset the cache for this factory.
	 */
	void recache();

	/**
	 * Get the timestamp of the last time this was refreshed.
	 */
	long getLastRefresh();

}