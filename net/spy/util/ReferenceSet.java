// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: ReferenceSet.java,v 1.3 2003/04/16 05:03:57 dustin Exp $

package net.spy.util;

import java.lang.ref.Reference;

import java.util.Set;
import java.util.HashSet;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class aids in implementing sets of references.
 */
public abstract class ReferenceSet extends AbstractSet {

	private HashSet contents=null;

	/**
	 * Get an instance of ReferenceSet.
	 */
	public ReferenceSet() {
		super();

		contents=new HashSet();
	}

	/** 
	 * Create a ReferenceSet with the given capacity.
	 * 
	 * @param n the initial capacity
	 */
	public ReferenceSet(int n) {
		super();
		contents=new HashSet(n);
	}

	/** 
	 * Get a ReferenceSet with the contents from the given Collection.
	 * 
	 * @param c the collection
	 */
	public ReferenceSet(Collection c) {
		super();
		if (c == null) {
			throw new NullPointerException(
				"Null collection provided to ReferenceSet");
		}

		contents = new HashSet(c.size() * 2);

		// Copy references into the content map
		for(Iterator i=c.iterator(); i.hasNext();) {
			add(i.next());
		}
	}

	/** 
	 * Add an object to the Set.
	 * 
	 * @param o the object
	 * @return true if the object did not already exist
	 */
	public boolean add(Object o) {
		boolean rv=contents.add(getReference(o));

		return (rv);
	}

	/** 
	 * Get the current size of the Set.  This is not an entirely cheap
	 * operation, as it walks the entire iterator to make sure all entries
	 * are still valid references.
	 */
	public int size() {
		int rv=0;
		for(Iterator i=iterator(); i.hasNext();) {
			i.next();
			rv++;
		}
		return(rv);
	}

	/** 
	 * Get an iterator.
	 *
	 * This iterator does not support removing entries due to limitations
	 * with HashMap and Iterator that would otherwise require me to
	 * duplicate all of HashMap.
	 */
	public Iterator iterator() {
		return(new ReferenceIterator(contents.iterator()));
	}

	/** 
	 * Obtain the desired type of reference to the given object.
	 *
	 *  <p>
	 * Unfortunately, java doesn't give me a way to enforce this in the
	 * language (i.e. at compile time), but subclasses of ReferenceSet
	 * must implement hashCode() and equals() in such a way that they
	 * return what the referenced object would return if the object were
	 * not a reference.  If the reference has disappeared, equals() should
	 * return false, and hashCode should return 0.
	 * </p>
	 * 
	 * @param o an object
	 * @return a reference to that object
	 */
	protected abstract Reference getReference(Object o);

	private class ReferenceIterator extends Object implements Iterator {

		private Iterator backIterator=null;
		boolean hasNext=false;
		private Object current=null;
		private Reference currentRef=null;

		public ReferenceIterator(Iterator i) {
			super();

			this.backIterator=i;
			findNext();
		}

		public boolean hasNext() {
			return(hasNext);
		}

		public Object next() {
			Object rv=current;
			findNext();
			return(rv);
		}

		public void remove() {
			throw new UnsupportedOperationException(
				"This is currently not supported");
		}

		private void findNext() {
			current=null;
			while(current==null && backIterator.hasNext()) {
				currentRef=(Reference)backIterator.next();
				current=currentRef.get();
				// If the reference is dead, get rid of our copy
				if(current==null) {
					backIterator.remove();
				}
			}
			if(current==null) {
				hasNext=false;
			} else {
				hasNext=true;
			}
		} // findNext

	}

}
