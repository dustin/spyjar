// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.util;

import java.lang.ref.WeakReference;

/**
 * A weighted connection to a SPNode.
 *
 * @see ShortestPathFinder
 */
public class SPVertex<N extends SPNode<N>> extends Object
	implements Comparable<SPVertex<N>> {

	// A weak reference to the next spnode
	private final WeakReference<N> to;
	private final int cost;

	public static final int DEFAULT_COST=10;

	/**
	 * Get an instance of SPVertex.
	 *
	 * @param destination the destination node
	 * @param theCost the cost
	 */
	public SPVertex(N destination, int theCost) {
		super();
		if(destination == null) {
			throw new NullPointerException("Destination node may not be null.");
		}
		this.to=new WeakReference<N>(destination);
		this.cost=theCost;
	}

	/** 
	 * String me.
	 */
	@Override
	public String toString() {
		return("{SPVertex cost=" + cost + " dest=" + to.get() + "}");
	}

	/** 
	 * Get an instance of SPVertex linking the two nodes with the default
	 * cost.
	 * 
	 * @param destination the destination node
	 */
	public SPVertex(N destination) {
		this(destination, DEFAULT_COST);
	}

	/** 
	 * Get the destination node.
	 */
	public N getTo() {
		return(to.get());
	}

	/** 
	 * Get the cost of this vertex.
	 */
	public int getCost() {
		return(cost);
	}

	/** 
	 * Compare this vertex to another vertex.
	 *
	 * Weight will be considered first.  If vertices are at the same
	 * weight, the destination nodes will be compared.
	 */
	public int compareTo(SPVertex<N> other) {
		int rv=0;

		if( getCost() > other.getCost()) {
			rv=1;
		} else if(getCost() < other.getCost()) {
			rv=-1;
		} else {
			rv=getTo().compareTo(other.getTo());
		}

		return(rv);
	}

}
