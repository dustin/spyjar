// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Interface for callables that may need to be retried if unsuccessful.
 */
public interface RetryableCallable<V> extends Callable<V> {

	/**
	 * Special return value for getRetryDelay indicating no further retries
	 * should be attempted.
	 */
	static final int NO_MORE_RETRIES=-1;

	/**
	 * Get the number of milliseconds we should wait until the next retry.
	 * 
	 * @return ms to wait if positive, else stop retrying
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
	 * @param result if this execution wasn't successful, the result will be
	 *   a CompositeExecutorException itemizing the result
	 */
	void onComplete(boolean successful, Object result);
}
