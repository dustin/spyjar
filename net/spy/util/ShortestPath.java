// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: ShortestPath.java,v 1.2 2002/10/19 08:32:21 dustin Exp $

package net.spy.util;

import java.util.ArrayList;

/**
 * Implements the shortest path between two nodes.
 */
public class ShortestPath extends ArrayList {

	private int cost=0;

	/**
	 * Get an instance of ShortestPath.
	 *
	 * @param from the starting node
	 * @param to the ending node
	 *
	 * @throws NoPathException if there's no path to the destination
	 * @throws NullPointerException if from or to is null
	 * @throws IllegalArgumentException if from and to are the same node
	 */
	public ShortestPath(SPNode from, SPNode to) throws NoPathException {
		super();

		if (from == null) {
			throw new NullPointerException("From may not be null.");
		}

		if (to == null) {
			throw new NullPointerException("To may not be null.");
		}

		if (from.equals(to)) {
			throw new IllegalArgumentException(
				"From and To must be different nodes.");
		}

		SPVertex v=from.getNextHop(to);
		if(v==null) {
			throw new NoPathException(from, to);
		}

		SPNode current=v.getTo();
		int i=0;
		while(! current.equals(to)) {
			if(i>1024) {
				throw new NoPathException(from, to, "Too deep!");
			}

			add(current);

			v=current.getNextHop(to);
			current=v.getTo();
		}

		add(current);
	}

	/** 
	 * Get the cost of this path.
	 */
	public int getCost() {
		return(cost);
	}

}
