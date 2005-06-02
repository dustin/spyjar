// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 773E3F8A-1110-11D9-8879-000A957659CC

package net.spy.net;

import java.net.ConnectException;

/**
 * Exception thrown when IOs timeout.
 */
public class IOTimeoutException extends ConnectException {

	/**
	 * Get an instance of IOTimeoutException.
	 */
	public IOTimeoutException(String message) {
		super(message);
	}
}
