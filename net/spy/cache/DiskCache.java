// Copyright (c) 1999 Dustin Sallings <dustin@spy.net>
// $Id: DiskCache.java,v 1.4 2002/09/13 17:27:49 dustin Exp $

package net.spy.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.AbstractMap;

import net.spy.util.NestedRuntimeException;

/**
 * Simple local disk caching.
 *
 * This is used for terribly simple caches with no expiration dates on
 * objects.  Things go in and they stay in.
 */
public class DiskCache extends AbstractMap {

	// Base directory for hashing
	private String basedir = null;

	/**
	 * Get an DiskObject using the given directory.
	 */
	public DiskCache(String basedir) {
		super();
		this.basedir=basedir;
	}

	// Get the path of an object for the given key
	private String getPath(Object key) {
		MessageDigest md=null;
		try {
			md=MessageDigest.getInstance("SHA");
		} catch(NoSuchAlgorithmException e) {
			throw new Error("There's no SHA?");
		}
		if(key instanceof String) {
			String k=(String)key;
			md.update(k.getBytes());
		} else {
			String tmpkey=key.getClass().getName() + key.hashCode();
			md.update(tmpkey.getBytes());
		}

		String hashed=net.spy.SpyUtil.byteAToHexString(md.digest());

		String base=basedir+"/"+hashed.substring(0,2);
		String path=base+"/"+hashed;

		File f=new File(base);
		if(!f.isDirectory()) {
			f.mkdirs();
		}

		return(path);
	}

	/**
	 * Store an object in the cache.
	 *
     * @param k object key
     * @param v value
     * @return the old object stored in that location (if any)
	 */
    public Object put(Object k, Object v) {
		Object rv=get(k);

		String pathto=getPath(k);

		try {
			FileOutputStream ostream = new FileOutputStream(pathto);
			ObjectOutputStream p = new ObjectOutputStream(ostream);
			p.writeObject(k);
			p.writeObject(v);
			p.flush();
			p.close();
			ostream.close();
		} catch(IOException e) {
			throw new NestedRuntimeException("Error storing object", e);
		}

		return(rv);
	}

    /** 
     * Old API for storing.
	 *
	 * @deprecated use put(Object,Object) instead
     */
    public void storeObject(String key, Object o) throws IOException {
		put(key, o);
	}

	/**
	 * Get an object from the cache.
	 *
	 * @return the object, or null if there's no such object
	 */
    public Object get(Object key) {
		Object rv=null;

		if(key==null) {
			throw new NullPointerException("Name not provided");
		}

		try {
			FileInputStream istream = new FileInputStream(getPath(key));
			ObjectInputStream p = new ObjectInputStream(istream);
			Object storedKey = p.readObject();
			Object o = p.readObject();

			if(!key.equals(storedKey)) {
				throw new Exception("Key value did not match ("
					+ storedKey + " != " + key + ")");
			}

			rv=o;

			p.close();
			istream.close();
		} catch(FileNotFoundException e) {
            // System.err.println(e.toString());
		} catch(Exception e) {
			e.printStackTrace();
		}
		return(rv);
	}

	public Set entrySet() {
		Set rv=null;

		try {
			rv=new WalkerDiskCacheRanger();
		} catch(ClassNotFoundException e) {
			throw new NestedRuntimeException("Error getting set", e);
		} catch(IOException e) {
			throw new NestedRuntimeException("Error getting set", e);
		}

		return(rv);
	}

    /** 
     * Old API for getting objects.
	 *
	 * @deprecated use get(Object) instead
     */
    public Object getObject(String key) {
		return(get(key));
	}

	private class WalkerDiskCacheRanger extends HashSet {

		public WalkerDiskCacheRanger()
			throws IOException, ClassNotFoundException {
			super();

			init(new File(basedir));
		}

		private void init(File f) throws IOException, ClassNotFoundException {
			if(f.isDirectory()) {
				// If it's a directory, recurse
				File stuff[]=f.listFiles();
				if(stuff!=null) {
					for(int i=0; i<stuff.length; i++) {
						init(stuff[i]);
					}
				}
			} else {
				// Regular file, open it and read it
				FileInputStream istream = new FileInputStream(f);
				ObjectInputStream p = new ObjectInputStream(istream);
				Object key=p.readObject();
				p.close();
				istream.close();

				// Add the new entry.
				add(new E(key, f));
			}
		}

		/** 
		 * Get an iterator.
		 */
		public Iterator iterator() {
			return(new I(this, super.iterator()));
		}

	}

	// Iterator implementation
	private class I extends Object implements Iterator {

		private Set s=null;
		private Iterator i=null;
		private E current=null;
		private boolean begun=false;

		// Instatiate the iterator over the default iterator implementation
		public I(Set s, Iterator i) {
			super();
			this.i=i;
			this.s=s;
		}

		/** 
		 * Get the next object.
		 */
		public Object next() {
			begun=true;
			current=(E)i.next();
			return(current);
		}

		/** 
		 * True if there's another entry.
		 */
		public boolean hasNext() {
			return(i.hasNext());
		}

		/** 
		 * Remove the given object.
		 */
		public void remove() {
			if(begun==false) {
				throw new IllegalStateException("Have not yet begun walking.");
			}

			if(current!=null) {
				File f=current.getPath();
				f.delete();
			}
			i.remove();
		}

	}

	// Map entry implementation
	private class E extends Object implements Map.Entry {

		File path=null;
		Object k=null;

		public E(Object key, File path) {
			super();
			this.k=key;
			this.path=path;
		}

		/** 
		 * True if two objects are equal.
		 */
		public boolean equals(Object o) {
			boolean rv=false;

			if(o instanceof E) {
				E e=(E)o;
				rv=e.k.equals(k);
			}

			return(rv);
		}

		/** 
		 * Get the path to the file.
		 */
		public File getPath() {
			return(path);
		}

		/** 
		 * Get the key.
		 */
		public Object getKey() {
			return(k);
		}

		/** 
		 * Get the hash code.
		 */
		public int hashCode() {
			return(k.hashCode());
		}

		/** 
		 * Get the value.
		 */
		public Object getValue() {
			Object rv=null;

			try {
				FileInputStream istream = new FileInputStream(path);
				ObjectInputStream p = new ObjectInputStream(istream);
				Object key=p.readObject();
				Object val=p.readObject();
				p.close();
				istream.close();

				rv=val;
			} catch(IOException e) {
				throw new NestedRuntimeException("Error getting object",e);
			} catch(ClassNotFoundException e) {
				throw new NestedRuntimeException("Error getting object",e);
			}

			return(rv);
		}

		/** 
		 * Not implemented.
		 */
		public Object setValue(Object o) {
			throw new UnsupportedOperationException("Can't set here.");
		}

	}

}
