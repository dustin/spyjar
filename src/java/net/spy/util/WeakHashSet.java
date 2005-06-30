// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 8E2BBB45-1110-11D9-938F-000A957659CC

package net.spy.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.WeakHashMap;

/**
 * HashSet backed by a WeakHashMap.
 */
public class WeakHashSet<T extends Object> extends AbstractSet<T> {

	private transient WeakHashMap<T, Object> map=null;

	private static final Object PRESENT=new Object();

	/**
	 * Get an instance of WeakHashSet.
	 */
	public WeakHashSet() {
		super();
		map=new WeakHashMap();
	}

	/** 
	 * Create a WeakHashSet with the given capacity.
	 * 
	 * @param n the capacity
	 */
	public WeakHashSet(int n) {
		super();
		map=new WeakHashMap(n);
	}

	/** 
	 * Get a WeakHashSet with the contents from the given Collection.
	 * 
	 * @param c the collection
	 */
	public WeakHashSet(Collection<T> c) {
		this();
		if(c == null) {
			throw new NullPointerException(
				"Null collection provided to WeakHashSet");
		}
		addAll(c);
	}

	/** 
	 * Get the Iterator for the backing Map.
	 */
	public Iterator<T> iterator() {
		return(map.keySet().iterator());
	}

	/** 
	 * Get the number of keys currently contained in this Set.
	 */
	public int size() {
		return(map.size());
	}

	/** 
	 * True if this set contains no elements.
	 */
	public boolean isEmpty() {
		return(map.isEmpty());
	}

	/** 
	 * True if this Set contains the given Object.
	 */
	public boolean contains(Object o) {
		return(map.containsKey(o));
	}

	/** 
	 * Add this object to this Set if it's not already present.
	 * 
	 * @param o the object to add
	 * @return true if this object was just added, false if it already existed
	 */
	public boolean add(T o) {
		Object old=map.put(o, PRESENT);
		return(old == null);
	}

	/** 
	 * Remove the given object from this Set.
	 * 
	 * @param o Object to be removed
	 * @return true if the Set did contain this object (but now doesn't)
	 */
	public boolean remove(Object o) {
		Object old=map.remove(o);
		return(old==PRESENT);
	}

	/** 
	 * Remove all entries from this Set.
	 */
	public void clear() {
		map.clear();
	}

}
