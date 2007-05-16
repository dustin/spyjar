// Copyright (c) 1999 Dustin Sallings <dustin@spy.net>
// arch-tag: 608AC506-1110-11D9-A809-000A957659CC

package net.spy.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;
import net.spy.util.CloseUtil;
import net.spy.util.SpyUtil;

/**
 * Simple local disk caching.
 *
 * This is used for terribly simple caches with no expiration dates on
 * objects.  Things go in and they stay in.
 */
public class DiskCache extends AbstractMap<Serializable, Serializable> {

	// Base directory for hashing
	final String basedir;
	private final LRUCache<Serializable, SoftReference<Serializable>> lruCache;

	private static final int DEFAULT_LRU_CACHE_SIZE=100;

	private transient Logger logger = null;

	/**
	 * Get an DiskObject using the given directory.
	 */
	public DiskCache(String base) {
		this(base, DEFAULT_LRU_CACHE_SIZE);
	}

	/** 
	 * Get a DiskCache using the given directory with a backing LRU cache
	 * of the specified size.
	 * 
	 * @param base the base directory for the disk cache
	 * @param lruCacheSize the size of the LRU cache holding recently accessed
	 *		objects
	 */
	public DiskCache(String base, int lruCacheSize) {
		super();
		this.basedir=base;
		lruCache=new LRUCache<Serializable,
			SoftReference<Serializable>>(lruCacheSize);
	}

	/** 
	 * Get the base directory which this cache is watching.
	 */
	public String getBaseDir() {
		return(basedir);
	}

	// Get the path of an object for the given key
	private String getPath(Object key) {
		MessageDigest md=null;
		try {
			md=MessageDigest.getInstance("SHA");
		} catch(NoSuchAlgorithmException e) {
			throw new AssertionError("There's no SHA?");
		}
		if(key instanceof String) {
			String k=(String)key;
			md.update(k.getBytes());
		} else {
			String tmpkey=key.getClass().getName() + key.hashCode();
			md.update(tmpkey.getBytes());
		}

		String hashed=SpyUtil.byteAToHexString(md.digest());

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
    @Override
	public Serializable put(Serializable k, Serializable v) {
		Serializable rv=get(k);

		String pathto=getPath(k);

		FileOutputStream ostream=null;
		ObjectOutputStream p=null;
		try {
			ostream = new FileOutputStream(pathto);
			p = new ObjectOutputStream(ostream);
			p.writeObject(k);
			p.writeObject(v);
			p.flush();
		} catch(IOException e) {
			throw new RuntimeException("Error storing object", e);
		} finally {
			CloseUtil.close(p);
			CloseUtil.close(ostream);
		}

		return(rv);
	}

	/**
	 * Get an object from the cache.
	 *
	 * @return the object, or null if there's no such object
	 */
    @Override
	public Serializable get(Object key) {
		Serializable rv=null;

		if(key==null) {
			throw new NullPointerException("Name not provided");
		}

		rv=(Serializable) lruCache.get(key);
		if(rv==null) {
			rv=(Serializable) getFromDiskCache(key);
			lruCache.put((Serializable) key,
					new SoftReference<Serializable>(rv));
		}

		return(rv);
	}

	private Logger getLogger() {
		if(logger==null) {
			logger=LoggerFactory.getLogger(getClass());
		}
		return(logger);
	}

	private Object getFromDiskCache(Object key) {

		Object rv=null;

		FileInputStream istream=null;
		ObjectInputStream p=null;
		try {
			istream = new FileInputStream(getPath(key));
			p = new ObjectInputStream(istream);
			Object storedKey = p.readObject();
			Object o = p.readObject();

			if(!key.equals(storedKey)) {
				throw new Exception("Key value did not match ("
					+ storedKey + " != " + key + ")");
			}

			rv=o;
		} catch(FileNotFoundException e) {
			if(getLogger().isDebugEnabled()) {
				getLogger().debug("File not found loading disk cache", e);
			}
		} catch(Exception e) {
			getLogger().warn("Error getting ``%s'' from disk cache", key, e);
		} finally {
			CloseUtil.close(p);
			CloseUtil.close(istream);
		}

		return(rv);
	}

	@Override
	public Set<Map.Entry<Serializable, Serializable>> entrySet() {
		Set<Map.Entry<Serializable, Serializable>> rv=null;

		try {
			rv=new WalkerDiskCacheRanger();
		} catch(ClassNotFoundException e) {
			throw new RuntimeException("Error getting set", e);
		} catch(IOException e) {
			throw new RuntimeException("Error getting set", e);
		}

		return(rv);
	}

	private class WalkerDiskCacheRanger
		extends HashSet<Map.Entry<Serializable, Serializable>> {

		public WalkerDiskCacheRanger()
			throws IOException, ClassNotFoundException {
			super();

			init(new File(basedir));
		}

		private void init(File f) throws IOException, ClassNotFoundException {
			if(f.isDirectory()) {
				// If it's a directory, recurse
				File[] stuff=f.listFiles();
				for(int i=0; i<stuff.length; i++) {
					init(stuff[i]);
				}
			} else {
				// Regular file, open it and read it
				FileInputStream istream = null;
				ObjectInputStream p = null;
				Serializable key=null;
				try {
					istream = new FileInputStream(f);
					p = new ObjectInputStream(istream);
					key=(Serializable) p.readObject();
				} finally {
					CloseUtil.close(p);
					CloseUtil.close(istream);
				}

				// Add the new entry.
				add(new E(key, f));
			}
		}

		/** 
		 * Get an iterator.
		 */
		@Override
		public Iterator<Entry<Serializable, Serializable>> iterator() {
			return(new I(super.iterator()));
		}

	}

	// Iterator implementation
	private static class I extends
		Object implements Iterator<Entry<Serializable, Serializable>> {

		private final Iterator<Map.Entry<Serializable, Serializable>> i;
		private E current=null;
		private boolean begun=false;

		// Instatiate the iterator over the default iterator implementation
		public I(Iterator<Map.Entry<Serializable, Serializable>> it) {
			super();
			i=it;
		}

		/** 
		 * Get the next object.
		 */
		public E next() {
			begun=true;
			current=(E) i.next();
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

			File f=current.getPath();
			f.delete();
			i.remove();
		}

	}

	// Map entry implementation
	private static class E extends Object
		implements Map.Entry<Serializable, Serializable> {

		final File path;
		final Serializable k;

		public E(Serializable key, File p) {
			super();
			k=key;
			path=p;
		}

		/** 
		 * True if two objects are equal.
		 */
		@Override
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
		public Serializable getKey() {
			return(k);
		}

		/** 
		 * Get the hash code.
		 */
		@Override
		public int hashCode() {
			return(k.hashCode());
		}

		/** 
		 * Get the value.
		 */
		public Serializable getValue() {
			Serializable rv=null;

			FileInputStream istream=null;
			ObjectInputStream p=null;
			try {
				istream = new FileInputStream(path);
				p = new ObjectInputStream(istream);
				Object key=p.readObject();
				assert(key.equals(k));
				rv=(Serializable) p.readObject();
			} catch(IOException e) {
				throw new RuntimeException("Error getting object",e);
			} catch(ClassNotFoundException e) {
				throw new RuntimeException("Error getting object",e);
			} finally {
				CloseUtil.close(p);
				CloseUtil.close(istream);
			}

			return(rv);
		}

		/** 
		 * Not implemented.
		 */
		public Serializable setValue(Serializable o) {
			throw new UnsupportedOperationException("Can't set here.");
		}

	}

}
