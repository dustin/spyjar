//
// $Id: PoolException.java,v 1.1 2002/08/28 00:34:56 dustin Exp $

package net.spy.pool;

/**
 * Exception thrown when there's a problem dealing with the pool.
 */
public class PoolException extends Exception {
	/**
	 * Get a PoolException instance.
	 */
	public PoolException(String msg) {
		super(msg);
	}
}

