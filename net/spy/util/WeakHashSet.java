// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: WeakHashSet.java,v 1.1 2002/10/16 05:46:39 dustin Exp $

package net.spy.util;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import java.util.Collection;

/**
 * Implementation of ReferenceSet that uses weak references.
 */
public class WeakHashSet extends ReferenceSet {

	/**
	 * Get an instance of WeakHashSet.
	 */
	public WeakHashSet() {
		super();
	}

	/** 
	 * Create a WeakHashSet with the given capacity.
	 * 
	 * @param n the capacity
	 */
	public WeakHashSet(int n) {
		super(n);
	}

	/** 
	 * Get a WeakHashSet with the contents from the given Collection.
	 * 
	 * @param c the collection
	 */
	public WeakHashSet(Collection c) {
		super(c);
	}

	/** 
	 * Return a weak reference.
	 */
	protected Reference getReference(Object o) {
		return(new MyWeakReference(o));
	}

	private class MyWeakReference extends WeakReference {

		public MyWeakReference(Object o) {
			super(o);
		}

		public int hashCode() {
			int rv=0;
			Object o=get();
			if (o != null) {
				rv=o.hashCode();
			}

			return (rv);
		}

		public boolean equals(Object o) {
			boolean rv=false;
			Object me=get();
			if(me!=null) {
				rv=me.equals(o);
			}

			return(rv);
		}

	}

}
