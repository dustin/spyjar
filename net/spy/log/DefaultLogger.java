// Copyright (c) 2002  SPY internetworking <dustin@spy.net>
//
// $Id: DefaultLogger.java,v 1.3 2002/11/05 07:36:19 dustin Exp $

package net.spy.log;

/**
 * Default logger implementation.
 *
 * This logger is really primitive.  It just logs everything to stderr if
 * it's higher than INFO.
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
		if(level > INFO) {
			System.err.println(getName() + " (" + levelToString(level)
				+ "): " + message);
			if(e != null) {
				e.printStackTrace();
			}
		}
	}

}
