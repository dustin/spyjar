// Copyright (c) 2002  SPY internetworking <dustin@spy.net>
//
// $Id: DefaultLogger.java,v 1.1 2002/11/04 21:13:57 dustin Exp $

package net.spy.log;

/**
 * Default logger implementation.
 *
 * This logger is really primitive.  It just sinks everything below error.
 * Error and fatal are send to stderr.
 */
public class DefaultLogger extends AbstractLogger {

	/**
	 * Get an instance of DefaultLogger.
	 */
	public DefaultLogger(String name) {
		super(name);
	}

	/** 
	 * @see AbstractLogger
	 */
	protected void logAt(int level, Object message, Throwable e) {
		if(level >= INFO) {
			System.err.println(getName() + " (" + levelToString(level)
				+ "): " + message);
			if(e != null) {
				e.printStackTrace();
			}
		}
	}

}
