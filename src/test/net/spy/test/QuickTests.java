// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 2092F544-1110-11D9-9917-000A957659CC

package net.spy.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Run just the quick tests.
 */
public class QuickTests extends TestSuite {

	/**
	 * Get an instance of QuickTests.
	 */
	public QuickTests(String name) {
		super(name);
	}

	/**
	 * Get this test suite.
	 */
	public static Test suite() {
		TestSuite rv=new TestSuite();
		rv.addTest(CacheTest.suite());
		rv.addTest(DiskCacheTest.suite());
		rv.addTest(DBTest.suite());
		rv.addTest(RingBufferTest.suite());
		rv.addTest(PromiseTest.suite());
		rv.addTest(DigestTest.suite());
		rv.addTest(URLWatcherTest.suite());
		rv.addTest(PKTest.suite());
		rv.addTest(WeakHashSetTest.suite());
		rv.addTest(ShortestPathTest.suite());
		rv.addTest(LimitedListTest.suite());
		rv.addTest(LRUTest.suite());
		rv.addTest(IdentityEqualifierTest.suite());
		rv.addTest(NetStringTest.suite());
		rv.addTest(RangeTest.suite());
		rv.addTest(BitArrayTest.suite());
		rv.addTest(FactoryTest.suite());
		return(rv);
	}

	/**
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

}
