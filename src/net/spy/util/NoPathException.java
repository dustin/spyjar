// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: NoPathException.java,v 1.1 2002/10/19 08:32:21 dustin Exp $

package net.spy.util;

/**
 * 
 */
public class NoPathException extends Exception {

	/**
	 * Get an instance of NoPathException.
	 */
	public NoPathException(SPNode from, SPNode to) {
		super("No path from " + from + " to " + to);
	}

	/**
	 * Get an instance of NoPathException with a message.
	 */
	public NoPathException(SPNode from, SPNode to, String msg) {
		super("No path from " + from + " to " + to + " - " + msg);
	}

}
