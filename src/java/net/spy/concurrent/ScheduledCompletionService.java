// Copyright (c) 2007  Dustin Sallings <dustin@spy.net>
package net.spy.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Extended completion service allowing future tasks to also be tracked.
 */
public interface ScheduledCompletionService<V> extends CompletionService<V> {

	/**
	 * Schedule a callable to be run in the future.
	 *
	 * @param c the callable
	 * @param delay how long to wait
	 * @param unit time unit for the delay
	 * @return the future to track the result
	 */
	Future<V> schedule(Callable<V> c, long delay, TimeUnit unit);

	/**
	 * Schedule a runnable to be run in the future.
	 *
	 * @param r the runnable
	 * @param delay how long to wait
	 * @param unit time unit for the delay
	 * @return the future to track the result
	 */
	Future<?> schedule(Runnable r, long delay, TimeUnit unit);

}
