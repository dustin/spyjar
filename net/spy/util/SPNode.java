// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: SPNode.java,v 1.1 2002/10/18 07:11:04 dustin Exp $

package net.spy.util;

import java.util.SortedSet;

/**
 * An Node for a shortest path graph.
 *
 * This is essentially an object that weighted connections to other
 * objects.
 */
public interface SPNode extends Comparable {

	/** 
	 * Get all of the connections to other nodes.
	 * 
	 * @return a SortedSet of SPVertex objects.
	 */
	SortedSet getConnections();

}
