// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// $Id: BitArray.java,v 1.2 2002/11/06 20:01:40 dustin Exp $

package net.spy.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

/**
 * An array of bits.
 *
 * The initial version of this is not intended to be efficient.
 */
public class BitArray extends Object {

	private List bitList=null;

	/**
	 * Get an instance of BitArray.
	 */
	public BitArray() {
		super();
		bitList=new ArrayList();
	}

	/** 
	 * Add a bit.
	 * 
	 * @param value if true, add a one bit.
	 */
	public void add(boolean value) {
		if(value) {
			bitList.add(new Integer(1));
		} else {
			bitList.add(new Integer(0));
		}
	}

	/** 
	 * Add a set of bits from an integer.
	 * 
	 * @param bitSet the integer containing the bits
	 * @param numBits the number of bits to add.
	 */
	public void addBits(int bitSet, int numBits) {
		List bitsToAdd=new ArrayList(numBits);

		for(int i=0; i<numBits; i++) {
			if( (bitSet&0x1) == 0) {
				bitsToAdd.add(new Integer(0));
			} else {
				bitsToAdd.add(new Integer(1));
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
		List l=bitList.subList(from, from+number);
		for(Iterator i=l.iterator(); i.hasNext();) {
			Integer iTmp=(Integer)i.next();
			// Make room for the new bit
			rv<<=1;
			// Add it
			rv|=iTmp.intValue();
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

		for(Iterator i=bitList.iterator(); i.hasNext(); ) {
			sb.append(i.next());
		}

		return(sb.toString());
	}

}
