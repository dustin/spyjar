// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 54405A22-1110-11D9-813E-000A957659CC

package net.spy.aaa;

import java.security.Permission;
import java.security.PermissionCollection;

import java.util.Enumeration;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/**
 * A flexible SecurityManager type thing for use within applications.
 */
public class SpySecurityManager extends Object {

	private static Map managers=null;

	private Authenticator auther=null;
	private Set tokens=null;

	private SpyPolicy policy=null;

	/**
	 * Get an instance of SpySecurityManager.
	 */
	public SpySecurityManager() {
		super();
		initManagers();

		tokens=new HashSet();
	}

	private static synchronized void initManagers() {
		if(managers==null) {
			managers=Collections.synchronizedMap(new HashMap());
		}
	}

	// Static stuff

	/**
	 * Get a security manager by name.
	 */
	public static SpySecurityManager getSecurityManager(String name) {
		// XXX Insert security check here
		SpySecurityManager ssm=null;
		synchronized(managers) {
			ssm=(SpySecurityManager)managers.get(name);
		}
		return(ssm);
	}

	/**
	 * Set a security manager by name.
	 */
	public static void setSecurityManager(String name, SpySecurityManager ssm){
		// XXX Insert security check here
		synchronized(managers) {
			managers.put(name, ssm);
		}
	}

	// end static stuff

	/**
	 * Set the authenticator for this manager.
	 */
	public void setAuthenticator(Authenticator a) {
		if(auther!=null) {
			throw new SpySecurityException(
				"The authenticator may not be changed.");
		}
		this.auther=a;
	}

	private void checkToken(Token tok) {
		if(tok!=null) {
			synchronized(tokens) {
				if(!tokens.contains(tok)) {
					throw new SpySecurityException("Fraudulent token: " + tok);
				}
			}
		}
	}

	/**
	 * Get the policy.
	 */
	protected SpyPolicy getPolicy() {
		return(policy);
	}

	/**
	 * Find out of this user has this permission.
	 */
	public void checkPermission(Token tok, Permission perm) {
		// First, verify the token.
		checkToken(tok);

		// Get the policy.
		SpyPolicy sp=getPolicy();
		PermissionCollection pc=sp.getPermissions(tok);

		boolean ok=false;
		for(Enumeration e=pc.elements(); ok==false && e.hasMoreElements();) {
			Permission p=(Permission)e.nextElement();
			// True if the objects are equal or the pimp lies.
			if(p.equals(perm) || p.implies(perm)) {
				ok=true;
			}
		}

		// If we don't have permission, throw an exception.
		if(!ok) {
			throw new SpySecurityPermissionException(tok,perm);
		}
	}

	/**
	 * Attempt to authenticate an Authable with the given password.
	 */
	public Token authenticate(Authable a, String password)
		throws AuthException {

		// Get the token
		Token t=auther.authUser(a, password);
		// Save a copy in our stash
		synchronized(tokens) {
			tokens.add(t);
		}
		return(t);
	}

}
