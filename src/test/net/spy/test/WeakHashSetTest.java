// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 2AA64DDF-1110-11D9-B77E-000A957659CC

package net.spy.test;

import java.util.Set;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.spy.util.WeakHashSet;

/**
 * Test a weak hash set.
 */
public class WeakHashSetTest extends TestCase {

	/**
	 * Get an instance of WeakHashSetTest.
	 */
	public WeakHashSetTest(String name) {
		super(name);
	}

	/** 
	 * Get the suite.
	 */
	public static Test suite() {
		return new TestSuite(WeakHashSetTest.class);
	}

	/** 
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	/** 
	 * Test the basic functionality of the weak hash set.
	 */
	public void testBasicWeakHash() {
		Set<String> s=new java.util.HashSet();
		for(int i=0; i<1000; i++) {
			s.add("Blah" + i);
		}

		WeakHashSet<String> whs=new WeakHashSet(s);

		assertEquals("Size didn't match", whs.size(), s.size());

		// Remove some items.
		int i=0;
		for(Iterator it=s.iterator(); it.hasNext();) {
			it.next();
			if( (i++%2) == 0) {
				it.remove();
			}
		}

		// Do a garbage collection.
		System.gc();

		// Recompare the sizes.
		assertEquals("Size didn't match after removals", whs.size(), s.size());
	}

}
