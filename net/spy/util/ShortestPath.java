// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: ShortestPath.java,v 1.1 2002/10/18 07:11:04 dustin Exp $

package net.spy.util;

import java.util.ArrayList;

/**
 * Implements the shortest path between two nodes.
 */
public class ShortestPath extends ArrayList {

	private int cost=0;

	/**
	 * Get an instance of ShortestPath.
	 */
	public ShortestPath(SPNode from, SPNode to) throws NoPathException {
		super();

		throw new NoPathException(from, to);
	}

	/** 
	 * Get the cost of this path.
	 */
	public int getCost() {
		return(cost);
	}

}
