// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.SpyObject;

/**
 * Object that will wait until a predicate determines that the value has been
 * set to a particular value.
 *
 * Note that the predicate is not guaranteed to see every value change.  It is
 * quite likely that changes will be missed when the value is changing rapidly.
 */
public class SynchronizationObject<T> extends SpyObject {

	private T theObject=null;

	/**
	 * Construct a synchronization object on the given object.
	 */
	public SynchronizationObject(T o) {
		super();
		theObject=o;
	}

	/**
	 * Get the current value of this lock.
	 */
	public synchronized T get() {
		return theObject;
	}

	/**
	 * Set a new value and signal anyone listening for a value change.
	 */
	public synchronized T set(T o) {
		T rv=theObject;
		theObject=o;
		notifyAll();
		return rv;
	}

	/**
	 * String this SynchronizationObject.
	 */
	@Override
	public String toString() {
		return "{SynchronizationObject obj=" + get() + "}";
	}

	/**
	 * Wait for the given predicate to become true in respect to the
	 * contained object.
	 *
	 * @param p the predicate
	 * @param timeout how long to wait for this condition to become true
	 * @param timeunit the time unit for the timeout
	 * @throws InterruptedException
	 * @throws TimeoutException if a timeout occurs before the condition
	 *         becomes true
	 */
	public synchronized void waitUntilTrue(Predicate<T> p,
		long timeout, TimeUnit timeunit)
		throws InterruptedException, TimeoutException {

		assert p != null : "Null predicate";
		assert timeout >= 0 : "Invalid timeout";

		long now=System.currentTimeMillis();
		long theEnd=now + timeunit.toMillis(timeout);
		// If theEnd is negative here, it's because we rolled over, likely
		// because the timeout was too far in the future.  Might as well make
		// it effectively infinite.
		if(theEnd < 0) {
			theEnd=Long.MAX_VALUE;
		}
		while(!p.evaluate(theObject)) {
			if(now >= theEnd) {
				throw new TimeoutException();
			}
			wait(timeunit.toMillis(theEnd - now));
			now=System.currentTimeMillis();
		}
	}

	/**
	 * Wait for the contained object to become non-null.
	 *
	 * @param timeout how long to wait for this condition to become true
	 * @param timeunit the time unit for the timeout
	 * @throws InterruptedException
	 * @throws TimeoutException if a timeout occurs before the condition
	 *         becomes true
	 */
	public void waitUntilNotNull(long timeout, TimeUnit timeunit)
		throws InterruptedException, TimeoutException {
		waitUntilTrue(new Predicate<T>() {
			public boolean evaluate(T o) { return o != null; }
			}, timeout, timeunit);
	}

	/**
	 * Wait for the contained object to become equal to the provided value.
	 *
	 * If the provided value is null, then this also waits for the contained
	 * object to become null.
	 *
	 * @param val the value to wait for
	 * @param timeout how long to wait for this condition to become true
	 * @param timeunit the time unit for the timeout
	 * @throws InterruptedException
	 * @throws TimeoutException if a timeout occurs before the condition
	 *         becomes true
	 */
	public void waitUntilEquals(final T val, long timeout, TimeUnit timeunit)
		throws InterruptedException, TimeoutException {

		waitUntilTrue(new Predicate<T>() {
			public boolean evaluate(T o) {
				return val == null ? o == null : val.equals(o);
			}
			}, timeout, timeunit);
	}

	/**
	 * Synchronization object predicate for evaluation in waitUntilTrue.
	 */
	public static interface Predicate<T> {

		/**
		 * Evaluate with the current object to determine whether the condition
		 * is true.
		 *
		 * @param o the current object
		 * @return whether the condition is met
		 */
		boolean evaluate(T o);
	}
}
