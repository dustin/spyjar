// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: AbstractSPNode.java,v 1.3 2003/07/26 07:46:53 dustin Exp $

package net.spy.util;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.Collections;

/**
 * Abstract implementation of SPNode to make implementation a bit easier.
 */
public abstract class AbstractSPNode extends Object implements SPNode {

	private SortedSet links=null;
	private Map nextHops=null;

	/**
	 * Get an instance of AbstractSPNode holding its links in the given
	 * Set.
	 */
	protected AbstractSPNode(SortedSet links) {
		this();
		this.links=links;
		nextHops=new WeakHashMap();
	}

	/** 
	 * Get an instance of AbstractSPNode.
	 */
	protected AbstractSPNode() {
		this.links=new TreeSet();
		nextHops=new WeakHashMap();
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
	public SortedSet getConnections() {
		return( Collections.unmodifiableSortedSet(links));
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
		return( (SPVertex)nextHops.get(n));
	}

	/** 
	 * @see SPNode
	 */
	public void addNextHop(SPNode n, SPVertex v) {
		SPVertex currentHop=getNextHop(n);
		if (currentHop == null) {
			nextHops.put(n, v);
		} else {
			// If we already have a next hop at a lower cost, keep the
			// current one, else add a new one
			if(v.getCost() < currentHop.getCost()) {
				nextHops.put(n, v);
			}
		}
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
