// Copyright (c) 2002  SPY internetworking <dustin@spy.net>
//
// $Id: LoggerFactory.java,v 1.1 2002/11/04 21:13:57 dustin Exp $

package net.spy.log;

/**
 * Factory to get logger instances.
 * Currently, this just gets an instance of DefaultLogger and moves along.
 */
public class LoggerFactory extends Object {

	/**
	 * Get an instance of LoggerFactory.
	 */
	private LoggerFactory() {
		super();
	}

	/** 
	 * Get a logger by class.
	 * 
	 * @param clazz the class for which we want the logger.
	 * @return a Logger instance
	 */
	public static Logger getLogger(Class clazz) {
		return(getLogger(clazz.getName()));
	}

	/** 
	 * Get a logger by name.
	 * 
	 * @param name the name for which we want the logger
	 * @return a Logger instance
	 */
	public static Logger getLogger(String name) {
		return(new DefaultLogger(name));
	}

}
