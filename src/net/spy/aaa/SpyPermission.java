// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 52DDFB5F-1110-11D9-8949-000A957659CC

package net.spy.aaa;

import java.security.BasicPermission;

/**
 * Permissions used in spy stuff.
 */
public class SpyPermission extends BasicPermission {

	/**
	 * Get an instance of SpyPermission.
	 */
	public SpyPermission(String name) {
		super(name);
	}

}
