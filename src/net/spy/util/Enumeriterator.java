// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 82479F4D-1110-11D9-9427-000A957659CC

package net.spy.util;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * Wrap an Enumeration to provide an Iterator interface.
 */
public class Enumeriterator extends Object implements Iterator {

	private Enumeration e=null;

	/**
	 * Get an instance of Enumeriterator.
	 */
	public Enumeriterator(Enumeration src) {
		super();
		this.e=src;
	}

	/** 
	 * Determine whether this Iterator has more elements.
	 * 
	 * @return true if another call to next() will return an object
	 */
	public boolean hasNext() {
		return(e.hasMoreElements());
	}

	/** 
	 * Get the next object.
	 * 
	 * @return the next Object
	 */
	public Object next() {
		return(e.nextElement());
	}

	/** 
	 * NOT IMPLEMENTED.
	 */
	public void remove() {
		throw new UnsupportedOperationException("No can do, chief.");
	}

}
