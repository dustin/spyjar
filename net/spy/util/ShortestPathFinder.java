// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: ShortestPathFinder.java,v 1.3 2002/11/04 08:16:07 dustin Exp $

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
 *
 * <p>
 * The basic process is to create some {@link SPNode}s and link them
 * together arbitrarily with {@link SPVertex} instances.  Once the graph is
 * complete, obtain an instance of ShortestPathFinder and have it calculate
 * the paths for any (or all) nodes.  calculatePaths may be called as many
 * times as you need, it will reset the paths and build new ones.
 * </p>
 *
 * <p>
 * <table>
 *  <tr width="100%">
 *   <td>
 *    For example, consider the graph to the right.   The following will be
 *    true (this is actually my test case):
 *    <table border="1">
 *     <tr><th>From</th><th>To</th><th>Next Hop</th><th>Cost</th></tr>
 *
 *     <tr><td>A</td><td>B</td><td>B</td><td>10</td></tr>
 *     <tr><td>A</td><td>C</td><td>C</td><td>15</td></tr>
 *     <tr><td>A</td><td>D</td><td>C</td><td>25</td></tr>
 *     <tr><td>A</td><td>E</td><td>C</td><td>25</td></tr>
 *
 *     <tr><td>B</td><td>A</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>B</td><td>C</td><td>C</td><td>10</td></tr>
 *     <tr><td>B</td><td>D</td><td>C</td><td>20</td></tr>
 *     <tr><td>B</td><td>E</td><td>C</td><td>20</td></tr>
 *
 *     <tr><td>C</td><td>A</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>C</td><td>B</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>C</td><td>D</td><td>D</td><td>10</td></tr>
 *     <tr><td>C</td><td>E</td><td>E</td><td>10</td></tr>
 *
 *     <tr><td>D</td><td>A</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>D</td><td>B</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>D</td><td>C</td><td>C</td><td>100</td></tr>
 *     <tr><td>D</td><td>E</td><td>C</td><td>110</td></tr>
 *
 *     <tr><td>E</td><td>A</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>E</td><td>B</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>E</td><td>C</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>E</td><td>D</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *
 *    </table>
 *   </td>
 *   <td>
 *     <img src="../../../images/graphtest.png">
 *   </td>
 *  </tr>
 * </table>
 * </p>
 *
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
	 * @param nodes the nodes to calculate
	 */
	public void calculatePaths(Collection nodes) {
		for(Iterator i=nodes.iterator(); i.hasNext(); ) {
			calculatePaths( (SPNode)i.next() );
		}
	}


	/** 
	 * Calculate all of the paths for a single node.
	 * 
	 * @param node the node from which to calculate paths
	 */
	public void calculatePaths(SPNode node) {
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
				recordLink(node, nextCost, nextHop, spv.getTo(), s);
			}

			s.remove(other);
		}
	}
}
