// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: ABCA8432-B592-4692-8B10-0511F8B8E50E

package net.spy.concurrent;

import java.util.concurrent.Callable;

/**
 * Interface for callables that may need to be retried if unsuccessful.
 */
public interface RetryableCallable<V> extends Callable<V> {

	/**
	 * Get the number of milliseconds we should wait until the next retry.
	 * 
	 * @return ms to wait, or -1 if no more retries should be attempted
	 */
	long getRetryDelay();

	/**
	 * Method called before scheduling this callable for retry.
	 */
	void retrying();

	/**
	 * Method called indicating we've given up on retry attemps for this
	 * callable.
	 */
	void givingUp();
}
