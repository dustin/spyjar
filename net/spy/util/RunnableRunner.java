// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: RunnableRunner.java,v 1.2 2003/08/05 09:01:05 dustin Exp $

package net.spy.util;

/**
 * Interface for objects that run runnables.
 */
public interface RunnableRunner {

	/** 
	 * Run the supplied Runnable.
	 */
	void run(Runnable r);

}
