// Copyright (c) 1999 Dustin Sallings
//
// arch-tag: 75C36C5F-1110-11D9-8EBD-000A957659CC

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

