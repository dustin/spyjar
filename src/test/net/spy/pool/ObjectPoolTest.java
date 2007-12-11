// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>

package net.spy.pool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import junit.framework.TestCase;
import net.spy.util.SpyConfig;

/**
 * Object pool testing.
 */
public class ObjectPoolTest extends TestCase {

	/**
	 * Get an instance of ObjectPoolTest.
	 */
	public ObjectPoolTest(String name) {
		super(name);
	}

	/**
	 * Basic object pool test.
	 */
	@SuppressWarnings("unchecked")
	public void testBasicObjectPool() throws Exception {
		SpyConfig conf=new SpyConfig();
		conf.put("test.min", "1");
		conf.put("test.start", "1");
		conf.put("test.yellow", "90");
		conf.put("test.max", "10");

		// Create the pool
		ObjectPool op=new ObjectPool(conf);
		op.createPool("test", new PlainObjectFiller("test", conf));
		assertTrue(op.hasPool("test"));
		assertEquals(1, op.numPools());
		// Just to exercise the code.
		String.valueOf(op);

		Collection<PooledObject> pooledObs=new ArrayList<PooledObject>(10);
		for(int i=0; i<10; i++) {
			pooledObs.add(op.getObject("test"));
		}
		assertEquals(10, pooledObs.size());
		try {
			PooledObject po=op.getObject("test");
			fail("Pool gave me an object when it should be empty:  " + po);
		} catch(PoolException e) {
			// pass
		}
		// Return them all to the pool
		Collection<Object> objectsFromPool=new TreeSet<Object>();
		for(Iterator i=pooledObs.iterator(); i.hasNext();) {
			PooledObject po=(PooledObject)i.next();
			objectsFromPool.add(po.getObject());
			po.checkIn();
			i.remove();
		}
		/*
		// Try this again
		Collection pooledObs2=new ArrayList(10);
		for(int i=0; i<10; i++) {
			pooledObs2.add(op.getObject("test"));
		}
		Collection objectsFromPool2=new TreeSet();
		for(Iterator i=pooledObs2.iterator(); i.hasNext();) {
			PooledObject po=(PooledObject)i.next();
			objectsFromPool2.add(po.getObject());
			po.checkIn();
			i.remove();
		}
		assertEquals(objectsFromPool, objectsFromPool2);
		*/

		op.prune();
	}

	private static final class PlainObjectFiller extends PoolFiller {
		private int id=0;
		public PlainObjectFiller(String nm, SpyConfig cnf) {
			super(nm, cnf);
		}
		@Override
		public PoolAble getObject() throws PoolException {
			return(new PlainPoolAble(new Integer(id++), getPoolHash()));
		}
	}

	private static final class PlainPoolAble extends PoolAble {
		public PlainPoolAble(Object o, int h) {
			super(o, h);
		}
	}

}
