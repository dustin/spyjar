// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: ShortestPathFinder.java,v 1.1 2002/10/19 08:32:21 dustin Exp $

package net.spy.util;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;

/**
 * This is a utility class for finding the least costly paths from each node
 * from a collection to all nodes to which they link.
 */
public class ShortestPathFinder extends Object {

	/**
	 * Get an instance of ShortestPathFinder.
	 */
	public ShortestPathFinder() {
		super();
	}

	/** 
	 * Calculate all the paths for all the nodes in the given collection.
	 * 
	 * @param nodes the nodes to recalculate.
	 */
	public void calculatePaths(Collection nodes) {
		for(Iterator i=nodes.iterator(); i.hasNext(); ) {
			calculatePaths( (SPNode)i.next() );
		}
	}

	// Calculate all the paths for the given node.
	private void calculatePaths(SPNode node) {
		Set nodesSeen=new HashSet();
		// Clear the current list
		node.clearNextHops();

		for(Iterator i=node.getConnections().iterator(); i.hasNext(); ) {
			SPVertex spv=(SPVertex)i.next();
			recordLink(node, spv.getCost(), spv.getTo(), spv.getTo(),
				nodesSeen);
		}
	}

	// Calculate the links in node ``node'' at the given cost, routed over
	// the given next hop, starting at node ``other'' and maintaining seen
	// links in s
	private void recordLink(SPNode node, int cost, SPNode nextHop,
		SPNode other, Set s) {

		if(! s.contains(other)) {
			s.add(other);

			node.addNextHop(other, new SPVertex(nextHop, cost));

			for(Iterator i=other.getConnections().iterator(); i.hasNext();) {
				SPVertex spv=(SPVertex)i.next();
				int nextCost=cost+spv.getCost();
				node.addNextHop(spv.getTo(), new SPVertex(nextHop, nextCost));
				recordLink(node, cost+spv.getCost(), nextHop, spv.getTo(), s);
			}

			s.remove(other);
		}
	}
}
