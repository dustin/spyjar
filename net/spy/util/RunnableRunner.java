// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: RunnableRunner.java,v 1.1 2003/03/28 07:30:54 dustin Exp $

package net.spy.util;

/**
 * Interface for objects that run runnables.
 */
public interface RunnableRunner {

	/** 
	 * Run the supplied Runnable.
	 */
	public void run(Runnable r);

}
