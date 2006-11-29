// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 25BC30A2-1110-11D9-9B0F-000A957659CC

package net.spy.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.spy.util.Range;

/**
 * Test range operations.
 */
public class RangeTest extends TestCase {

	/**
	 * Get an instance of RangeTest.
	 */
	public RangeTest(String name) {
		super(name);
	}

	/** 
	 * Get the test suite.
	 */
	public static Test suite() {
		return new TestSuite(RangeTest.class);
	}

	/** 
	 * Run the test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	/** 
	 * Test the Comparable implementation (sorting).
	 */
	@SuppressWarnings("unchecked")
	public void testRangeSorting() {
		Integer a=new Integer(1);
		Integer b=new Integer(2);
		Integer c=new Integer(3);
		Integer d=new Integer(4);
		Integer e=new Integer(5);
		Integer f=new Integer(6);

		String[] sortedVals={
			"null-1", "null-2", "1-1", "1-2", "1-null", "2-2",
			"2-3", "2-null", "3-3", "3-4", "4-4", "4-5", "5-5", "5-6", "6-6"
			};

		ArrayList<Range<Integer>> al=new ArrayList<Range<Integer>>();
		al.add(new Range<Integer>(null, a));
		al.add(new Range<Integer>(null, b));
		al.add(new Range<Integer>(a, a));
		al.add(new Range<Integer>(a, b));
		al.add(new Range<Integer>(a, null));
		al.add(new Range<Integer>(b, b));
		al.add(new Range<Integer>(b, null));
		al.add(new Range<Integer>(b, c));
		al.add(new Range<Integer>(c, c));
		al.add(new Range<Integer>(c, d));
		al.add(new Range<Integer>(d, d));
		al.add(new Range<Integer>(d, e));
		al.add(new Range<Integer>(e, e));
		al.add(new Range<Integer>(e, f));
		al.add(new Range<Integer>(f, f));

		// Unsort
		Collections.shuffle(al);

		// Sort
		Collections.sort(al);

		// Validate the order
		for(int i=0; i<sortedVals.length; i++) {
			Range r=al.get(i);
			String tmps=r.getLow() + "-" + r.getHigh();

			assertEquals("Sort failure", sortedVals[i], tmps);
		}
	}

	public void testSimpleCompare() {
		Integer a=new Integer(1);
		Integer b=new Integer(2);

		assertEquals(0,
			new Range<Integer>(a, b).compareTo(new Range<Integer>(a, b)));
		assertEquals(1,
			new Range<Integer>(a, null).compareTo(new Range<Integer>(a, b)));
		assertEquals(-1,
			new Range<Integer>(a, b).compareTo(new Range<Integer>(a, null)));
		assertEquals(0,
			new Range<Integer>(a, null).compareTo(new Range<Integer>(a, null)));
		assertEquals(-1,
			new Range<Integer>(null, a).compareTo(new Range<Integer>(a, null)));
		assertEquals(1,
			new Range<Integer>(a, null).compareTo(new Range<Integer>(null, a)));
	}

	@SuppressWarnings("unchecked")
	public void testRangeEquality() {
		ArrayList<Range<Integer>> al=new ArrayList<Range<Integer>>();
		Integer a=new Integer(1);
		Integer b=new Integer(2);
		al.add(new Range<Integer>(null, a));
		al.add(new Range<Integer>(null, b));
		al.add(new Range<Integer>(a, a));
		al.add(new Range<Integer>(a, b));
		al.add(new Range<Integer>(a, null));
		al.add(new Range<Integer>(b, b));
		al.add(new Range<Integer>(b, null));

		HashSet<Range<Integer>> hs=new HashSet<Range<Integer>>(al);

		assertEquals(al.size(), hs.size());

		// Make sure they can all be looked up
		for(Iterator i=al.iterator(); i.hasNext();) {
			assertTrue(hs.contains(i.next()));
		}
	}

	public void testForeignEquality() {
		Range<Integer> r=new Range<Integer>(new Integer(0), new Integer(1));
		assertFalse(r.equals("x"));
	}

