/*
 * Java Interface Implementor.  :)
 *
 * Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
 *
 * arch-tag: D9D3F080-1110-11D9-8A4C-000A957659CC
 */

package net.spy.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;

import net.spy.SpyObject;

/**
 * Extend existing classes with missing methods required to implement a
 * specified interface.
 *
 * Think Java Interfaces are a poor replacement for the lack of multiple
 * inheritence?  SO DO I!  That's why this class exists.
 * <p>
 * Using InterfaceImplementor, you can write code that's portable among
 * different API specs.  You simply implement what you need out of an
 * interface, and InterfaceImplementor writes a class that extends from the
 * class you wrote, and fills in all the blanks for you.
 * <p>
 * Here's the way you use it:
 * <p>
 * java net.spy.util.InterfaceImplementor -interface java.sql.ResultSet
 * -superclass test.TestSet -outputclass test.TestSetImpl
 */
public class InterfaceImplementor extends SpyObject {

	// Functions that are already defined.
	private HashSet<String> definedFunctions=null;
	private Class interfaceClass=null;
	private Class superClass=null;
	private String outpackage=null;
	private String outclass="BLAH";

	/**
	 * Get a new InterfaceImplementor to implement the passed in interface.
	 *
	 * @param c The interface to implement.
	 *
	 * @exception NullPointerException if the passed in class is null
	 * @exception IllegalArgumentException if the passed in class is not
	 * 			an interface
	 */
	public InterfaceImplementor(Class c) {
		super();

		// Verify the interface isn't null
		if(c==null) {
			throw new NullPointerException("Null interface is invalid.");
		}

		// Verify that it's an interface
		if(!c.isInterface()) {
			throw new IllegalArgumentException(
				"Passed in class is not an interface.");
		}

		// Go ahead and initialize this here.  That way we don't have to
		// worry about it later.
		definedFunctions=new HashSet<String>();
		this.interfaceClass=c;
	}

	/** 
	 * Get the interface we're implementing.
	 */
	protected Class getInterface() {
		return(interfaceClass);
	}

	/** 
	 * Get the parent class of the generated class.
	 */
	protected Class getSuperclass() {
		return(superClass);
	}

	/**
	 * Get the name of the class we'll be generating
	 */
	public String getOutClassName() {
		return(outclass);
	}

	/**
	 * Get the name of the package containing class we'll be generating
	 */
	public String getOutPackageName() {
		return(outpackage);
	}

	/**
	 * Set the name of the output class.
	 */
	public void setOutputClass(String to) {
		int lastdot=to.lastIndexOf(".");
		if(lastdot==-1) {
			this.outclass=to;
		} else {
			outpackage=to.substring(0, lastdot);
			this.outclass=to.substring(lastdot+1);
		}
	}

	/**
	 * Set an optional superclass that defines some of the methods for the
	 * implementation.
	 *
	 * @param c Superclass
	 *
	 * @exception NullPointerException if the passed in class is null
	 * @exception IllegalArgumentException if the passed in class isn't
	 * 		valid for this operation.
	 */
	public void setSuperClass(Class c) {
		if(c==null) {
			throw new NullPointerException("Null class is invalid.");
		}

		int modifiers=c.getModifiers();

		if(Modifier.isFinal(modifiers)) {
			throw new IllegalArgumentException(
				"You can't extend from final classes.");
		}
		if(Modifier.isInterface(modifiers)) {
			throw new IllegalArgumentException(
				"Interfaces aren't valid here.");
		}

		superClass=c;
		// Extract the methods
		getMethods(c);
	}

	// Extract the methods from the above.
	private void getMethods(Class c) {
		// First, get the declared ones
		Method methods[]=c.getDeclaredMethods();
		for(int i=0; i<methods.length; i++) {
			int modifiers=methods[i].getModifiers();
			// Ignore abstract methods
			if(!Modifier.isAbstract(modifiers)) {
				definedFunctions.add(getSignature(methods[i]));
			}
		}
		// Then, get the rest (which includes some declared ones)
		methods=c.getMethods();
		for(int i=0; i<methods.length; i++) {
			int modifiers=methods[i].getModifiers();
			// Ignore abstract methods
			if(!Modifier.isAbstract(modifiers)) {
				definedFunctions.add(getSignature(methods[i]));
			}
		}
	}

	private String decodeType(Class type) {
		String rv=null;
		if(type.isArray()) {
			rv=decodeType(type.getComponentType()) + "[]";
		} else {
			rv=type.getName();
		}
		return(rv);
	}

	/** 
	 * Get the method signature.
	 * 
	 * @param method method needing the signature
	 * @return the method signature, as a String
	 */
	protected String getSignature(Method method) {
		return(getSignature(method, true));
	}

