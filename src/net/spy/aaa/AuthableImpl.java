// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: AuthableImpl.java,v 1.2 2003/08/05 09:01:02 dustin Exp $

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

