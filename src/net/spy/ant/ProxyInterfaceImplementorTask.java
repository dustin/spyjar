// Copyright (c) 2004  Dustin Sallings <dustin@spy.net>

package net.spy.ant;

import org.apache.tools.ant.BuildException;

import net.spy.util.ProxyInterfaceImplementor;

/**
 * Generate an interface wrapper proxy.
 */
public class ProxyInterfaceImplementorTask extends InterfaceImplementorTask {

	/**
	 * Get an instance of ProxyInterfaceImplementorTask.
	 */
	public ProxyInterfaceImplementorTask() {
		super();
	}

	/** 
	 * Perform the generation.
	 */
	public void execute() throws BuildException {
		generateWith(ProxyInterfaceImplementor.class);
	}

}
