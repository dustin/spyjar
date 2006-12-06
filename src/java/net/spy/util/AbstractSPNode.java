// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 80459114-1110-11D9-9389-000A957659CC

package net.spy.util;

import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;

import net.spy.SpyObject;

/**
 * Abstract implementation of SPNode to make implementation a bit easier.
 */
public abstract class AbstractSPNode<T extends SPNode<T>>
	extends SpyObject implements SPNode<T> {

	private SortedSet<SPVertex<T>> links=null;
	private Map<SPNode<T>, SPVertex<T>> nextHops=null;

	/**
	 * Get an instance of AbstractSPNode holding its links in the given
	 * Set.
	 * @param l the links.
	 */
	protected AbstractSPNode(SortedSet<SPVertex<T>> l) {
		super();
		this.links=l;
		nextHops=new WeakHashMap<SPNode<T>, SPVertex<T>>();
	}

	/** 
	 * Get an instance of AbstractSPNode.
	 */
	protected AbstractSPNode() {
		this(new TreeSet<SPVertex<T>>());
	}

	/** 
	 * Link this SPNode to the given SPNode at the given cost.
	 * 
	 * @param n SPNode to which to link
	 * @param cost cost of this link
	 */
	protected void linkTo(T n, int cost) {
		links.add(new SPVertex<T>(n, cost));
	}

	/** 
	 * Link this SPNode to the given SPNode at the default cost.
	 * 
	 * @param n the node to which to link
	 */
	protected void linkTo(T n) {
		linkTo(n, SPVertex.DEFAULT_COST);
	}

	/** 
	 * @see SPNode 
	 */
	public SortedSet<SPVertex<T>> getConnections() {
		return( Collections.unmodifiableSortedSet(links));
	}

	/** 
	 * @see SPNode
	 */
	public Map<SPNode<T>, SPVertex<T>> getNextHops() {
		return(Collections.unmodifiableMap(nextHops));
	}

	/** 
	 * @see SPNode
	 */
	public void clearNextHops() {
		nextHops.clear();
	}

	/** 
	 * @see SPNode
	 */
	public SPVertex<T> getNextHop(SPNode<T> n) {
		return(nextHops.get(n));
	}

	/** 
	 * @see SPNode
	 */
	public void addNextHop(SPNode<T> n, SPVertex<T> v) {
		nextHops.put(n, v);
	}

	/** 
	 * @see Object
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		boolean rv=false;

		try {
			// Since this object is Comparable, we can use compareTo to
			// implement equals.
			rv=(compareTo((T)o) == 0);
		} catch(ClassCastException cce) {
			// Ignored, return false
		}

		return (rv);
	}

	/** 
	 * Must override hashCode along with compareTo();
	 */
	@Override
	public abstract int hashCode();

}
