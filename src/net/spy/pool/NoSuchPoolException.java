// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 7AC16146-1110-11D9-886B-000A957659CC

package net.spy.pool;

/**
 * Exception thrown when there's NoSuchPool.
 */
public class NoSuchPoolException extends PoolException {

	private String poolName=null;

	/**
	 * Get an instance of NoSuchPoolException.
	 */
	public NoSuchPoolException(String poolName) {
		super("There's no pool called " + poolName);

		this.poolName=poolName;
	}

	/**
	 * Get the name of the pool that is missing.
	 */
	public String getPoolName() {
		return(poolName);
	}

}
