// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 1727EB84-1110-11D9-B332-000A957659CC

package net.spy.test;

import java.util.Collections;
import java.util.NoSuchElementException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.spy.util.LimitedList;

/**
 * Test the ring buffer functionality.
 */
public class LimitedListTest extends TestCase {

	/**
	 * Get an instance of LimitedListTest.
	 */
	public LimitedListTest(String name) {
		super(name);
	}

	/**
	 * Get the test suite.
	 */
	public static Test suite() {
		return new TestSuite(LimitedListTest.class);
	}

	/**
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	/** 
	 * Verify limited list limits properly.
	 */
	public void testLimitedList() {
		LimitedList<Integer> ll=new LimitedList(10);

		// These should all work
		for(int i=0; i<10; i++) {
			ll.add(new Integer(i));
		}

		assertEquals(10, ll.size());
		assertEquals(10, ll.getLimit());

		// This should fail
		try {
			ll.add(new Integer(11));
			fail("Allowed me to add eleven.");
		} catch(IndexOutOfBoundsException e) {
			// OK
		}

		// This should fail
		try {
			ll.addFirst(new Integer(11));
			fail("Allowed me to addFirst eleven.");
		} catch(IndexOutOfBoundsException e) {
			// OK
		}

		// This should fail
		try {
			ll.addLast(new Integer(11));
			fail("Allowed me to addLast eleven.");
		} catch(IndexOutOfBoundsException e) {
			// OK
		}

		// This should fail
		try {
			LimitedList tmp=new LimitedList(10);
			tmp.addLast(new Integer(11));
			ll.addAll(tmp);
			fail("Allowed me to addAll eleven.");
		} catch(IndexOutOfBoundsException e) {
			// OK
		}

		// This should fail
		try {
			LimitedList tmp=new LimitedList(10);
			tmp.addLast(new Integer(11));
			ll.addAll(2, tmp);
			fail("Allowed me to addAll eleven (2).");
		} catch(IndexOutOfBoundsException e) {
			// OK
		}

		// This should work again
		ll.removeFirst();
		ll.add(new Integer(11));

		// Add some more
		ll.setLimit(26);

		// These should all work
		for(int i=0; i<5; i++) {
			ll.addFirst(new Integer(i));
			ll.addAll(Collections.singleton(new Integer(i)));
			ll.addAll(7, Collections.singleton(new Integer(i)));
		}

		assertEquals(25, ll.size());
		assertEquals(26, ll.getLimit());

		// Test adding a value at a specific location
		ll.add(3, 17);
		assertEquals(new Integer(17), ll.get(3));
	}

	/** 
	 * Verify the thing works like a Queue should.
	 *
	 * This means stuff should come out in insert order, and you can't get
	 * more than you have.
	 */
	public void testQueue() {
		LimitedList ll=new LimitedList(10);
		for(int i=0; i<10; i++) {
			ll.add(new Integer(i));
		}

		for(int i=0; i<10; i++) {
			Integer tmpI=(Integer)ll.removeFirst();
			int tmp=tmpI.intValue();
			assertEquals("Error on value " + i, i, tmp);
		}

		try {
			ll.removeFirst();
			fail("Allowed me to remove more entries than I added.");
		} catch(NoSuchElementException e) {
			// OK
		}
	}

}