	/** 
	 * Get a String representing this method signature.
	 * 
	 * @param method the name of the method
	 * @param needExceptions true if exceptions are needed as part of the
	 * 		signature string
	 */
	protected String getSignature(Method method, boolean needExceptions) {

		String ret="";

		// Get the modifiers
		int modifiers=method.getModifiers();
		// Clear the abstract flag
		modifiers&=~(Modifier.ABSTRACT);
		// Add the modifiers to our string
		ret=Modifier.toString(modifiers);

		// Get the return type
		Class rt=method.getReturnType();
		ret+=" " + decodeType(rt) + " ";

		// Add the method name
		String name=method.getName();

		// OK, now deal with parameters
		Class types[]=method.getParameterTypes();

		ret+=name + "(";
		if(types.length > 0) {
			for(int i=0; i<types.length; i++) {
				ret+=decodeType(types[i]) + " a" + i + ", ";
			}
			// Strip off the last comma
			ret=ret.substring(0, ret.length()-2);
		}
		// Get rid of the last comma and add a paren
		ret+=") ";
		if(needExceptions) {
			ret+=getExSignature(method);
		}
		return(ret.trim());
	}

	/** 
	 * Get the relative javadoc signature for this method.
	 */
	protected String getDocLink(Method method) {
		String ret=method.getName();
		Class types[]=method.getParameterTypes();
		ret+="(";
		if(types.length > 0) {
			for(int i=0; i<types.length; i++) {
				ret+=decodeType(types[i]);
				ret+=",";
			}
			// Strip off the last ,
			ret=ret.substring(0, ret.length()-1);
		}
		ret+=")";
		return(ret);
	}

	private String getExSignature(Method method) {
		String ret="";
		// Now flip through the exceptions
		Class e[]=method.getExceptionTypes();
		if(e.length>0) {
			ret+="\n\t\tthrows ";
			for(int i=0; i<e.length; i++) {
				ret+=e[i].getName() + ",";
			}
			// Strip off the last comma
			ret=ret.substring(0, ret.length()-1);
		}

		return(ret);
	}

	// Get the constructor signature
	private String getSignature(Constructor con) {
		String ret=null;

		// Get the modifiers
		int modifiers=con.getModifiers();
		// Add the modifiers to our string
		ret=Modifier.toString(modifiers) + " " + outclass;

		Class types[]=con.getParameterTypes();
		ret+="(";
		if(types.length > 0) {
			for(int i=0; i<types.length; i++) {
				ret+=decodeType(types[i]) + " a" + i + ", ";
			}
			// Strip off the last ,space
			ret=ret.substring(0, ret.length()-2);
		}
		ret+=") ";

		// Now flip through the exceptions
		Class e[]=con.getExceptionTypes();
		if(e.length>0) {
			ret+="\n\t\tthrows ";
			for(int i=0; i<e.length; i++) {
				ret+=e[i].getName() + ", ";
			}
			// Strip off the last ,space
			ret=ret.substring(0, ret.length()-2);
		}

		return(ret);
	}

	/** 
	 * Get the relative javadoc signature for this Constructor.
	 */
	protected String getDocLink(Constructor con) {
		String ret=con.getName();
		Class types[]=con.getParameterTypes();
		ret+="(";
		if(types.length > 0) {
			for(int i=0; i<types.length; i++) {
				ret+=decodeType(types[i]);
				ret+=",";
			}
			// Strip off the last ,
			ret=ret.substring(0, ret.length()-1);
		}
		ret+=")";
		return(ret);
	}

	/** 
	 * Implement the given method.
	 *
	 * Subclasses may override this to provide a different method
	 * implementation
	 * 
	 * @param method the method to be implemented.
	 * @return the text required to implement this method
	 */
	protected String implement(Method method) {
		// Start
		String ret="\t/**\n"
			+ "\t * InterfaceImplementor implementation of "
				+ method.getName() + ".\n"
			+ "\t * @see " + interfaceClass.getName() + "#"
				+ getDocLink(method) + "\n"
			+ "\t */\n";
		ret+="\t" + getSignature(method) + " {\n";

		Class e[]=method.getExceptionTypes();
		// If we can throw an exception, do so.
		if(e.length>0) {
			ret+="\t\tthrow new "
				+ e[0].getName() + "(\""
				+ getSignature(method, false)
				+ " not implemented yet.\");\n";
		} else {
			// OK, let's check the return value...
			Class rt=method.getReturnType();
			if(rt.isPrimitive()) {
				if(rt == Boolean.TYPE) {
					ret+="\t\treturn false;\n";
				} else if(rt == Void.TYPE) {
					// Do nothing (nothing to do!)
				} else {
					ret+="\t\treturn 0;\n";
				}
			} else {
				ret+="\t\treturn null;\n";
			}
		}

		ret+="\t}\n\n";
		return(ret);
	}

