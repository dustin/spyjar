// Copyright (c) 2005  Dustin Sallings <dustin@spy.net>
// arch-tag: 7A23F674-5EE8-11D9-B78C-000A957659CC

package net.spy.factory;

/**
 * An instance of an object returned from a factory.
 */
public interface Instance {

	/** 
	 * Get the ID of this instance.
	 */
	int getId();

	/** 
	 * HashCode must be implemented for all Instance objects.
	 */
	int hashCode();

	/** 
	 * equals must be implemented for all Instance objects.
	 */
	boolean equals(Object o);

}
