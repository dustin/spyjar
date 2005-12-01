/*
 * Copyright (c) 2002  Dustin Sallings
 *
 * arch-tag: 1173AE26-1110-11D9-A3AF-000A957659CC
 */

package net.spy.test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.spy.cache.DiskCache;
import net.spy.util.PwGen;
import net.spy.util.SpyUtil;

/**
 * Test the cache system.
 */
public class DiskCacheTest extends TestCase {

	private DiskCache cache=null;

	private String tmpdir=null;

    /**
     * Get an instance of DiskCacheTest.
     */
    public DiskCacheTest(String name) {
        super(name);
    }

	/**
	 * Get this test suite.
	 */
	public static Test suite() {
		return new TestSuite(DiskCacheTest.class);
	}

	/**
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	private String getTmpDir() {
		if(tmpdir==null) {
			tmpdir="/tmp/sjju-" + PwGen.getPass(16);
		}
		return(tmpdir);
	}

	/** 
	 * Get the cache.
	 */
	protected void setUp() throws Exception {
		cache=new DiskCache(getTmpDir());
	}

	/** 
	 * Get rid of the cache.
	 */
	protected void tearDown() throws Exception {
		cache = null;
		SpyUtil.rmDashR(new File(tmpdir));
		tmpdir=null;
	}

	/** 
	 * Check getBaseDir.
	 */
	public void testGetBaseDir() {
		assertEquals(getTmpDir(), cache.getBaseDir());
	}

	/** 
	 * Test the basic functionality used for testing.
	 */
	public void testRmDashRf() throws IOException {
		File f=new File(tmpdir + "/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/");
		f.mkdirs();
	}

	private Map initCache() throws IOException {
		HashMap pairs=new HashMap();

		for(int i=0; i<100; i++) {
			String key=PwGen.getPass(8);
			String value=PwGen.getPass(8);
			pairs.put(key, value);
			cache.put(key, value);
		}

		return(pairs);
	}

	/** 
	 * Test basic functionality of the disk cache.
	 */
	public void testBasicDiskCache() throws Exception {
		Map pairs=initCache();

		for(Iterator i=pairs.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry me=(Map.Entry)i.next();

			// Try it twice (test the LRU)
			assertEquals(me.getValue(), cache.get(me.getKey()));
			assertEquals(me.getValue(), cache.get(me.getKey()));
		}
	}

	/** 
	 * Test storing keys that aren't strings.
	 */
	public void testNonStringThings() {
		for(int i=0; i<10; i++) {
			cache.put(new Integer(i), String.valueOf(i));
		}
		for(int i=0; i<10; i++) {
			String istr=(String)cache.get(new Integer(i));
			assertEquals(i, Integer.parseInt(istr));
		}
	}

	/** 
	 * Test invalid storage.
	 */
	public void testInvalidStore() {
		try {
			Object blah=cache.get(null);
			fail("Asked for null, got " + blah);
		} catch(NullPointerException e) {
			assertNotNull(e.getMessage());
		}
	}

	/** 
	 * Test basic functionality of the disk cache.
	 */
	public void testBasicDiskCacheNew() throws Exception {
		Map pairs=initCache();

		for(Iterator i=pairs.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry me=(Map.Entry)i.next();

			assertEquals(me.getValue(), cache.get(me.getKey()));
		}
	}

	/** 
	 * Test cache walking.
	 */
	public void testCacheWalking() throws Exception {
		Map pairs=initCache();

		// First, walk the map
		for(Iterator i=pairs.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry me=(Map.Entry)i.next();

			assertEquals(me.getValue(), cache.get(me.getKey()));
		}

		// Now, walk the cache
		int n=0;
		Set s=new HashSet();
		Map.Entry lastEntry=null;
		for(Iterator i=cache.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry me=(Map.Entry)i.next();

			// Make sure we haven't seen this record before
			assertTrue("Already seen " + me.getKey() + " at " + n,
				(!s.contains(me.getKey())));
			s.add(me.getKey());

			assertEquals(me.getValue(), pairs.get(me.getKey()));
			assertEquals(me, me);
			assertFalse(me + " shouldn't equal " + lastEntry,
				me.equals(lastEntry));
			assertFalse(me + " shouldn't equal x", me.equals("x"));

			// We shouldn't be allowed to store stuff.
			try {
				me.setValue("Test");
				fail("Entry let me set its value.");
			} catch(UnsupportedOperationException e) {
				// pass
			}

			// Make we don't run over
			assertTrue("Too many entries!", n<pairs.size());
			n++;

			lastEntry=me;
		}

		assertEquals("pairs.size() != cache.size()",
			pairs.size(), cache.size());
	}

	/** 
	 * Test cache walking.
	 */
	public void testCacheWalkingWithRemoval() throws Exception {
		initCache();

		// Now, walk the cache, removing every other entry
		Set cacheSet=cache.entrySet();
		int origsize=cache.size();
		int newsize=origsize;
		int n=0;

		try {
			cacheSet.iterator().remove();
			fail("Let me remove stuff before we started");
		} catch(IllegalStateException e) {
			// pass
		}

		for(Iterator i=cacheSet.iterator(); i.hasNext(); ) {
			i.next();

			// Remove every other entry.
			if(n%2 == 0) {
				i.remove();
				newsize--;
			}

			// Make we don't run over
			assertTrue("Removing more entries than we started with", newsize>0);
			n++;
		}

		assertTrue("Sizes didn't change", newsize<origsize);
		assertEquals("Incorrect new size in Set", newsize, cacheSet.size());
		cacheSet=cache.entrySet();
		assertEquals("Incorrect new size in Cache", newsize, cacheSet.size());

	}


}
