/*
 * Copyright (c) 2002  Dustin Sallings
 *
 * $Id: DiskCacheTest.java,v 1.4 2002/12/19 07:12:45 dustin Exp $
 */

package net.spy.test;

import java.io.File;
import java.io.IOException;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.spy.cache.DiskCache;
import net.spy.util.Digest;
import net.spy.SpyUtil;
import net.spy.util.PwGen;

/**
 * Test the cache system.
 */
public class DiskCacheTest extends TestCase {

	// The ever-increasing value that will be stored in the cache.
	private int val=0;

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
			PwGen pw=new PwGen();
			tmpdir="/tmp/sjju-" + pw.getPass(16);
		}
		return(tmpdir);
	}

	/** 
	 * Get the cache.
	 */
	protected void setUp() {
		String tmpdir=getTmpDir();
		cache=new DiskCache(tmpdir);
	}

	private void rmdashrf(File tmp) {
		// System.err.println("rmdashrf " + tmp);
		File f[]=tmp.listFiles();
		if(f!=null) {
			for(int i=0; i<f.length; i++) {
				if(f[i].isDirectory()) {
					rmdashrf(f[i]);
				} else {
					f[i].delete();
				}
			}
		}
		// Remove the dir itself
		tmp.delete();
	}

	/** 
	 * Get rid of the cache.
	 */
	protected void tearDown() {
		cache = null;
		rmdashrf(new File(tmpdir));
		tmpdir=null;
	}

	/** 
	 * Test the basic functionality used for testing.
	 */
	public void testRmDashRf() throws IOException {
		File f=new File(tmpdir + "/a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/");
		f.mkdirs();
	}

	private Map initCache() throws IOException {
		PwGen gen=new PwGen();
		HashMap pairs=new HashMap();

		for(int i=0; i<1000; i++) {
			String key=gen.getPass(8);
			String value=gen.getPass(8);
			pairs.put(key, value);
			cache.storeObject(key, value);
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
			assertEquals(me.getValue(), cache.getObject((String)me.getKey()));
			// Second time, use the new API, 'cuz why not?  :)
			assertEquals(me.getValue(), cache.get(me.getKey()));
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
		for(Iterator i=cache.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry me=(Map.Entry)i.next();

			// Make sure we haven't seen this record before
			assertTrue("Already seen " + me.getKey() + " at " + n,
				(!s.contains(me.getKey())));
			s.add(me.getKey());

			assertEquals(me.getValue(), pairs.get(me.getKey()));

			// Make we don't run over
			assertTrue("Too many entries!", n<pairs.size());
			n++;
		}

		assertEquals("pairs.size() != cache.size()",
			pairs.size(), cache.size());
	}

	/** 
	 * Test cache walking.
	 */
	public void testCacheWalkingWithRemoval() throws Exception {
		Map pairs=initCache();

		// Now, walk the cache, removing every other entry
		Set cacheSet=cache.entrySet();
		int origsize=cache.size();
		int newsize=origsize;
		int n=0;
		for(Iterator i=cacheSet.iterator(); i.hasNext(); ) {
			Map.Entry me=(Map.Entry)i.next();

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
