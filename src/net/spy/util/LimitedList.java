// Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 8303CC5C-1110-11D9-B1B6-000A957659CC

package net.spy.util;

import java.util.Collection;
import java.util.LinkedList;

/**
 * A LinkedList with a maximum capacity.
 */
public class LimitedList<T extends Object> extends LinkedList<T> {

	private int limit=0;

	/** 
	 * Get an instance of Queue.
	 */
	public LimitedList(int l) {
		super();
		this.limit=l;
	}

	/** 
	 * Dynamically reset the limit.
	 * 
	 * This will not have an effect on existing items, but will prevent new
	 * items from being added if the limit is exceeded.
	 *
	 * @param to the new limit value
	 */
	public void setLimit(int to) {
		limit=to;
	}

	/** 
	 * Get the maximum number of elements that may be stored in this list.
	 */
	public int getLimit() {
		return(limit);
	}

	/** 
	 * Override add to enforce the limit.
	 * 
	 * @param index index at which to add
	 * @param o object to add
	 *
	 * @exception IndexOutOfBoundsException if the limit is exceeded (or
	 * the parent size thinks it's too big).
	 */
	public void add(int index, T o) {
		checkSizeForNew();
		super.add(index, o);
	}

	/** 
	 * Override add to enforce the limit.
	 * 
	 * @param o object to add
	 *
	 * @exception IndexOutOfBoundsException if the limit is exceeded (or
	 * the parent size thinks it's too big).
	 */
	public boolean add(T o) {
		checkSizeForNew();
		return(super.add(o));
	}

	/** 
	 * Override addFirst to enforce the limit.
	 * 
	 * @param o object to add
	 *
	 * @exception IndexOutOfBoundsException if the limit is exceeded (or
	 * the parent size thinks it's too big).
	 */
	public void addFirst(T o) {
		checkSizeForNew();
		super.addFirst(o);
	}

	/** 
	 * Override addLast to enforce the limit.
	 * 
	 * @param o object to add
	 *
	 * @exception IndexOutOfBoundsException if the limit is exceeded (or
	 * the parent size thinks it's too big).
	 */
	public void addLast(T o) {
		checkSizeForNew();
		super.addLast(o);
	}

	/** 
	 * Override addAll to enforce the limit.
	 * 
	 * @param index the index at which to add the collection
	 * @param c Collectoin of objects to add
	 *
	 * @exception IndexOutOfBoundsException if the limit is exceeded (or
	 * the parent size thinks it's too big).
	 */
	public boolean addAll(int index, Collection<? extends T> c) {
		checkSizeForNew(c.size());
		return(super.addAll(index, c));
	}

	/** 
	 * Override addAll to enforce the limit.
	 * 
	 * @param c Collectoin of objects to add
	 *
	 * @exception IndexOutOfBoundsException if the limit is exceeded (or
	 * the parent size thinks it's too big).
	 */
	public boolean addAll(Collection<? extends T> c) {
		checkSizeForNew(c.size());
		return(super.addAll(c));
	}

	private void checkSizeForNew() {
		checkSizeForNew(1);
	}

	private void checkSizeForNew(int toAdd) {
		if((size()+toAdd) > limit) {
			throw new IndexOutOfBoundsException(
				"Adding " + toAdd + " element(s) would cause the size of this "
				+ "LimitedList to exceed the maximum specified size:  "
				+ limit);
		}
	}

}
