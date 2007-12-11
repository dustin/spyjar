// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.cache;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

import junit.framework.TestCase;
import net.spy.test.SyncThread;

/**
 * Test the simple cache.
 */
public class SimpleCacheTest extends TestCase {

	// The ever-increasing value that will be stored in the cache.
	private int val=0;
	private SimpleCache cache=null;

	/**
	 * Get the cache.
	 */
	@Override
	protected void setUp() {
		cache=SimpleCache.getInstance();
	}

	/**
	 * Get rid of the cache.
	 */
	@Override
	protected void tearDown() {
		SimpleCache.setInstance(null);
	}

	/**
	 * Test singleton methods.
	 */
	public void testSingleton() throws Throwable {
		SimpleCache.setInstance(null);
		int n=SyncThread.getDistinctResultCount(50, new Callable<Object>() {
			public Object call() throws Exception {
				return SimpleCache.getInstance();
			}});
		assertEquals(1, n);
	}

	public void testBasicCaching() throws InterruptedException {
		String key="testInt";

		Integer i=(Integer)cache.get(key);
		assertNull("Shouldn't be a value for " + key + " yet", i);

		// OK, now store it
		i=new Integer(++val);
		cache.store(key, i, 1000);

		// Check again immediately
		i=(Integer)cache.get(key);
		assertNotNull("Didn't get value for " + key, i);
		int tmp=i.intValue();
		assertEquals("Incorrect value returned from cache.", tmp, val);

		// Make sure enough time has passed
		Thread.sleep(1200);

		// Make sure we *don't* get the object from the cache
		i=(Integer)cache.get(key);
		assertNull(key + " should have expired by now", i);
	}

	public void testReferenceCaching() throws InterruptedException {
		String key="testInt";

		Integer i=(Integer)cache.get(key);
		assertNull("Shouldn't be a value for " + key + " yet", i);

		// OK, now store it
		i=new Integer(++val);
		cache.store(key, new SoftReference<Integer>(i), 250);

		// Check again immediately
		i=(Integer)cache.get(key);
		assertNotNull("Didn't get value for " + key, i);
		int tmp=i.intValue();
		assertEquals("Incorrect value returned from cache.", tmp, val);

		// Make sure enough time has passed
		Thread.sleep(300);

		// Make sure we *don't* get the object from the cache
		i=(Integer)cache.get(key);
		assertNull(key + " should have expired by now", i);
	}

	public void testReferenceExpiration() throws Exception {
		WeakReference<Integer> ref=new WeakReference<Integer>(1);
		cache.store("x", ref, 1000);
		assertEquals(new Integer(1), cache.get("x"));
		ref.clear();
		assertNull(cache.get("x"));
	}

	public void testClearing() {
		String key="testKey";
		assertNull("Shouldn't have a value for " + key, cache.get(key));

		Object d=new java.util.Date();
		cache.store(key, d, 1000);
		assertSame(d, cache.get(key));
		cache.remove(key);
		assertNull(cache.get(key));
	}

}
