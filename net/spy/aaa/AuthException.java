// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: AuthException.java,v 1.1 2002/08/28 00:34:55 dustin Exp $

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
