// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 272DF956-1110-11D9-B0CD-000A957659CC

package net.spy.test;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import junit.framework.TestCase;
import net.spy.util.RingBuffer;

/**
 * Test the ring buffer functionality.
 */
public class RingBufferTest extends TestCase {

	/**
	 * Get an instance of RingBufferTest.
	 */
	public RingBufferTest(String name) {
		super(name);
	}

	private void verify(RingBuffer<Integer> rb) {
		ArrayList<Integer> a=new ArrayList<Integer>(rb);

		// The buffer should be at capacity here
		assertTrue("Size is incorrect.", (rb.size() == a.size()));
		assertTrue("Capacity not filled.", (rb.size() == rb.getCapacity()));

		int i=a.get(0).intValue();
		for(int tmp : a) {
			assertEquals("Out of sequence", tmp, i);
			i++;
		}
		String.valueOf(rb);
	}

	/** 
	 * Basic RingBuffer test.
	 */
	public void testRingBuffer() {
		int cap=256;
		RingBuffer<Integer> rb=new RingBuffer<Integer>(cap);

		// Fill 'er up
		for(int i=1; i<cap; i++) {
			rb.add(i);
			assertFalse(rb.hasWrapped());
			assertTrue("Capacity filled prematurely", rb.size() < cap);
		}

		rb.add(cap);

		for(int i=cap+1; i<2048; i++) {
			rb.add(i);
			assertTrue("Exceeded capacity", rb.size() == cap);
			assertTrue(rb.hasWrapped());
			verify(rb);
		}
	}

	public void testRingBufferFromArray() {
		int cap=256;

		ArrayList<Integer> a=new ArrayList<Integer>(cap*2);
		for(int i=0; i<cap*2; i++) {
			a.add(i);
		}
		RingBuffer<Integer> rb=new RingBuffer<Integer>(cap, a);
		verify(rb);

		assertTrue(rb.hasWrapped());
	}

	public void testIterator() {
		int cap=256;
		ArrayList<Integer> a=new ArrayList<Integer>(cap*2);
		for(int i=0; i<cap*2; i++) {
			a.add(i);
		}
		RingBuffer<Integer> rb=new RingBuffer<Integer>(cap, a);

		Iterator itmp=rb.iterator();
		rb.add(1);
		try {
			itmp.hasNext();
		} catch(ConcurrentModificationException e) {
			// pass
		}

		for(itmp=rb.iterator(); itmp.hasNext(); ) {
			itmp.next();
			try {
				itmp.remove();
				fail("RingBuffer iterator allowed me to remove an item");
			} catch(UnsupportedOperationException e) {
				// pass
			}
		}
		try {
			itmp.next();
			fail("RingBuffer iterator allowed me to get more than it had");
		} catch(NoSuchElementException e) {
			// pass
		}
	}

}
