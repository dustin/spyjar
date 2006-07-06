// Copyright (c) 2004  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 5AA2B023-1110-11D9-AA16-000A957659CC

package net.spy.ant;

import java.io.IOException;
import java.lang.reflect.Constructor;

import net.spy.util.InterfaceImplementor;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Task to implement an interface.
 */
public class InterfaceImplementorTask extends Task {

	private String superClass=null;
	private String interfaceName=null;
	private String outClass=null;
	private String outDir=null;

	/**
	 * Get an instance of InterfaceImplementorTask.
	 */
	public InterfaceImplementorTask() {
		super();
	}

	public void setSuperClass(String to) {
		this.superClass=to;
	}

	public void setInterfaceName(String to) {
		this.interfaceName=to;
	}

	public void setOutClass(String to) {
		this.outClass=to;
	}

	public void setOutDir(String to) {
		this.outDir=to;
	}

	private void validateArg(String val, String name) throws BuildException {
		if(val == null) {
			throw new BuildException("Missing argument:  " + name);
		}
	}

	private void validateArgs() throws BuildException {
		validateArg(interfaceName, "interfaceName");
		validateArg(outClass, "outClass");
		validateArg(outDir, "outDir");
	}

	protected void generateWith(Class<? extends InterfaceImplementor> c)
		throws BuildException {
		// Load the interface
		Class<?> theInterface=null;
		try {
			theInterface=Class.forName(interfaceName);
		} catch(ClassNotFoundException e) {
			throw new BuildException("Could not load interface "
				+ interfaceName, e);
		}
		InterfaceImplementor ii=null;
		// Instantiate the InterfaceImplementor.
		try {
			Class[] params={Class.class};
			Constructor<? extends InterfaceImplementor> cons
				=c.getConstructor(params);
			Object[] args={theInterface};
			ii=cons.newInstance(args);
		} catch(Exception e) {
			e.printStackTrace();
			throw new BuildException(
				"Could not instantiate " + c + " with " + theInterface, e);
		}
		// Generate
		try {
			if(superClass != null) {
				ii.setSuperClass(Class.forName(superClass));
			}

			ii.setOutputClass(outClass);
			ii.writeSourceToFile(outDir);
		} catch(IOException e) {
			e.printStackTrace();
			throw new BuildException("Couldn't generate class", e);
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
			throw new BuildException("Couldn't generate class", e);
		}
	}

	/** 
	 * Perform the transformation.
	 */
	public void execute() throws BuildException {
		validateArgs();
		generateWith(InterfaceImplementor.class);
	}

}
