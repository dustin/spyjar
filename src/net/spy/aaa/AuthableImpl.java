// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 51AA1C34-1110-11D9-A39F-000A957659CC

package net.spy.aaa;

/**
 * Simple implementation of an Authable.
 */
public class AuthableImpl extends Object implements Authable {

	private String username=null;

	/**
	 * Get an instance of AuthableImpl.
	 */
	public AuthableImpl(String un) {
		super();
		this.username=un;
	}

	/**
	 * Get the username.
	 */
	public String getUsername() {
		return(username);
	}

}

