// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: IOTimeoutException.java,v 1.1 2002/08/28 00:34:56 dustin Exp $

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