	// Verify r.contains(c)
	@SuppressWarnings("unchecked")
	private void assertRangeHit(Range r, Comparable c) {
		// System.out.println("Expecting " + c + " in " + r);
		assertTrue(r + " should contain " + c, r.contains(c));
	}

	// Verify !r.contains(c)
	@SuppressWarnings("unchecked")
	private void assertRangeMiss(Range r, Comparable c) {
		// System.out.println("Not expecting " + c + " in " + r);
		assertTrue(r + " should not contain " + c, (!r.contains(c)));
	}

	@SuppressWarnings("unchecked")
	private void assertInvalidConstruct(Comparable l, Comparable h) {
		try {
			Range r=new Range(l, h);
			fail("Allowed to make range from " + l + " to " + h + ": " + r);
		} catch(IllegalArgumentException e) {
			// pass
		}
	}

	public void testBadConstructors() {
		assertInvalidConstruct(null, null);
		assertInvalidConstruct(new Integer(2), new Integer(1));
	}

	public void testDefaultParams() {
		Range<Integer> r=new Range<Integer>(new Integer(0), new Integer(100));
		assertEquals(Range.MatchType.INCLUSIVE, r.getLowMatch());
		assertEquals(Range.MatchType.INCLUSIVE, r.getHighMatch());
	}

	public void testRangeOperations() {
		Integer a=new Integer(3);
		Integer b=new Integer(9);

		// Build some ranges
		Range<Integer> athroughb=new Range<Integer>(a, b);
		Range<Integer> athrougha=new Range<Integer>(a, a);
		Range<Integer> nullthroughb=new Range<Integer>(null, b);
		Range<Integer> bthroughnull=new Range<Integer>(b, null);

		// Exclusive matched ends
		Range<Integer> athroughbx=new Range<Integer>(a, b);
		athroughbx.setHighMatch(Range.MatchType.EXCLUSIVE);
		Range<Integer> axthroughb=new Range<Integer>(a, b);
		axthroughb.setLowMatch(Range.MatchType.EXCLUSIVE);
		Range<Integer> axthroughbx=new Range<Integer>(a, b);
		axthroughbx.setLowMatch(Range.MatchType.EXCLUSIVE);
		axthroughbx.setHighMatch(Range.MatchType.EXCLUSIVE);

		// Things to look for
		Integer a0=new Integer(1);
		Integer b0=new Integer(7);
		Integer c=new Integer(137);

		assertRangeHit(athrougha, a);
		assertRangeMiss(athrougha, a0);
		assertRangeMiss(athrougha, b);
		assertRangeMiss(athrougha, null);

		assertRangeHit(athroughb, a);
		assertRangeHit(athroughb, b);
		assertRangeHit(athroughb, b0);
		assertRangeMiss(athroughb, a0);
		assertRangeMiss(athroughb, c);
		assertRangeMiss(athroughb, null);

		assertRangeHit(nullthroughb, a0);
		assertRangeHit(nullthroughb, a);
		assertRangeHit(nullthroughb, b);
		assertRangeMiss(nullthroughb, c);
		assertRangeMiss(nullthroughb, null);

		assertRangeHit(bthroughnull, b);
		assertRangeHit(bthroughnull, c);
		assertRangeMiss(bthroughnull, a);
		assertRangeMiss(bthroughnull, null);

		assertRangeHit(athroughbx, a);
		assertRangeHit(athroughbx, b0);
		assertRangeMiss(athroughbx, a0);
		assertRangeMiss(athroughbx, b);
		assertRangeMiss(athroughbx, c);

		assertRangeHit(axthroughb, b);
		assertRangeHit(axthroughb, b0);
		assertRangeMiss(axthroughb, a);
		assertRangeMiss(axthroughb, a0);
		assertRangeMiss(axthroughb, c);

		assertRangeHit(axthroughbx, b0);
		assertRangeMiss(axthroughbx, a);
		assertRangeMiss(axthroughbx, b);
		assertRangeMiss(axthroughbx, a0);
		assertRangeMiss(axthroughbx, c);
	}

}
