// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: Authable.java,v 1.1 2002/08/28 00:34:55 dustin Exp $

package net.spy.aaa;

/**
 * Authable things.
 */
public interface Authable {

	/**
	 * Get the username who's trying to auth.
	 */
	String getUsername();

}

