// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>
//
// $Id: ProxyInterfaceImplementor.java,v 1.1 2002/11/27 06:00:24 dustin Exp $

package net.spy.util;

import java.util.ArrayList;

import java.io.File;
import java.io.FileOutputStream;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.spy.SpyUtil;

/**
 * Create static proxy implementations of interfaces.
 *
 * This class is similiar to {@link InterfaceImplementor}, except it's used
 * to create static proxy interface implementations which may be extended
 * to override specific functionality (otherwise, the proxy would be kind
 * of worthless, wouldn't it?).
 */
public class ProxyInterfaceImplementor extends InterfaceImplementor {

	/**
	 * Get an instance of ProxyInterfaceImplementor.
	 *
	 * @param c the interface to be implemented
	 *
	 * @exception NullPointerException if the passed in class is null
	 * @exception IllegalArgumentException if the passed in class is not
	 * 			an interface
	 */
	public ProxyInterfaceImplementor(Class c) {
		super(c);
	}

	/** 
	 * Don't create the default constructors.
	 */
	protected boolean buildConstructors() {
		return(false);
	}

	/** 
	 * Create the instance variables and constructor for the proxy.
	 */
	protected String preConstructors() {
		Class i=getInterface();

		String ret="\tprivate " + i.getName() + " proxyedObject=null;\n\n";

		ret+="\t/**\n"
			+ "\t * Get a " + getOutClassName()
				+ " proxying the provided object.\n"
			+ "\t */\n";

		ret+="\tprotected " + getOutClassName() + "("
			+ i.getName() + " p) {\n"
			+ "\t\tsuper();\n"
			+ "\t\tthis.proxyedObject=p;\n"
			+ "\t}\n\n";

		return(ret);
	}

	/** 
	 * Implement this method as a proxy wrapper around the contained
	 * object.
	 * 
	 * @param method the method to implement
	 */
	protected String implement(Method method) {
		String ret="\t/**\n"
			+ "\t * ProxyInterfaceImplementor implementation of "
				+ method.getName() + ".\n"
			+ "\t * @see " + getInterface().getName() + "#"
				+ getDocLink(method) + "\n"
			+ "\t */\n";
		ret+="\t" + getSignature(method) + " {\n";

		// Get the parameters
		Class types[]=method.getParameterTypes();
		ArrayList l=new ArrayList(types.length);
		for(int i=0; i<types.length; i++) {
			l.add("a" + i);
		}
		String params=SpyUtil.join(l, ", ");

		// make the call via the proxy object
		Class rt=method.getReturnType();
		if(rt == Void.TYPE) {
			ret+="\t\tproxyedObject." + method.getName() + "("
				+ params
				+ ");\n";
		} else {
			ret+="\t\treturn(proxyedObject." + method.getName() + "("
				+ params
				+ "));\n";
		}

		ret+="\t}\n\n";

		return(ret);
	}

	private static void usage() {
		System.err.println("Usage:  ProxyInterfaceImplementor"
			+ " -interface className [-outputclass className]\n"
			+ "\t[-outputdir outputDir] [-superclass className]");
	}

	/** 
	 * Create a proxy class for the specified interface.
	 */
	public static void main(String args[]) throws Exception {
		String superclassName=null;
		String interfaceName=null;
		String outclass=null;
		String outdir=".";

		// Parse the arguments
		for(int i=0; i<args.length; i++) {
			if(args[i].equals("-superclass")) {
				superclassName=args[++i];
			} else if(args[i].equals("-interface")) {
				interfaceName=args[++i];
			} else if(args[i].equals("-outputclass")) {
				outclass=args[++i];
			} else if(args[i].equals("-outputdir")) {
				outdir=args[++i];
			} else {
				System.err.println("Unknown argument:  " + args[i]);
				usage();
				throw new Exception("Unknown argument:  " + args[i]);
			}
		}

		// make sure an interface name was given
		if(interfaceName == null) {
			System.err.println("No superinterface given.");
			usage();
			throw new Exception("No superinterface given.");
		}

		ProxyInterfaceImplementor i=
			new ProxyInterfaceImplementor(Class.forName(interfaceName));

		// Set the superclass
		if(superclassName!=null) {
			System.out.println("Loading super class:  " + superclassName);
			i.setSuperClass(Class.forName(superclassName));
		}

		// Figure out where to put the file
		if(outclass!=null) {
			// Set the output class name
			i.setOutputClass(outclass);
			i.writeSourceToFile(outdir);
		} else {
			System.out.print(i.makeSource());
		}
	}

}
