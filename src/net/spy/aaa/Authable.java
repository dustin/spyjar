// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 50D9E66E-1110-11D9-AFA1-000A957659CC

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

