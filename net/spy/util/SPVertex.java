// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: SPVertex.java,v 1.1 2002/10/18 07:11:04 dustin Exp $

package net.spy.util;

/**
 * A weighted connection to a SPNode.
 */
public class SPVertex extends Object implements Comparable {

	private SPNode from=null;
	private SPNode to=null;
	private int cost=0;

	public static final int DEFAULT_COST=10;

	/**
	 * Get an instance of SPVertex.
	 *
	 * @param from the source node
	 * @param to the destination node
	 * @param cost the cost
	 */
	public SPVertex(SPNode from, SPNode to, int cost) {
		super();
		if(from == null) {
			throw new NullPointerException("Source node may not be null.");
		}
		this.from=from;
		if(to == null) {
			throw new NullPointerException("Destination node may not be null.");
		}
		this.to=to;
		this.cost=cost;
	}

	/** 
	 * Get an instance of SPVertex linking the two nodes with the default
	 * cost.
	 * 
	 * @param from the source node
	 * @param to the destination node
	 */
	public SPVertex(SPNode from, SPNode to) {
		this(from, to, DEFAULT_COST);
	}

	/** 
	 * Get the source node.
	 */
	public SPNode getFrom() {
		return(from);
	}

	/** 
	 * Get the destination node.
	 */
	public SPNode getTo() {
		return(to);
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
