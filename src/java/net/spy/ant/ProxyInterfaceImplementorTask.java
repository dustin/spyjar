// Copyright (c) 2004  Dustin Sallings <dustin@spy.net>

package net.spy.ant;

import net.spy.util.ProxyInterfaceImplementor;

import org.apache.tools.ant.BuildException;

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
	@Override
	public void execute() throws BuildException {
		generateWith(ProxyInterfaceImplementor.class);
	}

}
