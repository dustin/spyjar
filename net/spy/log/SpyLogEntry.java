// Copyright (c) 1999 Dustin Sallings
//
// $Id: SpyLogEntry.java,v 1.1 2002/08/28 00:34:56 dustin Exp $

package net.spy.log;

/**
 * An entry in the spy log.
 */

public class SpyLogEntry extends Object {
	public SpyLogEntry() {
		super();
	}

	/**
	 * toString <i>must</i> be overridden for this to be useful.
	 */
	public String toString() {
		return("ERROR:  Method should be overridden");
	}
}

