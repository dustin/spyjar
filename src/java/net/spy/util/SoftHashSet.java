// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 89242245-1110-11D9-870E-000A957659CC

package net.spy.util;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collection;

/**
 * Implementation of ReferenceSet that uses soft references.
 */
public class SoftHashSet<T extends Object> extends ReferenceSet<T> {

	/**
	 * Get an instance of SoftHashSet.
	 */
	public SoftHashSet() {
		super();
	}

	/** 
	 * Create a SoftHashSet with the given capacity.
	 * 
	 * @param n the capacity
	 */
	public SoftHashSet(int n) {
		super(n);
	}

	/** 
	 * Get a SoftHashSet with the contents from the given Collection.
	 * 
	 * @param c the collection
	 */
	public SoftHashSet(Collection<T> c) {
		super(c);
	}

	/** 
	 * Return a soft reference.
	 */
	@Override
	protected Reference<T> getReference(T o) {
		return(new MySoftReference<T>(o));
	}

	static class MySoftReference<T> extends SoftReference<T> {

		public MySoftReference(T o) {
			super(o);
		}

		@Override
		public int hashCode() {
			int rv=0;
			Object o=get();
			if (o != null) {
				rv=o.hashCode();
			}

			return (rv);
		}

	}

}
