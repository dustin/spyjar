/*
 * Copyright (c) 2002  Dustin Sallings
 *
 * $Id: DiskCacheTest.java,v 1.1 2002/09/13 05:49:09 dustin Exp $
 */

package net.spy.test;

import java.io.File;
import java.io.IOException;

import java.util.Map;
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

	/** 
	 * Test basic functionality of the disk cache.
	 */
	public void testBasicDiskCache() throws Exception {
		PwGen gen=new PwGen();
		HashMap pairs=new HashMap();

		for(int i=0; i<1000; i++) {
			String key=gen.getPass(8);
			String value=gen.getPass(8);
			pairs.put(key, value);
			cache.storeObject(key, value);
		}

		for(Iterator i=pairs.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry me=(Map.Entry)i.next();

			assertEquals(me.getValue(), pairs.get(me.getKey()));
		}
	}

}
