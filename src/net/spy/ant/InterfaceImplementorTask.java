// Copyright (c) 2004  Dustin Sallings <dustin@spy.net>

package net.spy.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.lang.reflect.Constructor;

import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;

import org.apache.tools.ant.Task;

import net.spy.util.InterfaceImplementor;

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

	public void setSuperClass(String superClass) {
		this.superClass=superClass;
	}

	public void setInterfaceName(String interfaceName) {
		this.interfaceName=interfaceName;
	}

	public void setOutClass(String outClass) {
		this.outClass=outClass;
	}

	public void setOutDir(String outDir) {
		this.outDir=outDir;
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

	protected void generateWith(Class c) throws BuildException {
		// Load the interface
		Class theInterface=null;
		try {
			theInterface=Class.forName(interfaceName);
		} catch(ClassNotFoundException e) {
			throw new BuildException("Could not load interface "
				+ interfaceName, e);
		}
		InterfaceImplementor i=null;
		// Instantiate the InterfaceImplementor.
		try {
			Class params[]={Class.class};
			Constructor cons=c.getConstructor(params);
			Object args[]={theInterface};
			i=(InterfaceImplementor)cons.newInstance(args);
		} catch(Exception e) {
			e.printStackTrace();
			throw new BuildException(
				"Could not instantiate " + c + " with " + theInterface, e);
		}
		// Generate
		try {
			if(superClass != null) {
				i.setSuperClass(Class.forName(superClass));
			}

			i.setOutputClass(outClass);
			i.writeSourceToFile(outDir);
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
		generateWith(InterfaceImplementor.class);
	}

}
