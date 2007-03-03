// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: ABCA8432-B592-4692-8B10-0511F8B8E50E

package net.spy.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

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
	 * Method called whenever an execution exception occurs while running the
	 * call() method.  This is a good place to count failures and adjust the
	 * value to be returned by getRetryDelay().
	 *
	 * @param exception exception that occured
	 */
	void onExecutionException(ExecutionException exception);

	/**
	 * Invoked when the execution of a retryable is complete.
	 * 
	 * @param successful if true, the execution was successful
	 */
	void onComplete(boolean successful);
}
