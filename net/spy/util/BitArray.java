// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// $Id: BitArray.java,v 1.1 2002/11/06 09:04:24 dustin Exp $

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
	 * Get a few bits from the front of the list.
	 * 
	 * @param numBits number of bits to get
	 * @return an int representing the bits requested
	 *
	 * @exception IllegalArgumentException if numBits greater than 31 or
	 * 			the number of bits remaining in the bit list
	 */
	public int getBits(int numBits) {
		if(numBits > 31) {
			throw new IllegalArgumentException("Too many bits requested.");
		}
		if(numBits > size()) {
			throw new IllegalArgumentException("Too many bits requested.");
		}
		int rv=0;
		for(Iterator i=bitList.iterator(); i.hasNext() && numBits>0; ) {
			Integer iTmp=(Integer)i.next();
			// Make room for the new bit
			rv<<=1;
			// Add it
			rv|=iTmp.intValue();
			i.remove();
			numBits--;
		}

		return(rv);
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
