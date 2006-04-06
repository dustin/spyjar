// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 812BD47B-1110-11D9-94BA-000A957659CC

package net.spy.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An array of bits.
 *
 * The initial version of this is not intended to be efficient.
 */
public class BitArray extends Object {

	private List<Integer> bitList=null;

	/**
	 * Get an instance of BitArray with storage for a given number of bits.
	 */
	public BitArray(int size) {
		super();
		bitList=new ArrayList<Integer>(size);
	}

	/** 
	 * Get an intance of BitArray.
	 */
	public BitArray() {
		super();
		bitList=new ArrayList<Integer>();
	}

	/** 
	 * Get a BitArray by parsing a string.
	 *
	 * The string must only contain the characters 0 (zero), 1 (one), and
	 * whitespace.
	 * 
	 * @param bitString the string containing zeros and ones
	 */
	public BitArray(String bitString) {
		this(bitString.length());

		char acters[]=bitString.toCharArray();
		for(char c : acters) {
			if(!Character.isWhitespace(c)) {
				switch(c) {
					case '0':
						add(false);
						break;
					case '1':
						add(true);
						break;
					default:
						throw new IllegalArgumentException(
							"Only 0, 1, and whitespace is allowed.");
				} // switch - My name is Dustin Sallings, and I'm a programmer
			} // Ignore whitespace
		} // All characters
	}

	/** 
	 * Add a bit.
	 * 
	 * @param value if true, add a one bit.
	 */
	public void add(boolean value) {
		if(value) {
			bitList.add(1);
		} else {
			bitList.add(0);
		}
	}

	/** 
	 * Add a set of bits from an integer.
	 * 
	 * @param bitSet the integer containing the bits
	 * @param numBits the number of bits to add.
	 */
	public void addBits(int bitSet, int numBits) {
		List<Integer> bitsToAdd=new ArrayList<Integer>(numBits);

		for(int i=0; i<numBits; i++) {
			if( (bitSet&0x1) == 0) {
				bitsToAdd.add(0);
			} else {
				bitsToAdd.add(1);
			}
			bitSet>>=1;
		}
		Collections.reverse(bitsToAdd);
		bitList.addAll(bitsToAdd);
	}

	/** 
	 * Remove a give number of bits from the MSB side.
	 * 
	 * @param howMany bits to remove
	 */
	public void removeMSBBits(int howMany) {
		removeBits(0, howMany);
	}

	/** 
	 * Remove a give number of bits from the LSB side.
	 * 
	 * @param howMany bits to remove
	 */
	public void removeLSBBits(int howMany) {
		removeBits(bitList.size()-howMany, howMany);
	}

	/** 
	 * Remove a specific number of bits from a specific location.
	 * 
	 * @param from starting point
	 * @param howMany number of bits to remove.
	 */
	public void removeBits(int from, int howMany) {
		bitList.subList(from, from+howMany).clear();
	}

	/** 
	 * Get a given number of bits from a given offset.
	 * 
	 * @param from starting offset (from MSB side).
	 * @param number number of bits to retrieve
	 *
	 * @return an integer containing those bits
	 * @exception IllegalArgumentException if number &gt; 32
	 * @exception IndexOutOfBoundsException if the bit range would specify
	 * 				bits we don't have
	 */
	public int getBits(int from, int number) {
		if(number > 31) {
			throw new IllegalArgumentException(
				"Bits requested would exceed integer precision.");
		}

		int rv=0;
		// Get a sublist to walk
		List<Integer> l=bitList.subList(from, from+number);
		for(int iTmp : l) {
			// Make room for the new bit
			rv<<=1;
			// Add it
			rv|=iTmp;
		}

		return(rv);
	}

	/** 
	 * Get a given number of the most significant bits.
	 * 
	 * @param numBits number of bits to get
	 * @return an int representing the bits requested
	 *
	 * @exception IllegalArgumentException if numBits greater than 31 or
	 * 			the number of bits remaining in the bit list
	 */
	public int getMSBBits(int numBits) {
		return(getBits(0, numBits));
	}

	/** 
	 * Get a given number of the least significant bits.
	 * 
	 * @param numBits number of bits to get
	 * @return an int representing the bits requested
	 *
	 * @exception IllegalArgumentException if numBits greater than 31 or
	 * 			the number of bits remaining in the bit list
	 */
	public int getLSBBits(int numBits) {
		return(getBits((bitList.size()-numBits), numBits));
	}

	/** 
	 * Get the number of bits remaining in the bit set.
	 */
	public int size() {
		return(bitList.size());
	}

	/** 
	 * String me.
	 */
	public String toString() {
		StringBuffer sb=new StringBuffer(bitList.size());

		for(int i : bitList) {
			sb.append(i);
		}

		return(sb.toString());
	}

}
