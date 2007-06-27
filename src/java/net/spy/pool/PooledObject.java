package net.spy.pool;

import net.spy.SpyObject;

/**
 * Pooled object return package.  This object primarily exists as a means
 * for having a safe way to have access to PoolAble objects.  Anyone using
 * a PooledObject will not be able to retrieve the object that was pooled
 * after checking it back in, and it makes it safe to forget to check an
 * object back in on occasion.
 */
public class PooledObject extends SpyObject {

	private PoolAble poolAble=null;

	/**
	 * Get a new PooledObject containing the given PoolAble
	 */
	public PooledObject(PoolAble p) {
		super();
		this.poolAble=p;
		poolAble.checkOut();
	}

	/**
	 * Get the object we just checked out.
	 *
	 * @exception PoolException if a problem occurs
	 */
	public Object getObject() throws PoolException {
		return(poolAble.getObject());
	}

	/**
	 * Find out if the object is alive
	 *
	 * @return true if the object is alive
	 */
	public boolean isAlive() {
		return(poolAble.isAlive());
	}

	/**
	 * Manually check the object back in.
	 */
	public void checkIn() {
		poolAble.checkIn();
		poolAble=null;
	}

	/**
	 * Get the objectID for the pool object we have checked out.
	 *
	 * @return the object ID
	 */
	public int getObjectID() {
		return(poolAble.getObjectID());
	}

	/**
	 * Finalization will check-in any checked-out object that has not
	 * already been checked in.
	 */
	@Override
	protected void finalize() {
		if(poolAble!=null) {
			if(getLogger().isDebugEnabled()) {
				getLogger().debug("Finalization checking in object %s",
					poolAble.getObjectID());
			}
			poolAble.checkIn();
		}
	}

}
