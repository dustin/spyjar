// Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
//
// $Id: LimitedList.java,v 1.1 2002/12/05 08:07:02 dustin Exp $

package net.spy.util;

import java.util.Collection;
import java.util.LinkedList;

import net.spy.SpyObject;

/**
 * A LinkedList with a maximum capacity.
 */
public class LimitedList extends LinkedList {

	private int limit=0;

	/** 
	 * Get an instance of Queue.
	 */
	public LimitedList(int limit) {
		super();
		this.limit=limit;
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
	 * Override add to enforce the limit.
	 * 
	 * @param index index at which to add
	 * @param o object to add
	 *
	 * @exception IndexOutOfBoundsException if the limit is exceeded (or
	 * the parent size thinks it's too big).
	 */
	public void add(int index, Object o) {
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
	public boolean add(Object o) {
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
	public void addFirst(Object o) {
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
	public void addLast(Object o) {
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
	public boolean addAll(int index, Collection c) {
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
	public boolean addAll(Collection c) {
		checkSizeForNew(c.size());
		return(super.addAll(c));
	}

	private void checkSizeForNew() {
		checkSizeForNew(1);
	}

	private void checkSizeForNew(int toAdd) {
		if( (size()+toAdd) > limit) {
			throw new IndexOutOfBoundsException(
				"Adding " + toAdd + " element(s) would cause the size of this "
				+ "LimitedList to exceed the maximum specified size:  "
				+ limit);
		}
	}

}
