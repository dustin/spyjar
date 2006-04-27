// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: D2ED63F5-18BB-4FAA-BB2C-7B55EBDB1D14

package net.spy.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.spy.SpyObject;

/**
 * Object that will wait until a condition becomes true.
 */
public class SynchronizationObject<T> extends SpyObject {

	private Lock lock=null;
	private Condition changeCondition=null;
	private T theObject=null;

	/**
	 * Construct a synchronization object on the given object.
	 */
	public SynchronizationObject(T o) {
		super();
		theObject=o;
		lock=new ReentrantLock();
		changeCondition=lock.newCondition();
	}

	/**
	 * Get the current value of this lock.
	 */
	public Object get() {
		lock.lock();
		try {
			return theObject;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Set a new value and signal anyone listening for a value change.
	 */
	public void set(T o) {
		lock.lock();
		try {
			theObject=o;
			changeCondition.signalAll();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * String this SynchronizationObject.
	 */
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
	public void waitUntilTrue(Predicate<T> p, long timeout, TimeUnit timeunit)
		throws InterruptedException, TimeoutException {

		assert p != null : "Null predicate";
		assert timeout >= 0 : "Invalid timeout";

		lock.lock();
		try {
			long now=System.currentTimeMillis();
			long theEnd=now + timeout;
			while(!p.evaluate(theObject)) {
				if(now >= theEnd) {
					throw new TimeoutException();
				}
				changeCondition.await(theEnd - now, timeunit);
				now=System.currentTimeMillis();
			}
		} finally {
			lock.unlock();
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
