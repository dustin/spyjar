// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6BD76734-1110-11D9-9EFD-000A957659CC

package net.spy.db;

/**
 * Exception thrown from KeyStore when too many keys are requested.
 */
public class OverDrawnException extends Exception {

	/**
	 * Get an instance of OverDrawnException.
	 */
	public OverDrawnException() {
		super();
	}

}
