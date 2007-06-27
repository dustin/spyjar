// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

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
