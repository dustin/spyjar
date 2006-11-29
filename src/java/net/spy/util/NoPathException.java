// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 84B366EB-1110-11D9-9B12-000A957659CC

package net.spy.util;

/**
 * 
 */
public class NoPathException extends Exception {

	/**
	 * Get an instance of NoPathException.
	 */
	public NoPathException(SPNode<?> from, SPNode<?> to) {
		super("No path from " + from + " to " + to);
	}

	/**
	 * Get an instance of NoPathException with a message.
	 */
	public NoPathException(SPNode<?> from, SPNode<?> to, String msg) {
		super("No path from " + from + " to " + to + " - " + msg);
	}

}
