/*
 * Copyright (c) 2002 Scott Lamb <slamb@slamb.org>
 * This code is released under the MIT license; see the file LICENSE.
 *
 * arch-tag: 87C2C1A0-1110-11D9-938F-000A957659CC
 */

package net.spy.util;

import org.apache.tools.ant.BuildException;

/**
 * This location is deprecated.  Ant tasks belong in net.spy.ant.
 **/
public class SPGenTask extends net.spy.ant.SPGenTask {

	/** 
	 * Warn of deprecation.
	 */
	public void execute() throws BuildException {
		System.err.println("net.spy.util.SPGenTask is deprecated, "
			+ "use net.spy.ant.SPGenTask");
		super.execute();
	}

}
