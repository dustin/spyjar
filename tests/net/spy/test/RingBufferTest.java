// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: RingBufferTest.java,v 1.2 2002/10/31 08:12:18 dustin Exp $

package net.spy.test;

import java.util.ArrayList;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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

	/**
	 * Get the test suite.
	 */
	public static Test suite() {
		return new TestSuite(RingBufferTest.class);
	}

	/**
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	private void verify(RingBuffer rb) {
		ArrayList a=new ArrayList(rb);

		// The buffer should be at capacity here
		assertTrue("Size is incorrect.", (rb.size() == a.size()));
		assertTrue("Capacity not filled.", (rb.size() == rb.getCapacity()));

		int i=((Integer)a.get(0)).intValue();
		for(Iterator it=a.iterator(); it.hasNext(); i++) {
			Integer itmp=(Integer)it.next();
			int tmp=itmp.intValue();
			assertEquals("Out of sequence", tmp, i);
		}
	}

	/** 
	 * Basic RingBuffer test.
	 */
	public void testRingBuffer() {
		int cap=256;
		RingBuffer rb=new RingBuffer(cap);

		// Fill 'er up
		for(int i=1; i<cap; i++) {
			rb.add(new Integer(i));
			assertTrue("Capacity filled prematurely", rb.size() < cap);
		}

		for(int i=cap; i<2048; i++) {
			rb.add(new Integer(i));
			assertTrue("Exceeded capacity", rb.size() <= cap);
			verify(rb);
		}
	}

}
