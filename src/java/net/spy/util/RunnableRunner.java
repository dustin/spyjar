// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>

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
