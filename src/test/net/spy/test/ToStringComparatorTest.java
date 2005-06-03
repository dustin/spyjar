// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// arch-tag: CEB1D8F6-1D76-442B-936B-AA92C40A5828

package net.spy.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.spy.util.ToStringComparator;

/**
 * Base64 test.  Derived from the python base64 tests.
 */
public class ToStringComparatorTest extends TestCase {

	/**
	 * Get an instance of ToStringComparatorTest.
	 */
	public ToStringComparatorTest(String name) {
		super(name);
	}

	/** 
	 * Get the test suite.
	 */
	public static Test suite() {
		return new TestSuite(ToStringComparatorTest.class);
	}

	/** 
	 * Test the toString comparator.
	 */
	public void testToStringComparator() {
		ToStringComparator tsc=new ToStringComparator();
		ToStringComparator tscnf=
			new ToStringComparator(ToStringComparator.NULLS_FIRST);
		ToStringComparator tscnl=
			new ToStringComparator(ToStringComparator.NULLS_LAST);

		// Plain compares
		assertEquals("a".compareTo("b"), tsc.compare("a", "b"));
		assertEquals("a".compareTo("b"), tscnf.compare("a", "b"));
		assertEquals("a".compareTo("b"), tscnl.compare("a", "b"));

		assertEquals(-1, tsc.compare("a", null));
		assertEquals(1, tscnf.compare("a", null));
		assertEquals(-1, tscnl.compare("a", null));

		assertEquals(1, tsc.compare(null, "a"));
		assertEquals(-1, tscnf.compare(null, "a"));
		assertEquals(1, tscnl.compare(null, "a"));
	}

}
