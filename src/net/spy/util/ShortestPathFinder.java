// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: ShortestPathFinder.java,v 1.7 2003/08/05 09:01:05 dustin Exp $

package net.spy.util;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;

import net.spy.SpyObject;

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
 *     <tr><td>A</td><td>A</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>A</td><td>B</td><td>B</td><td>10</td></tr>
 *     <tr><td>A</td><td>C</td><td>C</td><td>15</td></tr>
 *     <tr><td>A</td><td>D</td><td>C</td><td>25</td></tr>
 *     <tr><td>A</td><td>E</td><td>C</td><td>25</td></tr>
 *
 *     <tr><td>B</td><td>A</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>B</td><td>B</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>B</td><td>C</td><td>C</td><td>10</td></tr>
 *     <tr><td>B</td><td>D</td><td>C</td><td>20</td></tr>
 *     <tr><td>B</td><td>E</td><td>C</td><td>20</td></tr>
 *
 *     <tr><td>C</td><td>A</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>C</td><td>B</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>C</td><td>C</td><td>D</td><td>110</td></tr>
 *     <tr><td>C</td><td>D</td><td>D</td><td>10</td></tr>
 *     <tr><td>C</td><td>E</td><td>E</td><td>10</td></tr>
 *
 *     <tr><td>D</td><td>A</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>D</td><td>B</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>D</td><td>C</td><td>C</td><td>100</td></tr>
 *     <tr><td>D</td><td>D</td><td>C</td><td>110</td></tr>
 *     <tr><td>D</td><td>E</td><td>C</td><td>110</td></tr>
 *
 *     <tr><td>E</td><td>A</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>E</td><td>B</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>E</td><td>C</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>E</td><td>D</td><td><i>n/a</i></td><td><i>n/a</i></td></tr>
 *     <tr><td>E</td><td>E</td><td>E</td><td>10</td></tr>
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
public class ShortestPathFinder extends SpyObject {

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
		// First, clear them all
		for(Iterator i=nodes.iterator(); i.hasNext();) {
			SPNode sn=(SPNode)i.next();
			sn.clearNextHops();
		}
		// Now, calculate them
		for(Iterator i=nodes.iterator(); i.hasNext();) {
			// Only calculate if there are no hops
			SPNode sn=(SPNode)i.next();
			if(sn.getNextHops().size() == 0) {
				if(getLogger().isDebugEnabled()) {
					getLogger().debug("No hops for " + sn + ", calculating...");
				}
				calculatePathsWithoutClearing(sn, new HashSet());
			} else {
				if(getLogger().isDebugEnabled()) {
					getLogger().debug("Not recalculating " + sn);
				}
			}
		}
	}

	/** 
	 * Calculate all of the paths for a single node.  Note:  this completely
	 * recalculates the next hops, so it starts by clearing
	 * 
	 * @param node the node from which to calculate paths
	 */
	public void calculatePaths(SPNode node) {
		// Clear the current list
		node.clearNextHops();

		calculatePathsWithoutClearing(node, new HashSet());
	}

	private void calculatePathsWithoutClearing(SPNode node, Set seen) {
		if(!seen.contains(node)) {
			seen.add(node);

			// Add all connections
			for(Iterator i=node.getConnections().iterator(); i.hasNext();) {
				SPVertex spv=(SPVertex)i.next();
				SPNode spn=spv.getTo();

				node.addNextHop(spn, spv);
			}

			// Flip through the list again for recursing
			for(Iterator i=node.getConnections().iterator(); i.hasNext();) {
				SPVertex spv=(SPVertex)i.next();
				SPNode spn=spv.getTo();
				// Make sure the sub's performed his calculations
				if(spn.getNextHops().size() == 0) {
					calculatePathsWithoutClearing(spn, seen);
				}

				// Add all of the stuff from the next hop with the prefix delta
				for(Iterator hi=spn.getNextHops().entrySet().iterator();
					hi.hasNext();) {
					Map.Entry me=(Map.Entry)hi.next();

					SPNode nspn=(SPNode)me.getKey();
					SPVertex nspv=(SPVertex)me.getValue();

					node.addNextHop(nspn,
						new SPVertex(spn, spv.getCost() + nspv.getCost()));
				}
			} // for children
			seen.remove(node);
		} // not seen
	} // calculatePathsWithoutClearing
}
