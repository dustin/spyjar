// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: SpyPermission.java,v 1.1 2002/08/28 00:34:55 dustin Exp $

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
