// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 885F60DE-1110-11D9-B9CC-000A957659CC

package net.spy.util;

import java.lang.ref.WeakReference;

/**
 * A weighted connection to a SPNode.
 *
 * @see ShortestPathFinder
 */
public class SPVertex extends Object implements Comparable {

	// A weak reference to the next spnode
	private WeakReference to=null;
	private int cost=0;

	public static final int DEFAULT_COST=10;

	/**
	 * Get an instance of SPVertex.
	 *
	 * @param to the destination node
	 * @param cost the cost
	 */
	public SPVertex(SPNode to, int cost) {
		super();
		if(to == null) {
			throw new NullPointerException("Destination node may not be null.");
		}
		this.to=new WeakReference(to);
		this.cost=cost;
	}

	/** 
	 * String me.
	 */
	public String toString() {
		StringBuffer sb=new StringBuffer(32);

		sb.append("{SPVertex cost=");
		sb.append(cost);
		sb.append(" dest=");
		sb.append(to.get());
		sb.append("}");

		return(sb.toString());
	}

	/** 
	 * Get an instance of SPVertex linking the two nodes with the default
	 * cost.
	 * 
	 * @param to the destination node
	 */
	public SPVertex(SPNode to) {
		this(to, DEFAULT_COST);
	}

	/** 
	 * Get the destination node.
	 */
	public SPNode getTo() {
		return( (SPNode)to.get());
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
	public int compareTo(Object o) {
		SPVertex other=(SPVertex)o;
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
