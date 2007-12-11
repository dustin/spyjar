// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>

package net.spy.util;

import java.util.Comparator;

/**
 * A Comparator that compares the String representation of objects.
 */
public class ToStringComparator extends Object implements Comparator<Object> {

	/**
	 * For sort order with nulls first.
	 */
	public static final int NULLS_FIRST=-1;
	/**
	 * For sort order with nulls last.
	 */
	public static final int NULLS_LAST=1;

	private final int nullOrder;

	/**
	 * Get an instance of ToStringComparator sorting nulls last.
	 */
	public ToStringComparator() {
		this(NULLS_LAST);
	}

	/**
	 * Get a ToStringComparator with the provided null order.
	 *
	 * @param no how nulls are ordered
	 */
	public ToStringComparator(int no) {
		super();
		nullOrder=no;
	}

	/**
	 * Perform the comparison.
	 */
	public int compare(Object o1, Object o2) {
		int rv=0;
		if(o1 == null) {
			// First item is null, figure out where it goes
			rv=nullOrder;
		} else if(o2 == null) {
			// Second item is null, reverse the null order
			rv = 0 - nullOrder;
		} else {
			// Otherwise, do a normal string compare
			rv=o1.toString().compareTo(o2.toString());
		}
		return(rv);
	}

}
