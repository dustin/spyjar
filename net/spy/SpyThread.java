// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// $Id: SpyThread.java,v 1.1 2002/11/20 04:52:32 dustin Exp $

package net.spy;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;

/**
 * Superclass for all Spy Threads.
 */
public class SpyThread extends Thread {

	private Logger logger=null;

	// Thread has *eight* constructors.  Damnit.

	/**
	 * Get an instance of SpyThread.
	 */
	public SpyThread() {
		super();
	}

	/** 
	 * Get an instance of SpyThread with a name.
	 * 
	 * @param name thread name
	 */
	public SpyThread(String name) {
		super(name);
	}

	/** 
	 * Get an instance of SpyThread with a name.
	 * 
	 * @param t the threadgroup in which this Thread will survive
	 * @param name thread name
	 */
	public SpyThread(ThreadGroup t, String name) {
		super(t, name);
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
