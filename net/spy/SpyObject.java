// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// $Id: SpyObject.java,v 1.1 2002/11/20 04:32:06 dustin Exp $

package net.spy;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;

/**
 * Superclass for all Spy Objects.
 */
public class SpyObject extends Object {

	private Logger logger=null;

	/**
	 * Get an instance of SpyObject.
	 */
	public SpyObject() {
		super();
	}

	/** 
	 * Get a Logger instance for this class.
	 * 
	 * @return an appropriate logger instance.
	 */
	protected Logger getLogger() {
		if(logger==null) {
			logger=LoggerFactory.getLogger(getClass());
		}
		return(logger);
	}

}
