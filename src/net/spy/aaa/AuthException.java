// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 5012030A-1110-11D9-A8A5-000A957659CC

package net.spy.aaa;

import net.spy.util.NestedException;

/**
 * Exception thrown when authentication failures occur.
 */
public class AuthException extends NestedException {

	/**
	 * Get an instance of AuthException.
	 */
	public AuthException(String message) {
		super(message);
	}

	/**
	 * Get an instance of AuthException.
	 */
	public AuthException(String message, Throwable root) {
		super(message, root);
	}

}
