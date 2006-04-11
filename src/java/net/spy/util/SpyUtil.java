// Copyright (c) 1999  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 4303E5D9-1110-11D9-89B7-000A957659CC

package net.spy.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;

import net.spy.log.LoggerFactory;

/**
 * Miscellaneous utilities.
 */
public class SpyUtil {

	// Do not instantiate
	private SpyUtil() {
		super();
	}

	/**
	 * Shuffle (unsort) an array.
	 *
	 * @param in The array of objects to shuffle.
	 */
	public static Object[] shuffle(Object in[]) {
		Object tmp;
		Object ret[] = new Object[in.length];
		System.arraycopy(in, 0, ret, 0, in.length);
		SecureRandom r = new SecureRandom();
		int i;

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
		while(st.hasMoreTokens()) {
			ret[i++]=st.nextToken();
		}

		return(ret);
	}

	/** 
	 * Load the contents of the given reader as a String.
	 * 
	 * @param r a reader
	 * @return a String with the contents of the reader
	 */
	public static String getReaderAsString(Reader r) throws IOException {
		char c[]=new char[8192];
		StringBuilder rv=new StringBuilder(8192);

		int size;
		while((size=r.read(c)) >=0) {
			String tmp = new String(c);
			// Substring to get rid of all the damned nulls
			rv.append(tmp.substring(0, size));

		}

		return(rv.toString());
	}

	/**
	 * Return the contents of a file as a string.
	 *
	 * @param file File to read.
	 *
	 * @exception IOException Thrown if the file cannot be opened.
	 */
	public static String getFileData(File file) throws IOException {
		FileReader f = new FileReader(file);
		String rv=null;
		try {
			rv=getReaderAsString(f);
		} finally {
			CloseUtil.close(f);
		}
		return(rv);
	}

	/**
	 * Join an Enumeration of Strings on a join string.
	 * @param e The enumeration
	 * @param on the join string
	 * @return a new String with all of the elements joined
	 */
	@SuppressWarnings("unchecked")
	public static String join(Enumeration e, String on) {
		return(join(new Enumeriterator(e), on));
	}

	/** 
	 * Join an Iterator of Objects on a join string.  Each object's
	 * toString() method will be used for stringification.
	 * 
	 * @param i the iterator of Objects
	 * @param on the join string
	 * @return a new String with all of the elements joined
	 */
	public static String join(Iterator<Object> i, String on) {
		StringBuilder sb=new StringBuilder(256);
		while(i.hasNext()) {
			Object o=i.next();
			sb.append(o);
			if(i.hasNext()) {
				sb.append(on);
			}
		}
		return(sb.toString());
	}

	/** 
	 * Join a Collection of Objects on a join string.  Each object's
	 * toString() method will be used for stringification.
	 * 
	 * @param c the collection
	 * @param on the join string
	 * @return a String with all the elements joined
	 */
	@SuppressWarnings("unchecked")
	public static String join(Collection c, String on) {
		return(join(c.iterator(), on));
	}

	/**
	 * Get a string representing the hexidecimal value of the given byte
	 * array.
	 *
	 * @param me the byte array that needs hexified.
	 */
	public static String byteAToHexString(byte me[]) {
		StringBuilder sb=new StringBuilder(me.length*2);

		for(byte b : me) {
			int bai=b & 0xff;
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
		// If no classloader is given, use the classloader that loaded this
		// class.
		runClass(SpyUtil.class.getClassLoader(), classname, args);
	}

	/**
	 * Class invoker (runs main(String[]) from a String array.
	 */
	public static void runClass(ClassLoader cl, String classname, String args[])
		throws Exception {

		// Load the class.
		Class tclass=Class.forName(classname, true, cl);

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

	/** 
	 * Returns a Boolean with a value represented by the specified String.
	 * The Boolean returned represents the value true if the string
	 * argument is not null  and is equal, ignoring case, to the string
	 * "true".
	 * 
	 * @param s the string
	 * @return the Boolean instance
	 */
	public static Boolean getBoolean(String s) {
		Boolean rv=Boolean.FALSE;

		if(s != null && s.equalsIgnoreCase("true")) {
			rv=Boolean.TRUE;
		}

		return(rv);
	}

	/** 
	 * Get a boolean instance.
	 * 
	 * @param b the type of Boolean instance you want
	 * @return a shared instance of Boolean
	 */
	public static Boolean getBoolean(boolean b) {
		Boolean rv=null;
		if(b) {
			rv=Boolean.TRUE;
		} else {
			rv=Boolean.FALSE;
		}
		return(rv);
	}

	/** 
	 * Remove HTML tags from a string.
	 * 
	 * @param contents the string whose HTML tags need removed.
	 * @return the deHTMLed string
	 */
	public static String deHTML(String contents) {
		int inTag=0;
		StringBuilder sb=new StringBuilder(contents.length());

		char chars[]=contents.toCharArray();

		for(char c : chars) {
			if(c == '<') {
				inTag++;
			} else if( c == '>' && inTag>0) {
				if(inTag>=1) {
					inTag--;
				}
			} else {
				if(inTag==0) {
					sb.append(c);
				}
			}
		}
		return(sb.toString());
	}

	/** 
	 * Recursively remove a path.
	 */
	public static void rmDashR(File tmp) {
		File f[]=tmp.listFiles();
		if(f!=null) {
			for(int i=0; i<f.length; i++) {
				if(f[i].isDirectory()) {
					rmDashR(f[i]);
				} else {
					if(!f[i].delete()) {
						LoggerFactory.getLogger(SpyUtil.class).warn(
							"Couldn't delete " + f[i]);
					}
				}
			}
		}
		if(!tmp.delete()) {
			LoggerFactory.getLogger(SpyUtil.class).warn(
				"Couldn't delete " + tmp);
		}
	}
}
