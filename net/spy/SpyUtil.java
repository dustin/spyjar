// Copyright (c) 1999  Dustin Sallings <dustin@spy.net>
//
// $Id: SpyUtil.java,v 1.2 2002/08/28 03:52:06 dustin Exp $

package net.spy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.security.SecureRandom;

import java.util.Collection;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import net.spy.util.Enumeriterator;

/**
 * Miscellaneous utilities.
 */

public class SpyUtil {
	/**
	 * Shuffle (unsort) an array.
	 *
	 * @param in The array of objects to shuffle.
	 */
	public static Object[] shuffle(Object in[]) {
		Object tmp;
		Object ret[] = in;
		SecureRandom r = new SecureRandom();
		int size, i;

		for(i=0; i<ret.length; i++) {
			// Get a random number the size of the length
			int n = r.nextInt();
			if(n<0) {
				n=-n;
			}
			n=n%ret.length;
			tmp=ret[i];
			ret[i]=ret[n];
			ret[n]=tmp;
		}
		return(ret);
	}

	/**
	 * Split a string based on a tokenizer.
	 *
	 * @param on the string to split on (from StringTokenizer)
	 *
	 * @param input the string that needs to be split
	 *
	 * @see StringTokenizer
	 */
	public static String[] split(String on, String input) {
		StringTokenizer st = new StringTokenizer(input, on);

		String ret[]=new String[st.countTokens()];

		int i=0;
		while( st.hasMoreTokens() ) {
			ret[i++]=st.nextToken();
		}

		return(ret);
	}

	/**
	 * @deprecated use getFileData(File)
	 */
	public static String getFileData(String file) throws IOException {
		return(getFileData(new File(file)));
	}

	/**
	 * Return the contents of a file as a string.
	 *
	 * @param file File to read.
	 *
	 * @exception IOException Thrown if the file cannot be opened.
	 */
	public static String getFileData(File file) throws IOException {
	 	byte b[]=new byte[8192];
		FileInputStream f = new FileInputStream(file);
		StringBuffer rv=new StringBuffer((int)file.length());
		int size;

		while( (size=f.read(b)) >=0 ) {
			String tmp = new String(b);
			// Substring to get rid of all the damned nulls
			rv.append(tmp.substring(0, size));

		}

		f.close();
		return(rv.toString());
	}

	/**
	 * Join an Enumeration of Strings on a join string.
	 * @param e The enumeration
	 * @param on the join string
	 * @return a new String with all of the elements joined
	 */
	public static String join(Enumeration e, String on) {
		return(join(new Enumeriterator(e), on));
	}

	/** 
	 * Join an Iterator of Strings on a join string.
	 * 
	 * @param i the iterator of Strings
	 * @param on the join string
	 * @return a new String with all of the elements joined
	 */
	public static String join(Iterator i, String on) {
		StringBuffer sb=new StringBuffer(256);
		while(i.hasNext()) {
			String s=(String)i.next();
			sb.append(s);
			if(i.hasNext()) {
				sb.append(on);
			}
		}
		return(sb.toString());
	}

	/** 
	 * Join a Collection of Strings on a join string.
	 * 
	 * @param c the collection
	 * @param on the join string
	 * @return a String with all the elements joined
	 */
	public static String join(Collection c, String on) {
		return(join(c.iterator(), on));
	}

	/**
	 * Get a stack from an Exception.
	 *
	 * @param e Exception from which we'll be extracting the stack.
	 * @param skip Number of stack entries to skip (usually two or three)
	 */
	public static Enumeration getStackEnum(Exception e, int skip) {
		Vector v=new Vector();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(bytes, true);
		e.printStackTrace(writer);

		StringTokenizer t = new StringTokenizer(bytes.toString(), "\n");
		for(int i=0; i<skip; i++) {
			t.nextToken();
		}

		while(t.hasMoreTokens()) {
			v.addElement(t.nextToken().substring(4));
		}

		return(v.elements());
	}

	/**
	 * Get a stack from an exception.
	 *
	 * @param e Exception from which we'll be extracting the stack.
	 * @param skip Number of stack entries to skip (usually two or three)
	 */
	public static String getStack(Exception e, int skip) {
		return(join(getStackEnum(e, skip), ", "));
	}

	/**
	 * Dump the current list of threads to stderr.
	 */
	public static void dumpThreads() {
		// Find the system group.
		ThreadGroup start=null, last=null;
		for(start=Thread.currentThread().getThreadGroup(); start!=null;) {
			last=start;
			start=start.getParent();
		}
		// Dump the system group.
		last.list();
	}

	/**
	 * Get a string representing the hexidecimal value of the given byte
	 * array.
	 *
	 * @param me the byte array that needs hexified.
	 */
	public static String byteAToHexString(byte me[]) {
		StringBuffer sb=new StringBuffer(me.length*2);

		for(int i=0; i<me.length; i++) {
			int bai=(int)me[i] & 0xff;
			if(bai<0x10) {
				sb.append('0');
			}
			sb.append(Integer.toHexString(bai));
		}

		return(sb.toString());
	}

	/**
	 * Class invoker (runs main(String[]) from a String array.
	 */
	public static void runClass(String classname, String args[])
		throws Exception {

		// Load the class.
		Class tclass=Class.forName(classname);

		// Find the method
		Class paramtypes[] = new Class[1];
		String tmp[]=new String[0];
		paramtypes[0]=tmp.getClass();
		Method m = tclass.getMethod("main", paramtypes);

		// Set the arguments.
		Object params[]=new Object[1];
		params[0]=args;

		// Run it!
		try {
			m.invoke(tclass, params);
		} catch(InvocationTargetException ite) {
			ite.printStackTrace();
			Throwable t=ite.getTargetException();
			if(t instanceof Exception) {
				throw (Exception)t;
			} else {
				t.printStackTrace();
			}
		}

	}
}
