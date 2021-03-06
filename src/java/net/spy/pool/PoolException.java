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

	/**
	 * Get a PoolException instance with a root cause.
	 */
	public PoolException(String msg, Throwable t) {
		super(msg, t);
	}

}
