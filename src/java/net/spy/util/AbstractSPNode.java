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
public abstract class AbstractSPNode extends SpyObject implements SPNode {

	private SortedSet<SPVertex> links=null;
	private Map<SPNode, SPVertex> nextHops=null;

	/**
	 * Get an instance of AbstractSPNode holding its links in the given
	 * Set.
	 * @param l the links.
	 */
	protected AbstractSPNode(SortedSet<SPVertex> l) {
		super();
		this.links=l;
		nextHops=new WeakHashMap();
	}

	/** 
	 * Get an instance of AbstractSPNode.
	 */
	protected AbstractSPNode() {
		this(new TreeSet());
	}

	/** 
	 * Link this SPNode to the given SPNode at the given cost.
	 * 
	 * @param n SPNode to which to link
	 * @param cost cost of this link
	 */
	protected void linkTo(SPNode n, int cost) {
		links.add(new SPVertex(n, cost));
	}

	/** 
	 * Link this SPNode to the given SPNode at the default cost.
	 * 
	 * @param n the node to which to link
	 */
	protected void linkTo(SPNode n) {
		linkTo(n, SPVertex.DEFAULT_COST);
	}

	/** 
	 * @see SPNode 
	 */
	public SortedSet<SPVertex> getConnections() {
		return( Collections.unmodifiableSortedSet(links));
	}

	/** 
	 * @see SPNode
	 */
	public Map<SPNode, SPVertex> getNextHops() {
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
	public SPVertex getNextHop(SPNode n) {
		return(nextHops.get(n));
	}

	/** 
	 * @see SPNode
	 */
	public void addNextHop(SPNode n, SPVertex v) {
		nextHops.put(n, v);
	}

	/** 
	 * @see Object
	 */
	public boolean equals(Object o) {
		boolean rv=false;

		try {
			// Since this object is Comparable, we can use compareTo to
			// implement equals.
			rv=(compareTo(o) == 0);
		} catch(ClassCastException cce) {
			// Ignored, return false
		}

		return (rv);
	}

	/** 
	 * Must override hashCode along with compareTo();
	 */
	public abstract int hashCode();

}
