// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6DFD1D04-1110-11D9-BF3C-000A957659CC

package net.spy.db;

/**
 * Exception thrown when a Saver save fails.
 */
public class SaveException extends Exception {

	/**
	 * Get an instance of SaveException with a message.
	 */
	public SaveException(String msg) {
		super(msg);
	}

	/**
	 * Get an instance of SaveException with a message and a root cause.
	 */
	public SaveException(String msg, Throwable root) {
		super(msg, root);
	}

}
