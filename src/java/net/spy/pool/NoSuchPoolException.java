// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.pool;

/**
 * Exception thrown when there's NoSuchPool.
 */
public class NoSuchPoolException extends PoolException {

	private String poolName=null;

	/**
	 * Get an instance of NoSuchPoolException.
	 */
	public NoSuchPoolException(String nm) {
		super("There's no pool called " + nm);

		this.poolName=nm;
	}

	/**
	 * Get the name of the pool that is missing.
	 */
	public String getPoolName() {
		return(poolName);
	}

}
