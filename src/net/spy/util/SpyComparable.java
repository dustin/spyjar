// Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 89655BD2-1110-11D9-8018-000A957659CC

package net.spy.util;

/**
 * SpyComparible allows a class to describe how two objects should be
 * compared.  One of these must be implemented for each type of object
 * we'll be sorting.
 */

public interface SpyComparable {
	/**
	 * Compare two objects.
	 */
	int compare(Object obj1, Object obj2);
}

