// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 53DCD258-1110-11D9-822A-000A957659CC

package net.spy.aaa;

import net.spy.util.NestedRuntimeException;

/**
 * An Exception that will allow chaining of another Throwable.
 */
public class SpySecurityException extends NestedRuntimeException {

	private Throwable root=null;

	/**
	 * Allow subclasses to get an exception without a message.
	 */
	protected SpySecurityException() {
		super();
	}

	/**
	 * Get an instance of SpySecurityException with a given message.
	 */
	public SpySecurityException(String msg) {
		super(msg);
	}

	/**
	 * Get a SpySecurityException with a given message and root cause
	 * throwable.
	 */
	public SpySecurityException(String msg, Throwable t) {
		super(msg);
		root=t;
	}

}
