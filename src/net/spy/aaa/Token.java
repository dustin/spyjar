// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 54D5FE54-1110-11D9-95D6-000A957659CC

package net.spy.aaa;

/**
 * A token provided by a security manager after a successful authentication.
 */
public class Token extends Object {

	private String username=null;

	/**
	 * Get an instance of Token.
	 */
	public Token(String un) {
		super();
		this.username=un;
	}

	/**
	 * Get the username.
	 */
	public String getUsername() {
		return(username);
	}

	/**
	 * String me.
	 */
	public String toString() {
		return("tok<" + username + "," + Integer.toHexString(hashCode()) +">");
	}

	// test
	public static void main(String args[]) {
		Token t=new Token(args[0]);
		System.out.println("Got token:  " + t);
	}

}

