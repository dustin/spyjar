/*
 * Copyright (c) 2002  Dustin Sallings
 *
 * arch-tag: 0CE1BEDE-1110-11D9-A32D-000A957659CC
 */

package net.spy.test;

import java.lang.ref.SoftReference;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.spy.cache.SpyCache;
import net.spy.cache.CacheDelegate;
import net.spy.cache.Cachable;

/**
 * Test the cache system.
 */
public class CacheTest extends TestCase {

	// The ever-increasing value that will be stored in the cache.
	private int val=0;

	private SpyCache cache=null;

	/** 
	 * Get the cache.
	 */
	protected void setUp() {
		cache=SpyCache.getInstance();
	}

	/** 
	 * Get rid of the cache.
	 */
	protected void tearDown() {
		SpyCache.shutdown();
	}

	/** 
	 * Test singleton methods.
	 */
	public void testSingleton() {
		assertSame(SpyCache.getInstance(), SpyCache.getInstance());
		SpyCache.shutdown();
		assertNotSame(cache, SpyCache.getInstance());
		// Test a double shutdown (triple with the fixture)
		SpyCache.shutdown();
		SpyCache.shutdown();
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
		cache.store(key, new SoftReference(i), 250);

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

	public void testClearing() {
		String key="testKey";
		assertNull("Shouldn't have a value for " + key, cache.get(key));

		Object d=new java.util.Date();
		cache.store(key, d, 1000);
		assertSame(d, cache.get(key));
		cache.uncache(key);
		assertNull(cache.get(key));
	}

	public void testDelegate() {
		try {
			cache.setDelegate(null);
		} catch(NullPointerException e) {
			assertEquals("Invalid delegate <null>", e.getMessage());
		}
		TestDelegate td=new TestDelegate();
		cache.setDelegate(td);

		assertEquals(0, td.cached);
		assertEquals(0, td.uncached);

		String key="testKey";
		assertNull("Shouldn't have a value for " + key, cache.get(key));

		Object d=new java.util.Date();
		cache.store(key, d, 1000);
		assertEquals(1, td.cached);

		cache.uncache(key);
		assertEquals(1, td.uncached);

		cache.store("k1", "test1", 1000);
		cache.store("k2", "test2", 1000);
		assertEquals(3, td.cached);

		cache.uncacheLike("");
		assertEquals(3, td.uncached);

		// Put something we've already seen back in there
		cache.store("k1", "test1", 1000);
		assertEquals(4, td.cached);
	}

	private static class TestDelegate implements CacheDelegate  {
		public int cached=0;
		public int uncached=0;
		public void cachedObject(String key, Cachable value) {
			cached++;
			String.valueOf(value);
		}
		public void uncachedObject(String key, Cachable value) {		
			uncached++;
		}
	}
}
