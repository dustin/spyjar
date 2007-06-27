// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.db;

import java.util.Hashtable;

/**
 * Context shared by all Savables inside a Saver.  This lets the Savables
 * keep track of whatever state they may need in order to maintain key
 * integrity or whatever.
 */
public class SaveContext extends Hashtable<String, Object> {

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
