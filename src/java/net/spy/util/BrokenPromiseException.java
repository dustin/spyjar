// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.util;

/**
 * Report on a broken promise.
 */
public class BrokenPromiseException extends Exception {

	/**
	 * Get an instance of BrokenPromiseException with a message.
	 */
	public BrokenPromiseException(String msg) {
		super(msg);
	}

	/**
	 * Get an instance of BrokenPromiseException with a message and a root
	 * cause Throwable.
	 */
	public BrokenPromiseException(String msg, Throwable t) {
		super(msg, t);
	}

}

