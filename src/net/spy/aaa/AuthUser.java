// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 508EC6B0-1110-11D9-926C-000A957659CC

package net.spy.aaa;

/**
 * A user capable of checking its own password.  Used internally by the
 * Authenticator.
 */
public interface AuthUser {

	/**
	 * Check a password for this user.
	 */
	void checkPassword(String password) throws AuthException;

}

