// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// $Id: ThreadPoolRunnable.java,v 1.2 2002/12/06 09:05:41 dustin Exp $

package net.spy.util;

/**
 * Interface that flags a class as having overridden toString() for debug
 * display in a thread list.
 *
 * <p>
 *  Runnables not implementing this interface will be displayed by their
 *  class name only.
 * </p>
 */
public interface ThreadPoolRunnable extends Runnable {

	/** 
	 * Subclasses must override toString() to produce an informative debug
	 * string.
	 */
	String toString();

}
