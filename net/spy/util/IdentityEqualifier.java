// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
//
// $Id: IdentityEqualifier.java,v 1.1 2003/04/19 03:33:19 dustin Exp $

package net.spy.util;

/**
 * A wrapper object used to compare two objects on the basis of identity
 * rather than equality.
 */
public class IdentityEqualifier extends Object {

	private Object o=null;

	/** 
	 * Get an IdentityEqualifier wrapping the given object.
	 */
	public IdentityEqualifier(Object o) {
		super();
		this.o=o;
	}

	/** 
	 * Get the object this IdentityEqualifier is wrapping.
	 * 
	 * @return 
	 */
	public Object get() {
		return(o);
	}

	/** 
	 * True if this object has the same identity as the given object.
	 */
	public boolean equals(Object other) {
		boolean rv=false;

		// If the other object is an IdentityEqualifier, see if its
		// contained object is the same, otherwise, see if this object is
		// the same.
		if(other instanceof IdentityEqualifier) {
			IdentityEqualifier ie=(IdentityEqualifier)other;
			rv = (o == ie.o);
		} else {
			rv = (o == other);
		}

		return(rv);
	}

	/** 
	 * The hashcode of this object.
	 * 
	 * @return the hashcode of the class of the wrapped object
	 */
	public int hashCode() {
		return(o.getClass().hashCode());
	}


}
