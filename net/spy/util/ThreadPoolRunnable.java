// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// $Id: ThreadPoolRunnable.java,v 1.1 2002/12/06 08:58:42 dustin Exp $

package net.spy.util;

/**
 * Interface that flags a class as having overridden toString() for debug
 * display in a thread list.
 */
public interface ThreadPoolRunnable extends Runnable {

	/** 
	 * Subclasses must override toString() to produce an informative debug
	 * string.
	 */
	String toString();

}