	// Implement a constructor that calls the super constructor
	private String implementConstructor(Constructor con) {
		String ret="\t/**\n"
			+ "\t * Constructor provided by InterfaceImplementor.\n";
		if(superClass != null) {
			ret+="\t * @see " + superClass.getName() + "#"
				+ getDocLink(con) + "\n";
		}
		ret+="\t */\n";
		ret+="\t" + getSignature(con) + " {\n";

		ret+="\t\tsuper(";
		Class params[]=con.getParameterTypes();
		if(params.length>0) {
			for(int i=0; i<params.length; i++) {
				ret+="a" + i + ", ";
			}
			// Get rid of the trailing ,space
			ret=ret.substring(0, ret.length()-2);
		}
		ret+=");\n\t}\n\n";
		return(ret);
	}

	/** 
	 * Anything that should appear before the automatically generated
	 * constructors.
	 */
	protected String preConstructors() {
		return(null);
	}

	/** 
	 * Anything that should appear before the automatically generated
	 * methods.
	 */
	protected String preMethods() {
		return(null);
	}

	/**
	 * Generate the source code for the class this object represents.
	 */
	public String makeSource() {
		String ret="";

		// If there's a package, declare it
		if(outpackage!=null) {
			ret+="package " + outpackage + ";\n\n";
		}

		ret+="/**\n"
			+ " * InterfaceImplementor implementation of "
				+ interfaceClass.getName() + ".\n"
			+ " */\n";

		ret+="public class " + outclass + " ";

		// If there's a superclass, extend it
		if(superClass!=null) {
			ret+="extends " + superClass.getName() + " ";
		}
		
		ret+="implements " + interfaceClass.getName() + " {\n\n";

		// Do any pre-constructor stuff
		String pc=preConstructors();
		if(pc!=null) {
			ret+=pc;
		}

		// If there's a superclass, grab all of the constructors from that
		// superclass and make sure they all get called.
		if(superClass!=null && buildConstructors()) {
			Constructor constructors[]=superClass.getConstructors();
			for(int i=0; i<constructors.length; i++) {
				ret+=implementConstructor(constructors[i]);
			}
		}

		// Stuff that needs to be added after all methods.
		String pm=preMethods();
		if(pm!=null) {
			ret+=pm;
		}

		// Now, implement the methods of the interface
		Method methods[]=interfaceClass.getMethods();
		for(int i=0; i<methods.length; i++) {
			String sig=getSignature(methods[i]);
			if(!definedFunctions.contains(sig)) {
				ret+=implement(methods[i]);
			}
		}

		ret+=("}\n");
		return(ret);
	}

	/** 
	 * If true, build the default constructors.
	 */
	protected boolean buildConstructors() {
		return(true);
	}

	/** 
	 * Write this implementation out to a given file.
	 * 
	 * @param outdir the base directory to write the file
	 * @throws IOException if there's a problem writing the file
	 */
	public void writeSourceToFile(String outdir) throws IOException {
		String fn=outdir + File.separatorChar;
		String op=getOutPackageName();
		String oc=getOutClassName();
		// Figure out if there's a package name, if so, make sure the
		// dirs exist and all that.
		if(op!=null) {
			File packagepath= new File(
				fn + op.replace('.', File.separatorChar));
			packagepath.mkdirs();
			fn=packagepath.toString() + File.separatorChar;
		}
		// Stick the classname.java to the end
		fn+=oc + ".java";

		// Write it out...
		System.out.println("Writing output to " + fn);
		FileWriter fw=new FileWriter(fn);
		try {
			fw.write(makeSource());
		} finally {
			CloseUtil.close(fw);
		}
	}

	// A method com

	private static void usage() {
		System.err.println("Usage:  InterfaceImplementor"
			+ " -interface className [-superclass className]\n"
			+ "\t[-outputdir outputDir] [-outputclass className]");
	}

	public static void main(String args[]) throws Exception {

		String superclassName=null;
		String interfaceName=null;
		String outclass=null;
		String outdir=".";

		// parse the arguments
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

		// Make sure an interface was given.
		if(interfaceName == null) {
			System.err.println("No superinterface given.");
			usage();
			throw new Exception("No superinterface given.");
		}

		// Get an interface implementor
		InterfaceImplementor i=
			new InterfaceImplementor(Class.forName(interfaceName));

		// Set the superclass
		if(superclassName!=null) {
			System.out.println("Loading super class:  " + superclassName);
			i.setSuperClass(Class.forName(superclassName));
		}

		// If the user specified an output class, create the .java file to
		// make it, else send it to stdout with the class name BLAH
		if(outclass!=null) {
			// Set the output class name
			i.setOutputClass(outclass);

			i.writeSourceToFile(outdir);
		} else {
			System.out.print(i.makeSource());
		}
	}
}
