// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: SaveContext.java,v 1.2 2004/02/02 23:23:57 dustin Exp $

package net.spy.db;

import java.util.Hashtable;

/**
 * Context shared by all Savables inside a Saver.  This lets the Savables
 * keep track of whatever state they may need in order to maintain key
 * integrity or whatever.
 */
public class SaveContext extends Hashtable {

	/**
	 * Get an instance of SaveContext.
	 */
	public SaveContext() {
		super();
	}

	/** 
	 * Get the session ID for this context.  The session ID is not guaranteed
	 * to be globally unique, just unique enough for local correlation in logs.
	 */
	public int getId() {
		return(System.identityHashCode(this));
	}

}
