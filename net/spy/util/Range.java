// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: Range.java,v 1.1 2003/07/05 07:50:54 dustin Exp $

package net.spy.util;

/**
 * A range of Comparable objects.
 */
public class Range extends Object implements Comparable {

	/** 
	 * Match type for inclusive matches.
	 */
	public static final int INCLUSIVE=1;

	/** 
	 * Match type for exclusive matches.
	 */
	public static final int EXCLUSIVE=2;

	private Comparable low=null;
	private Comparable high=null;

	private int lowMatch=INCLUSIVE;
	private int highMatch=INCLUSIVE;

	/**
	 * Get an instance of Range.
	 */
	public Range(Comparable lowObject, Comparable highObject) {
		super();

		low=lowObject;
		high=highObject;

		invariant();
	}

	// Class invariant
	private void invariant() {
		if (low==null && high==null) {
			throw new IllegalArgumentException(
				"At least one of the low or high object must be set.");
		}

		if( (low != null) && (high != null) ) {
			if(low.compareTo(high) > 0) {
				throw new IllegalArgumentException(
					"Low object must not be greater than the high object");
			}
		}

		validateMatch(lowMatch);
		validateMatch(highMatch);
	}

	private void validateMatch(int m) {
		if(m != INCLUSIVE && m != EXCLUSIVE) {
			throw new IllegalArgumentException("Invalid match type");
		}
	}

	/** 
	 * Get the low match type.
	 */
	public int getLowMatch() {
		return(lowMatch);
	}

	/** 
	 * Set the low match type.
	 * 
	 * @param lm either INCLUSIVE or EXCLUSIVE
	 */
	public void setLowMatch(int lm) {
		validateMatch(lm);
		lowMatch=lm;
	}

	/** 
	 * Get the high match type.
	 */
	public int getHighMatch() {
		return(highMatch);
	}

	/** 
	 * Set the high match type.
	 * 
	 * @param hm either INCLUSIVE or EXCLUSIVE
	 */
	public void setHighMatch(int hm) {
		validateMatch(hm);
		this.highMatch=hm;
	}

	/** 
	 * Get the low object.
	 */
	public Comparable getLow() {
		return(low);
	}

	/** 
	 * Get the high object.
	 */
	public Comparable getHigh() {
		return(high);
	}

	/** 
	 * Describe this object in Set notation.
	 */
	public String toString() {
		StringBuffer sb=new StringBuffer(256);

		sb.append("{Range ");
		if(lowMatch == INCLUSIVE) {
			sb.append('[');
		} else {
			sb.append('(');
		}
		sb.append(low);

		sb.append(", ");
		sb.append(high);
		if(highMatch == INCLUSIVE) {
			sb.append(']');
		} else {
			sb.append(')');
		}

		sb.append("}");

		return(sb.toString());
	}

	/** 
	 * True if the given object lies within this range.
	 */
	public boolean contains(Comparable c) {
		// False if we prove it's not there
		boolean rv=true;

		int lowCompare=0;
		int highCompare=0;

		if(lowMatch == EXCLUSIVE) {
			lowCompare=1;
		}

		if(highMatch == EXCLUSIVE) {
			highCompare=-1;
		}

		if(c != null) {
			// If low and high are the same, this range represents an exact
			// match.
			if( (low != null) && (high != null) && low.equals(high) ) {
				if( !(c.compareTo(low) == 0)) {
					rv=false;
				}
			} else {
				// Make sure it's not below the low
				if( (low != null) && (c.compareTo(low) < lowCompare) ) {
					rv=false;
				}

				// Make sure it's not above the high
				if( (high != null) && (c.compareTo(high) > highCompare) ) {
					rv=false;
				}
			}
		} else {
			// Null isn't there
			rv=false;
		}

		return(rv);
	}

	/** 
	 * @see Comparable 
	 */
	public int compareTo(Object o) {
		int rv=compareLow(this, (Range)o);
		if(rv == 0) {
			rv=compareHigh(this, (Range)o);
		}
		return(rv);
	}

	/** 
	 * True if the given object is a Range object that represents the same
	 * range.
	 */
	public boolean equals(Object o) {
		boolean rv=false;
		if(o instanceof Range) {
			rv= (compareTo(o)==0);
		}
		return(rv);
	}

	/** 
	 * Get a predictable hash code.
	 * @return the low object's hash code if it's available, else the high
	 * 	object's hash code
	 */
	public int hashCode() {
		int rv=0;
		if(low != null) {
			rv=low.hashCode();
		} else {
			rv=high.hashCode();
		}
		return(rv);
	}

	// Compare two ranges based on their low value
	private int compareLow(Range a, Range b) {
		int rv=0;

		if(! (a.low==null && b.low == null) ) {
			// At least one is not null
			if(a.low != null && b.low != null) {
				// neither is null, plain compare
				rv=a.low.compareTo(b.low);
			} else {
				// One is null, null is first
				if(a.low == null) {
					// First one is null
					rv=-1;
				} else {
					// second is null
					rv=1;
				}
			}
		}

		return(rv);
	}

	// Compare two ranges based on their high value
	private int compareHigh(Range a, Range b) {
		int rv=0;

		if(! (a.high==null && b.high == null) ) {
			// At least one is not null
			if(a.high != null && b.high != null) {
				// neither is null, plain compare
				rv=a.high.compareTo(b.high);
			} else {
				// One is null, null is last
				if(a.high == null) {
					// First one is null
					rv=1;
				} else {
					// second is null
					rv=-1;
				}
			}
		}

		return(rv);
	}

}
