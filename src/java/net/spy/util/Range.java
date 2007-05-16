// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 869AE1AE-1110-11D9-BDD6-000A957659CC

package net.spy.util;

/**
 * A range of Comparable objects.
 */
public class Range<T extends Comparable<T>> extends Object
	implements Comparable<Range<T>> {

	public enum MatchType { INCLUSIVE, EXCLUSIVE }

	private final T low;
	private final T high;

	private MatchType lowMatch=MatchType.INCLUSIVE;
	private MatchType highMatch=MatchType.INCLUSIVE;

	/**
	 * Get an instance of Range.
	 */
	public Range(T lowObject, T highObject) {
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

		if((low != null) && (high != null)) {
			if(low.compareTo(high) > 0) {
				throw new IllegalArgumentException(
					"Low object must not be greater than the high object");
			}
		}
	}

	/** 
	 * Get the low match type.
	 */
	public MatchType getLowMatch() {
		return(lowMatch);
	}

	/** 
	 * Set the low match type.
	 * 
	 * @param lm either INCLUSIVE or EXCLUSIVE
	 */
	public void setLowMatch(MatchType lm) {
		lowMatch=lm;
	}

	/** 
	 * Get the high match type.
	 */
	public MatchType getHighMatch() {
		return(highMatch);
	}

	/** 
	 * Set the high match type.
	 * 
	 * @param hm either INCLUSIVE or EXCLUSIVE
	 */
	public void setHighMatch(MatchType hm) {
		this.highMatch=hm;
	}

	/** 
	 * Get the low object.
	 */
	public Comparable<T> getLow() {
		return(low);
	}

	/** 
	 * Get the high object.
	 */
	public Comparable<T> getHigh() {
		return(high);
	}

	/** 
	 * Describe this object in Set notation.
	 */
	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder(256);

		sb.append("{Range ");
		if(lowMatch == MatchType.INCLUSIVE) {
			sb.append('[');
		} else {
			sb.append('(');
		}
		sb.append(low);

		sb.append(", ");
		sb.append(high);
		if(highMatch == MatchType.INCLUSIVE) {
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
	public boolean contains(T c) {
		// False if we prove it's not there
		boolean rv=true;

		int lowCompare=0;
		int highCompare=0;

		if(lowMatch == MatchType.EXCLUSIVE) {
			lowCompare=1;
		}

		if(highMatch == MatchType.EXCLUSIVE) {
			highCompare=-1;
		}

		if(c != null) {
			// If low and high are the same, this range represents an exact
			// match.
			if((low != null) && (high != null) && low.equals(high)) {
				if(!(c.compareTo(low) == 0)) {
					rv=false;
				}
			} else {
				// Make sure it's not below the low
				if((low != null) && (c.compareTo(low) < lowCompare)) {
					rv=false;
				}

				// Make sure it's not above the high
				if((high != null) && (c.compareTo(high) > highCompare)) {
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
	public int compareTo(Range<T> r) {
		int rv=compareLow(this, r);
		if(rv == 0) {
			rv=compareHigh(this, r);
		}
		return(rv);
	}

	/** 
	 * True if the given object is a Range object that represents the same
	 * range.
	 */
	@Override
	// Unchecked Range cast.
	@SuppressWarnings("unchecked")
	public boolean equals(Object o) {
		boolean rv=false;
		if(o instanceof Range) {
			rv= (compareTo((Range<T>)o)==0);
		}
		return(rv);
	}

	/** 
	 * Get a predictable hash code.
	 * @return the low object's hash code if it's available, else the high
	 * 	object's hash code
	 */
	@Override
	public int hashCode() {
		return(low != null ? low.hashCode() : high.hashCode());
	}

	// Compare two ranges based on their low value
	private int compareLow(Range<T> a, Range<T> b) {
		int rv=0;

		if(!(a.low==null && b.low == null)) {
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
	private int compareHigh(Range<T> a, Range<T> b) {
		int rv=0;

		if(!(a.high==null && b.high == null)) {
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
