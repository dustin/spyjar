// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 0B3A8E46-1110-11D9-84C4-000A957659CC

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
		assertEquals(1, bitArray.size());
		assertEquals("True didn't work.", "1", bitArray.toString());
		bitArray=new BitArray();
		bitArray.add(false);
		assertEquals(1, bitArray.size());
		assertEquals("False didn't work.", "0", bitArray.toString());
		bitArray.add(true);
		assertEquals(2, bitArray.size());
		assertEquals("False+True didn't work.", "01", bitArray.toString());
		bitArray.add(true);
		assertEquals(3, bitArray.size());
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
	 * Test that we can get MSB bits.
	 */
	public void testGetMSBBits() {
		BitArray bitArray=new BitArray();
		// 1101011
		bitArray.addBits(0x6b, 7);
		assertEquals(7, bitArray.size());
		assertEquals("One didn't work", 1, bitArray.getMSBBits(1));
		bitArray.removeMSBBits(1);
		assertEquals(6, bitArray.size());
		assertEquals("Two didn't work", 2, bitArray.getMSBBits(2));
		bitArray.removeMSBBits(2);
		assertEquals(4, bitArray.size());
		assertEquals("Three didn't work", 5, bitArray.getMSBBits(3));
		bitArray.removeMSBBits(3);
		assertEquals(1, bitArray.size());

		try {
			bitArray.getMSBBits(16);
			fail("Allowed me to get more bits than existed.");
		} catch(IndexOutOfBoundsException e) {
			// OK
		}

		// Add some bits, just to check the range
		bitArray.addBits(0xffff, 16);
		bitArray.addBits(0xffff, 16);
		bitArray.addBits(0xffff, 16);
		bitArray.addBits(0xffff, 16);

		try {
			bitArray.getMSBBits(32);
			fail("Allowed me to get more than 32 bits.");
		} catch(IllegalArgumentException e) {
			// OK
		}

	}

	/** 
	 * Test that we can get LSB bits.
	 */
	public void testGetLSBBits() {
		BitArray bitArray=new BitArray();
		// 1101011
		bitArray.addBits(0x6b, 7);
		assertEquals("One didn't work", 1, bitArray.getLSBBits(1));
		bitArray.removeLSBBits(1);
		assertEquals("Two didn't work", 1, bitArray.getLSBBits(2));
		bitArray.removeLSBBits(2);
		assertEquals("Three didn't work", 5, bitArray.getLSBBits(3));

		try {
			bitArray.getLSBBits(16);
			fail("Allowed me to get more bits than existed.");
		} catch(IndexOutOfBoundsException e) {
			// OK
		}

		// Add some bits, just to check the range
		bitArray.addBits(0xffff, 16);
		bitArray.addBits(0xffff, 16);
		bitArray.addBits(0xffff, 16);
		bitArray.addBits(0xffff, 16);

		try {
			bitArray.getLSBBits(32);
			fail("Allowed me to get more than 32 bits.");
		} catch(IllegalArgumentException e) {
			// OK
		}
	}

	/** 
	 * Test that we can get arbitrary bits.
	 */
	public void testGetArbitraryBits() {
		BitArray bitArray=new BitArray();
		// 1101011
		bitArray.addBits(0x6b, 7);
		assertEquals("Getting the middle bits didn't work",
			2, bitArray.getBits(2, 3));
	}

	/** 
	 * Test the string constructor.
	 */
	public void testStringConstructor() {
		String toTest="00101101110010111011101001101";
		BitArray bitArray=new BitArray(toTest);
		assertEquals("BitArray string construction failed",
			toTest, bitArray.toString());

		bitArray=new BitArray("    " + toTest + "\n\t   ");
		assertEquals("BitArray string construction with whitespace failed",
			toTest, bitArray.toString());

		try {
			bitArray=new BitArray(toTest + "2");
			fail("Parsed an invalid bit array.");
		} catch(IllegalArgumentException e) {
			// That's what it's supposed to do
		}
	}

}
