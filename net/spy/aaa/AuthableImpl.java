// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: AuthableImpl.java,v 1.1 2002/08/28 00:34:55 dustin Exp $

package net.spy.aaa;

/**
 * Simple implementation of an Authable.
 */
public class AuthableImpl extends Object implements Authable {

	private String username=null;

	/**
	 * Get an instance of AuthableImpl.
	 */
	public AuthableImpl(String username) {
		super();
		this.username=username;
	}

	/**
	 * Get the username.
	 */
	public String getUsername() {
		return(username);
	}

}

