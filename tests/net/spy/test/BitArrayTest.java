// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: BitArrayTest.java,v 1.1 2002/11/06 09:04:24 dustin Exp $

package net.spy.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.spy.util.BitArray;

/**
 * Test the BitArray implementation.
 */
public class BitArrayTest extends TestCase {

	/**
	 * Get an instance of BitArrayTest.
	 */
	public BitArrayTest(String name) {
		super(name);
	}

	/** 
	 * Get this test.
	 */
	public static Test suite() {
		return new TestSuite(BitArrayTest.class);
	}

	/** 
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	/** 
	 * Test simple adds.
	 */
	public void testSimple() {
		BitArray bitArray=new BitArray();
		bitArray.add(true);
		assertEquals("True didn't work.", "1", bitArray.toString());
		bitArray=new BitArray();
		bitArray.add(false);
		assertEquals("False didn't work.", "0", bitArray.toString());
		bitArray.add(true);
		assertEquals("False+True didn't work.", "01", bitArray.toString());
		bitArray.add(true);
		assertEquals("False+True+True didn't work.", "011",
			bitArray.toString());
	}

	/** 
	 * Test integer adds.
	 */
	public void testIntAdd() {
		BitArray bitArray=new BitArray();
		bitArray.addBits(0x6b, 7);
		assertEquals("0x6b add didn't work.", "1101011", bitArray.toString());
		bitArray.add(true);
		assertEquals("0x6b+true add didn't work.",
			"11010111", bitArray.toString());
		bitArray.addBits(0x6b, 7);
		assertEquals("0x6b+true+0x6b add didn't work.",
			"110101111101011", bitArray.toString());
	}

	/** 
	 * Test that we can get bits.
	 */
	public void testGetBits() {
		BitArray bitArray=new BitArray();
		bitArray.addBits(0x6b, 7);
		assertEquals("One didn't work", 1, bitArray.getBits(1));
		assertEquals("Two didn't work", 2, bitArray.getBits(2));
		assertEquals("Three didn't work", 5, bitArray.getBits(3));

		try {
			bitArray.getBits(16);
			fail("Allowed me to get more bits than existed.");
		} catch(IllegalArgumentException e) {
			// OK
		}

		// Add some bits, just to check the range
		bitArray.addBits(0xffff, 16);
		bitArray.addBits(0xffff, 16);
		bitArray.addBits(0xffff, 16);
		bitArray.addBits(0xffff, 16);

		try {
			bitArray.getBits(32);
			fail("Allowed me to get more than 32 bits.");
		} catch(IllegalArgumentException e) {
			// OK
		}

	}

}
