// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 88043DBE-1110-11D9-AC50-000A957659CC

package net.spy.util;

import java.util.Map;
import java.util.SortedSet;

/**
 * A Node for a shortest path graph.
 *
 * This is essentially an object that weighted connections to other
 * objects.
 *
 * <p>
 * 
 * </p>
 *
 * @see ShortestPathFinder
 */
public interface SPNode extends Comparable {

	/** 
	 * Get all of the connections to other nodes.
	 * 
	 * @return a SortedSet of {@link SPVertex} objects.
	 */
	SortedSet<SPVertex> getConnections();

	/** 
	 * Get the mapping of SPNode -&gt; SPVertex hops for this SPNode.
	 * 
	 * @return an unmodifiable Map representing mapping
	 */
	Map<SPNode, SPVertex> getNextHops();

	/** 
	 * Clear out the next hop map.  This should only be called by an
	 * {@link ShortestPathFinder}.
	 */
	void clearNextHops();

	/** 
	 * Get the next hop to take you to the given node.
	 * 
	 * @param n the node you want to get to
	 * @return the vertex describing the next hop, or null if there's no
	 * 			route to the given node
	 */
	SPVertex getNextHop(SPNode n);

	/** 
	 * Add a vertex to the next hop database to take you to a particular
	 * location.  This is used just for routing information, not normal
	 * links.  This should only be called by a {@link ShortestPathFinder}.
	 * 
	 * @param n the destination node
	 * @param v the vertex that will take you there
	 */
	void addNextHop(SPNode n, SPVertex v);

	/** 
	 * Object.hashCode must be overridden to return a consistent hashCode.
	 * Objects where
	 * <code>one.compareTo(other) == one.equals(other) == true</code>
	 * must return the same hashCode.
	 */
	int hashCode();

	/** 
	 * Object.equals must be overridden to return results consistent to
	 * compareTo(other).
	 * 
	 * @param o the object to which to compare
	 * @return true if the objects are equal
	 */
	boolean equals(Object o);

}
