// Copyright (c) 1999 Dustin Sallings <dustin@spy.net>
// $Id: DiskCache.java,v 1.3 2002/09/13 06:52:23 dustin Exp $

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
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.AbstractMap;
import java.util.AbstractSet;

import net.spy.util.NestedRuntimeException;

/**
 * Simple local disk caching.
 *
 * This is used for terribly simple caches with no expiration dates on
 * objects.  Things go in and they stay in.
 */
public class DiskCache extends Object {

	private static final String DEFAULT_DIR="/tmp/diskcache";

	// Base directory for hashing
	private String basedir = null;

	/**
	 * Get a DiskCache object using the default directory.
	 */
	public DiskCache() {
		this(DEFAULT_DIR);
	}

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
			String storedName = (String)p.readObject();
			Object o = p.readObject();

			if(!key.equals(storedName)) {
				throw new Exception("Key value did not match ("
					+ storedName + " != " + key + ")");
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

    /** 
     * Old API for getting objects.
	 *
	 * @deprecated use get(Object) instead
     */
    public Object getObject(String key) {
		return(get(key));
	}

}
