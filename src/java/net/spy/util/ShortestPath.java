// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.util;

import java.util.ArrayList;

/**
 * Represents the shortest path between two SPNodes.  This class can be used
 * to find and store the shortest path between two {@link SPNode}
 * instances.
 *
 * @see ShortestPathFinder
 */
public class ShortestPath<N extends SPNode<N>> extends ArrayList<N> {

	private int cost=0;

	/**
	 * Get an instance of ShortestPath.
	 *
	 * @param from the starting node
	 * @param to the ending node
	 *
	 * @throws NoPathException if there's no path to the destination
	 * @throws NullPointerException if from or to is null
	 */
	public ShortestPath(N from, N to) throws NoPathException {
		super();

		if (from == null) {
			throw new NullPointerException("From may not be null.");
		}

		if (to == null) {
			throw new NullPointerException("To may not be null.");
		}

		SPVertex<N> v=from.getNextHop(to);
		if(v==null) {
			throw new NoPathException(from, to);
		}

		// This will be the cost for the complete path
		cost=v.getCost();

		N current=v.getTo();
		int i=0;
		while(! current.equals(to)) {
			if(++i>1024) {
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
