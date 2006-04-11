// Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 7B3A3A10-1110-11D9-B529-000A957659CC

package net.spy.pool;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import net.spy.SpyObject;
import net.spy.util.SpyConfig;
import net.spy.util.TimeStampedHashMap;

/**
 * ObjectPool is the entry point for all object pooling facilities in
 * net.spy.pool.*.  ObjectPools have a shared reference to a pool, so there
 * is exactly one set of pools per ClassLoader.  This can be very useful in
 * consolidating applications' pools into one.
 * <p>
 * Pools are referenced by name, so as long as two pools have two different
 * names, they will be used independently.
 * <p>
 * When creating a pool, you must have a PoolFiller that will populate the
 * pool with objects when it needs them.
 * <p>
 * The following is an example demonstrating how to instantiate a JDBC pool
 * using JDBCPoolFiller:
 * <pre>
 * SpyConfig conf=new SpyConfig("pool.conf");
 * ObjectPool op=new ObjectPool(conf);
 * JDBCPoolFiller pf=new JDBCPoolFiller("db", conf);
 * op.createPool("db", pf);
 * </pre>
 */

public class ObjectPool extends SpyObject {

	// toString buffer length
	private static final int TOSTRING_LEN=256;

	// Number of cleans to run
	private static final int NUM_CLEANS=6;
	private static final int TIME_BETWEEN_CLEANS=300000;

	// This is static so we can check up on it.
	private static ObjectPoolCleaner cleaner=null;
	// This is static because we want everyone to see the same pools, of
	// course.
	private static TimeStampedHashMap<String, PoolContainer> pools=null;

	public ObjectPool(SpyConfig conf) {
		super();

		initialize();
	}

	/**
	 * Create a new object pool.
	 *
	 * @param name The name of the object pool.
	 * @param pf The PoolFiller object that will be used to create new
	 * objects within the pool.
	 *
	 * @exception PoolException when bad things happen
	 */
	public void createPool(String name, PoolFiller pf)
		throws PoolException {

		synchronized(pools) {
			// Make sure we don't already have a this pool
			if(hasPool(name)) {
				throw new PoolException("There's already a pool called "
					+ name);
			}

			// Grab a PoolContainer
			PoolContainer pc=new PoolContainer(name, pf);

			// add it to our pool list
			pools.put(name, pc);
		}
	}

	/**
	 * Destory a pool.
	 *
	 * @param name The pool to destroy.
	 * @exception PoolException if there's a problem removing the pool
	 * @exception NoSuchPoolException if the pool we want to remove doesn't
	 * 				exist
	 */
	public void destroyPool(String name) throws PoolException {
		synchronized (pools) {
			getPool(name);
			pools.remove(name);
		}
	}

	/**
	 * Find out if the ObjectPool contains the named pool.
	 *
	 * @param name the name of the pool we're looking for
	 */
	public boolean hasPool(String name) {
		boolean ret=false;
		synchronized (pools) {
			ret=pools.containsKey(name);
		}
		return(ret);
	}

	/**
	 * Get an object from a pool.
	 *
	 * @param name The pool from which we'll get our object.
	 *
	 * @return a PooledObject object.
	 *
	 * @exception PoolException if it can't get an object
	 * @exception NoSuchPoolException if there isn't a pool by that name
	 */
	public PooledObject getObject(String name) throws PoolException {
		PooledObject ret=null;
		PoolContainer pc=null;
		checkCleaner();
		synchronized (pools) {
			pc=getPool(name);
		}
		ret=pc.getObject();
		return(ret);
	}

	/**
	 * Get a count of the number of object pools.
	 */
	public int numPools() {
		int rv=0;
		synchronized (pools) {
			rv=pools.size();
		}
		return(rv);
	}

	/**
	 * Dump out the object pools.
	 */
	public String toString() {
		StringBuilder out=new StringBuilder(TOSTRING_LEN);
		ArrayList<PoolContainer> a=new ArrayList<PoolContainer>();
		synchronized (pools) {
			for(PoolContainer pc : pools.values()) {
				a.add(pc);
			}
		}
		// This is broken out to get out of the lock fast...
		for(PoolContainer pc : a) {
			out.append(pc);
		}
		return(out.toString());
	}

	/**
	 * Prune the object pools.  This method requests that each individual
	 * pool prune itself, removing unusable or unnecessary PoolAbles.
	 *
	 * @exception PoolException if something bad happens
	 */
	public void prune() throws PoolException {
		ArrayList<PoolContainer> a=new ArrayList<PoolContainer>(pools.size());
		// Clean up any pools that are empty
		synchronized (pools) {
			for(Iterator i=pools.values().iterator(); i.hasNext();) {
				PoolContainer pc=(PoolContainer)i.next();

				// If it's empty, remove it.
				if(pc.totalObjects()==0) {
					// Remove the pool from our collection of pools
					i.remove();
				} else {
					a.add(pc);
				}
			}
		}
		// A second loop (out of the synchronized block) to ask each individual
		// pool to clean itself.
		for(PoolContainer pc : a) {
			pc.prune();
		}
	}

	private static synchronized PoolContainer getPool(String name)
		throws PoolException {

		PoolContainer ret=null;

		synchronized (pools) {
			ret=pools.get(name);
			if(ret==null) {
				throw new NoSuchPoolException(name);
			}
		}
		return(ret);
	}

	private void initialize() {
		// Do we have a pool?
		synchronized(ObjectPool.class) {
			if(pools==null) {
				pools=new TimeStampedHashMap<String, PoolContainer>();
			}
		}

		checkCleaner();
	}

	// Make sure the cleaner is doing its job.
	private void checkCleaner() {
		synchronized(ObjectPool.class) {
			if(cleaner==null || (!cleaner.isAlive())) {
				cleaner=new ObjectPoolCleaner(this);
			}
		}
	}

	// This is a private class that keeps the pool clean.
	private class ObjectPoolCleaner extends Thread {

		// The object pool reference we'll be cleaning.
		private ObjectPool op=null;

		// How many times we've cleaned so far.
		private int numCleans=0;

		// Last time we cleaned.
		private Date lastClean=null;

		// Create (and start) the ObjectPoolCleaner.
		public ObjectPoolCleaner(ObjectPool o) {
			super();
			this.op=o;
			setDaemon(true);
			setName("ObjectPoolCleaner");
			start();
		}

		// Look like a normal thread, but report number of times the thing's
		// cleaned.
		public String toString() {
			StringBuilder sb=new StringBuilder(TOSTRING_LEN);
			sb.append(super.toString());
			sb.append(" - ");
			sb.append(numCleans);
			sb.append(" served");

			if(lastClean!=null) {
				sb.append(".  Most recent cleaning:  ");
				sb.append(lastClean);
			}

			return(sb.toString());
		}

		private void doPrune() throws Exception {
			if(getLogger().isDebugEnabled()) {
				getLogger().debug("Cleaning pool:  " + op);
			}
			op.prune();
			numCleans++;
			getLogger().debug("Finished cleaning, looks like this:  %s", op);
		}

		public void run() {
			// Only do six cleans (sleeping ten minutes, that's an hour!)
			while(numCleans<NUM_CLEANS) {
				try {
					// Prune every once in a while.
					sleep(TIME_BETWEEN_CLEANS);
					lastClean=new Date();
					doPrune();
				} catch(Exception e) {
					getLogger().error("Cleaner got an exception", e);
				}
			}
		}
	} // ObjectPoolCleaner

} // ObjectPool
