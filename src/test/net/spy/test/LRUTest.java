// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 13FD9F8D-1110-11D9-A39E-000A957659CC

package net.spy.test;

import java.lang.ref.SoftReference;
import java.util.Collections;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.spy.cache.CacheListener;
import net.spy.cache.LRUCache;

/**
 * Test the LRU cache implementation
 */
public class LRUTest extends TestCase {

	/**
	 * Get an instance of LRUTest.
	 */
	public LRUTest(String name) {
		super(name);
	}

	/** 
	 * Get this test suite.
	 */
	public static Test suite() {
		return new TestSuite(LRUTest.class);
	}

	/** 
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	/** 
	 * Perform basic LRU testing.
	 */
	public void testBasicLRU() {
		Integer zero=new Integer(0);
		LRUCache<Integer, Integer> cache=new LRUCache<Integer, Integer>(10);

		assertNull("Zero shouldn't be there.", cache.get(zero));

		// Keep this value up-to-date
		cache.put(zero, zero);
		assertNotNull("Zero should be there.", cache.get(zero));

		for(int i=1; i<100; i++) {
			assertTrue("Cache size exceeded valid size", cache.size() <= 10);
			Integer ii=new Integer(i);
			assertNull(cache.get(ii));
			cache.put(ii, ii);
			assertNotNull(cache.get(zero));
			assertNotNull(cache.get(ii));

			if(i>10) {
				assertNull(cache.get(new Integer(i-10)));
			}
		}
	}

	/** 
	 * Perform reference LRU testing.
	 */
	public void testReferenceLRU() {
		Integer zero=new Integer(0);
		LRUCache<Integer, SoftReference<Integer>> cache
			=new LRUCache<Integer, SoftReference<Integer>>(10);

		assertNull("Zero shouldn't be there.", cache.get(zero));

		// Keep this value up-to-date
		cache.put(zero, new SoftReference<Integer>(zero));
		assertNotNull("Zero should be there.", cache.get(zero));

		for(int i=1; i<100; i++) {
			assertTrue("Cache size exceeded valid size", cache.size() <= 10);
			Integer ii=new Integer(i);
			assertNull(cache.get(ii));
			cache.put(ii, new SoftReference<Integer>(ii));
			assertNotNull(cache.get(zero));
			assertNotNull(cache.get(ii));

			if(i>10) {
				assertNull(cache.get(new Integer(i-10)));
			}
		}
	}

	/** 
	 * Test cache listener.
	 */
	@SuppressWarnings("unchecked")
	public void testCacheListener() {
		LRUCache<Comparable, Object> cache
			=new LRUCache<Comparable, Object>(10);

		TestListener tl=new TestListener();
		cache.put("listener", tl);
		for(int i=1; i<100; i++) {
			Integer ii=new Integer(i);
			assertNull(cache.get(ii));
			cache.put(ii, ii);
			assertNotNull(cache.get(ii));
		}
		assertNull(cache.get("listener"));
		assertEquals(1, tl.cached);
		assertEquals(1, tl.uncached);
	}

	public void testCacheListenerPutAll() {
		LRUCache<String, TestListener> cache
			=new LRUCache<String, TestListener>(10);
		cache.putAll(Collections.singletonMap("listener", new TestListener()));
		TestListener tl=cache.get("listener");
		assertEquals(1, tl.cached);
	}

	private static final class TestListener extends Object
		implements CacheListener {

		public int cached=0;
		public int uncached=0;

		public TestListener() {
			super();
		}

		public void cachedEvent(Object key) {
			cached++;
		}

		public void uncachedEvent(Object key) {
			uncached++;
		}
	}

}
