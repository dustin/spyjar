// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: SPNode.java,v 1.2 2002/10/19 08:32:21 dustin Exp $

package net.spy.util;

import java.util.SortedSet;

/**
 * An Node for a shortest path graph.
 *
 * This is essentially an object that weighted connections to other
 * objects.
 *
 * <p>
 * 
 * </p>
 */
public interface SPNode extends Comparable {

	/** 
	 * Get all of the connections to other nodes.
	 * 
	 * @return a SortedSet of SPVertex objects.
	 */
	SortedSet getConnections();

	/** 
	 * Clear out the next hop map.  This should only be called by an
	 * SPRouter.
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
	 * links.  This should only be called by a SPRouter.
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
